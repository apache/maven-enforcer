/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.enforcer.rules.modules;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.dependency.ResolverUtil;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.graph.DependencyNode;

/**
 * Bans <em>split packages</em>: a package contained in more than one Java module. The boot layer
 * rejects two modules on the module path that contain the same package
 * ({@code java.lang.module.ResolutionException}), and if the offending module happens not to be
 * {@code requires}d, the build stays green while a type or endpoint is silently absent at runtime.
 * Split packages are typically introduced by accident — a code generator, a shaded/unpacked
 * dependency, or a copied resource placing classes of a foreign package into the project's output.
 *
 * <p>The rule intersects the package sets of the project's compiled output(s) (classic layout and
 * the Maven&nbsp;4 module source hierarchy) and of every declared dependency — deliberately
 * <em>not</em> only the {@code requires}d modules, which is exactly what catches the silent
 * variant. Overlaps between two modules that would both end up on the module path are errors;
 * overlaps involving a plain classpath artifact are legal today and reported at
 * {@code <classpathSeverity>} (default {@code warn}) as a modularization hazard.</p>
 */
@Named("banSplitPackages")
public final class BanSplitPackages extends AbstractModuleInfoRule {

    /** Severity for overlaps involving an artifact that is not on the module path. */
    private static final String SEVERITY_WARN = "warn";

    private static final String SEVERITY_ERROR = "error";
    private static final String SEVERITY_IGNORE = "ignore";

    /**
     * Severity for overlaps involving a non-modular (classpath) artifact:
     * {@code warn} (default), {@code error} or {@code ignore}.
     */
    private String classpathSeverity = SEVERITY_WARN;

    /** Packages permitted to overlap. */
    private List<String> allowedSplitPackages = new ArrayList<>();

    /** Module names whose artifacts are excluded from the check. */
    private List<String> ignoredModules = new ArrayList<>();

    /** Dependencies excluded from the check, as {@code groupId:artifactId}. */
    private List<String> ignoredArtifacts = new ArrayList<>();

    private final ResolverUtil resolverUtil;

    @Inject
    public BanSplitPackages(MavenProject project, ResolverUtil resolverUtil) {
        super(project);
        this.resolverUtil = resolverUtil;
    }

    public void setClasspathSeverity(String classpathSeverity) {
        this.classpathSeverity = classpathSeverity;
    }

    public void setAllowedSplitPackages(List<String> allowedSplitPackages) {
        this.allowedSplitPackages = allowedSplitPackages;
    }

    public void setIgnoredModules(List<String> ignoredModules) {
        this.ignoredModules = ignoredModules;
    }

    public void setIgnoredArtifacts(List<String> ignoredArtifacts) {
        this.ignoredArtifacts = ignoredArtifacts;
    }

    /** One holder of a package set: a project module output or a dependency artifact. */
    private static final class PackageOwner {
        private final String display;
        private final JavaModuleInfo module;
        private final boolean automatic;
        private final Set<String> packages;

        private PackageOwner(String display, JavaModuleInfo module, boolean automatic, Set<String> packages) {
            this.display = display;
            this.module = module;
            this.automatic = automatic;
            this.packages = packages;
        }
    }

    /** One detected overlap. */
    private static final class Overlap {
        private final String packageName;
        private final PackageOwner first;
        private final PackageOwner second;
        private final boolean modulePath;

        private Overlap(String packageName, PackageOwner first, PackageOwner second, boolean modulePath) {
            this.packageName = packageName;
            this.first = first;
            this.second = second;
            this.modulePath = modulePath;
        }

        private String describe() {
            return "package " + packageName + " is contained in " + first.display + " and " + second.display;
        }
    }

    @Override
    public void execute() throws EnforcerRuleException {
        if (!Arrays.asList(SEVERITY_WARN, SEVERITY_ERROR, SEVERITY_IGNORE).contains(classpathSeverity)) {
            throw new EnforcerRuleException(
                    "Invalid <classpathSeverity> '" + classpathSeverity + "': use warn, error or ignore");
        }

        List<PackageOwner> owners = new ArrayList<>();
        Set<String> projectRequires = new HashSet<>();
        collectProjectOutputs(owners, projectRequires);
        collectDependencies(owners);

        List<Overlap> overlaps = findOverlaps(owners, projectRequires);
        report(overlaps);
    }

    /** One package owner per compiled project output; also accumulates the project's {@code requires}. */
    private void collectProjectOutputs(List<PackageOwner> owners, Set<String> projectRequires)
            throws EnforcerRuleException {
        for (ModuleOutput output : moduleOutputs()) {
            Set<String> packages = new TreeSet<>();
            collectOutputPackages(output.root(), "", packages);
            JavaModuleInfo module = output.moduleInfo();
            if (module != null) {
                projectRequires.addAll(module.requires());
            }
            if (module == null && packages.isEmpty()) {
                continue; // e.g. an empty output directory
            }
            String display = module != null
                    ? "module " + module.name() + " (project output "
                            + output.root().getName() + ")"
                    : "the non-modular project output " + output.root();
            if (module != null && ignoredModules.contains(module.name())) {
                continue;
            }
            // The compiled module-info's package list is only finalized at packaging time, and
            // offending classes are frequently added after compile (generation, copy) — so the
            // walked directory content is authoritative, not the descriptor.
            owners.add(new PackageOwner(display, module, false, packages));
        }
    }

    /** Every package below {@code directory} that holds at least one class file. */
    private void collectOutputPackages(File directory, String packageName, Set<String> packages) {
        File[] entries = directory.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                String childPackage = packageName.isEmpty() ? entry.getName() : packageName + "." + entry.getName();
                collectOutputPackages(entry, childPackage, packages);
            } else if (!packageName.isEmpty() && entry.getName().endsWith(".class")) {
                packages.add(packageName);
            }
        }
    }

    /** One package owner per resolved declared dependency (test scope excluded). */
    private void collectDependencies(List<PackageOwner> owners) throws EnforcerRuleException {
        DependencyNode root =
                resolverUtil.resolveTransitiveDependencies(false, true, false, Arrays.asList(Artifact.SCOPE_TEST));
        Map<File, String> artifactFiles = new LinkedHashMap<>();
        collectArtifactFiles(root, true, artifactFiles, new HashSet<>());
        for (Map.Entry<File, String> entry : artifactFiles.entrySet()) {
            File file = entry.getKey();
            String id = entry.getValue();
            if (!file.isDirectory() && !file.getName().endsWith(".jar")) {
                continue;
            }
            ArtifactModuleScanner.ScannedArtifact scanned;
            try {
                scanned = ArtifactModuleScanner.scan(file);
            } catch (IOException e) {
                throw new EnforcerRuleException("Failed to scan dependency " + id + ": " + e.getMessage(), e);
            }
            JavaModuleInfo module = scanned.moduleInfo();
            if (module != null && ignoredModules.contains(module.name())) {
                continue;
            }
            String display = module != null
                    ? "module " + module.name() + " (" + id + (scanned.isAutomatic() ? ", automatic)" : ")")
                    : "the non-modular dependency " + id;
            owners.add(new PackageOwner(display, module, scanned.isAutomatic(), scanned.packages()));
        }
    }

    private void collectArtifactFiles(
            DependencyNode node, boolean isRoot, Map<File, String> collected, Set<DependencyNode> visited) {
        if (!visited.add(node)) {
            return;
        }
        if (!isRoot && node.getArtifact() != null) {
            org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
            if (artifact.getFile() != null
                    && !ignoredArtifacts.contains(artifact.getGroupId() + ":" + artifact.getArtifactId())) {
                collected.putIfAbsent(artifact.getFile(), artifact.toString());
            }
        }
        for (DependencyNode child : node.getChildren()) {
            collectArtifactFiles(child, false, collected, visited);
        }
    }

    private List<Overlap> findOverlaps(List<PackageOwner> owners, Set<String> projectRequires) {
        List<Overlap> overlaps = new ArrayList<>();
        for (int i = 0; i < owners.size(); i++) {
            for (int j = i + 1; j < owners.size(); j++) {
                PackageOwner first = owners.get(i);
                PackageOwner second = owners.get(j);
                Set<String> shared = new TreeSet<>(first.packages);
                shared.retainAll(second.packages);
                shared.removeAll(allowedSplitPackages);
                if (shared.isEmpty()) {
                    continue;
                }
                boolean modulePath = isOnModulePath(first, projectRequires) && isOnModulePath(second, projectRequires);
                for (String packageName : shared) {
                    overlaps.add(new Overlap(packageName, first, second, modulePath));
                }
            }
        }
        return overlaps;
    }

    /**
     * Whether the owner ends up on the module path: an explicit module always does (for the silent
     * variant it is exactly the not-{@code requires}d explicit module that breaks late); an
     * automatic module only when the project actually {@code requires} its derived name; a
     * non-modular artifact never does.
     */
    private boolean isOnModulePath(PackageOwner owner, Set<String> projectRequires) {
        if (owner.module == null) {
            return false;
        }
        return !owner.automatic || projectRequires.contains(owner.module.name());
    }

    private void report(List<Overlap> overlaps) throws EnforcerRuleException {
        Set<String> failures = new LinkedHashSet<>();
        for (Overlap overlap : overlaps) {
            if (overlap.modulePath) {
                failures.add(overlap.describe());
            } else if (SEVERITY_ERROR.equals(classpathSeverity)) {
                failures.add(overlap.describe() + " (classpath overlap, <classpathSeverity> is 'error')");
            } else if (SEVERITY_WARN.equals(classpathSeverity)) {
                getLog().warn(ruleName() + ": " + overlap.describe()
                        + " — legal on the classpath, but will break a move to the module path");
            }
        }
        if (!failures.isEmpty()) {
            String message = getMessage();
            StringBuilder buf = new StringBuilder(
                    message != null
                            ? message
                            : "Split package(s) found — a package must belong to exactly one module:");
            for (String failure : failures) {
                buf.append(System.lineSeparator()).append("  ").append(failure);
            }
            if (message == null) {
                buf.append(System.lineSeparator())
                        .append("Move the offending classes into the module that owns the package, or list the ")
                        .append("package in <allowedSplitPackages> if the overlap is intentional.");
            }
            throw new EnforcerRuleException(buf.toString());
        }
    }

    @Override
    public String toString() {
        return String.format(
                "BanSplitPackages[classpathSeverity=%s, allowedSplitPackages=%s, ignoredModules=%s, "
                        + "ignoredArtifacts=%s, message=%s]",
                classpathSeverity, allowedSplitPackages, ignoredModules, ignoredArtifacts, getMessage());
    }
}
