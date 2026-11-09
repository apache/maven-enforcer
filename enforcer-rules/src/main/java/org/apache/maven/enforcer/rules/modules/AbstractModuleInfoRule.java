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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;

/**
 * Base class for rules that inspect the {@code module-info.class} of the project's main output.
 * Two output layouts are supported:
 *
 * <ul>
 *   <li><b>Classic</b>: the descriptor sits directly in {@code ${project.build.outputDirectory}}
 *       (one Maven project = one Java module);</li>
 *   <li><b>Module source hierarchy</b> (Maven 4, POM model 4.1.0): one Maven project compiles
 *       several modules, each to its own subdirectory
 *       {@code ${project.build.outputDirectory}/<module-name>/module-info.class}.</li>
 * </ul>
 *
 * Subclasses call {@link #moduleOutputs()} and enforce a specific constraint on each returned
 * {@link ModuleOutput}.
 */
public abstract class AbstractModuleInfoRule extends AbstractStandardEnforcerRule {

    /** Base URL of the published rule documentation, for pointing users at the phase binding advice. */
    private static final String DOCUMENTATION_BASE_URL = "https://maven.apache.org/enforcer/enforcer-rules/";

    /** Descriptor of a multi-release JAR, as {@code META-INF/versions/<n>/module-info.class}. */
    private static final Pattern VERSIONED_MODULE_INFO =
            Pattern.compile("META-INF/versions/(\\d+)/module-info\\.class");

    /**
     * Plugins known to inject a module descriptor into the packaged artifact after compilation, by
     * {@code groupId:artifactId}. Used only to word a warning more precisely -- never to decide whether
     * a rule passes -- so an injector missing from this list costs a less specific hint, nothing more.
     */
    private static final List<String> DESCRIPTOR_INJECTING_PLUGINS =
            Collections.singletonList("org.moditect:moditect-maven-plugin");

    /** Goal of a descriptor-injecting plugin, matched alongside the coordinates so a fork is still recognised. */
    private static final String DESCRIPTOR_INJECTING_GOAL = "add-module-info";

    protected final MavenProject project;

    protected AbstractModuleInfoRule(MavenProject project) {
        this.project = project;
    }

    /** One compiled output that may form a Java module. */
    protected static final class ModuleOutput {
        private final File root;
        private final JavaModuleInfo moduleInfo;

        private ModuleOutput(File root, JavaModuleInfo moduleInfo) {
            this.root = root;
            this.moduleInfo = moduleInfo;
        }

        /** The directory the module's classes are compiled to. */
        File root() {
            return root;
        }

        /** The parsed module descriptor, or {@code null} if {@link #root()} has no {@code module-info.class}. */
        JavaModuleInfo moduleInfo() {
            return moduleInfo;
        }
    }

    /** The directory the main classes (and any {@code module-info.class}) are compiled to. */
    protected File outputDirectory() {
        return new File(project.getBuild().getOutputDirectory());
    }

    /**
     * Discover the project's module outputs.
     *
     * <p>If the output directory itself holds a {@code module-info.class} (classic layout), exactly
     * that one output is returned. Otherwise, if at least one first-level subdirectory holds a
     * {@code module-info.class} (Maven 4 module source hierarchy), every first-level
     * subdirectory is returned as one output each — including non-modular ones, whose
     * {@link ModuleOutput#moduleInfo()} is {@code null}, so rules can flag them. Failing both, the
     * output directory is returned as a single non-modular output.
     *
     * @throws EnforcerRuleException if a {@code module-info.class} exists but cannot be read
     */
    protected List<ModuleOutput> moduleOutputs() throws EnforcerRuleException {
        File outputDirectory = outputDirectory();
        JavaModuleInfo topLevel = readModuleInfo(outputDirectory);
        if (topLevel != null) {
            return Collections.singletonList(new ModuleOutput(outputDirectory, topLevel));
        }
        File[] subdirectories = outputDirectory.listFiles(File::isDirectory);
        if (subdirectories != null) {
            Arrays.sort(subdirectories);
            List<ModuleOutput> outputs = new ArrayList<>();
            boolean modular = false;
            for (File subdirectory : subdirectories) {
                JavaModuleInfo moduleInfo = readModuleInfo(subdirectory);
                modular |= moduleInfo != null;
                outputs.add(new ModuleOutput(subdirectory, moduleInfo));
            }
            if (modular) {
                getLog().debug("Detected module source hierarchy layout below " + outputDirectory + " ("
                        + outputs.size() + " module output(s))");
                return outputs;
            }
        }
        // Nothing in the output directory. A plugin such as Moditect injects the descriptor into the
        // packaged artifact and never writes target/classes, so the artifact is the only place left to
        // look. Only reached for the classic one-module layout: a module source hierarchy returns above,
        // since one main artifact cannot stand for several modules.
        JavaModuleInfo packaged = readPackagedArtifactModuleInfo();
        if (packaged != null) {
            getLog().debug("No module-info.class below " + outputDirectory
                    + ", using the descriptor carried by the packaged artifact");
        }
        return Collections.singletonList(new ModuleOutput(outputDirectory, packaged));
    }

    /**
     * Read the module descriptor from the project's packaged artifact, for projects whose descriptor is
     * injected after compilation. Prefers the root {@code module-info.class} and otherwise takes the
     * highest-versioned multi-release descriptor.
     *
     * @return the parsed descriptor, or {@code null} when there is no artifact to inspect (the rule runs
     *         before {@code package}), it carries no descriptor, or it cannot be read as an archive --
     *         an unreadable artifact therefore keeps the stricter outcome
     */
    private JavaModuleInfo readPackagedArtifactModuleInfo() throws EnforcerRuleException {
        Artifact artifact = project.getArtifact();
        File file = artifact != null ? artifact.getFile() : null;
        if (file == null || !file.isFile()) {
            getLog().debug("No packaged artifact available to inspect for a module descriptor");
            return null;
        }
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = descriptorEntry(jar);
            if (entry == null) {
                return null;
            }
            requireJava9Runtime();
            try (InputStream in = jar.getInputStream(entry)) {
                return JavaModuleInfoReader.read(in);
            }
        } catch (IOException e) {
            getLog().debug("Could not read a module descriptor from " + file + ": " + e.getMessage());
            return null;
        }
    }

    /** The module descriptor entry of a JAR: the root one, else the highest-versioned one, else {@code null}. */
    private static JarEntry descriptorEntry(JarFile jar) {
        JarEntry root = jar.getJarEntry("module-info.class");
        if (root != null) {
            return root;
        }
        return jar.stream()
                .map(candidate -> {
                    Matcher matcher = VERSIONED_MODULE_INFO.matcher(candidate.getName());
                    return matcher.matches() ? new Object[] {Integer.valueOf(matcher.group(1)), candidate} : null;
                })
                .filter(java.util.Objects::nonNull)
                .max(java.util.Comparator.comparingInt(pair -> (Integer) pair[0]))
                .map(pair -> (JarEntry) pair[1])
                .orElse(null);
    }

    /**
     * Guard invoked once a {@code module-info.class} is known to be present in an output directory:
     * only a Java 9+ runtime can parse it, so fail fast with a clear message on Java 8 instead of the
     * obscure "failed to read" error that {@code ModuleDescriptor.read} would otherwise surface.
     * Non-modular outputs never reach this guard, so the rules keep their documented behaviour there
     * (do nothing, or — for {@code requireExplicitModules} — report the missing module). The Enforcer
     * plugin itself keeps its Java 8 baseline; only reading a module descriptor needs 9+.
     *
     * @throws EnforcerRuleException if the runtime is Java 8 or older
     */
    private void requireJava9Runtime() throws EnforcerRuleException {
        if (runtimeMajorVersion() < 9) {
            throw new EnforcerRuleException("The " + ruleName() + " rule requires the build to run on Java 9 "
                    + "or later; a module-info.class can only be read on a Java 9+ runtime. The current runtime is "
                    + "Java " + System.getProperty("java.specification.version") + ". The Enforcer plugin itself "
                    + "still supports Java 8 — only the Java module rules need 9+.");
        }
    }

    /**
     * The major version of the running JVM, detected in a Java 8-safe way: {@code java.specification.version}
     * is {@code "1.8"} on Java 8 and {@code "9"}, {@code "17"}, … from Java 9 onwards.
     */
    private static int runtimeMajorVersion() {
        String spec = System.getProperty("java.specification.version", "");
        if (spec.startsWith("1.")) {
            spec = spec.substring(2); // "1.8" -> "8"
        }
        int dot = spec.indexOf('.');
        if (dot >= 0) {
            spec = spec.substring(0, dot);
        }
        try {
            return Integer.parseInt(spec);
        } catch (NumberFormatException e) {
            return 9; // unrecognized (newer) format: assume modern and let the read proceed
        }
    }

    /**
     * Read the module descriptor of one output directory.
     *
     * @return the parsed {@link JavaModuleInfo}, or {@code null} if the directory has no
     *         {@code module-info.class}
     * @throws EnforcerRuleException if a {@code module-info.class} exists but cannot be read
     */
    private JavaModuleInfo readModuleInfo(File directory) throws EnforcerRuleException {
        File moduleInfo = new File(directory, "module-info.class");
        if (!moduleInfo.isFile()) {
            getLog().debug("No module-info.class in " + directory);
            return null;
        }
        // A module-info.class is present but can only be read on a Java 9+ runtime: fail with a clear
        // message here rather than letting ModuleDescriptor.read surface an obscure error on Java 8.
        requireJava9Runtime();
        try (InputStream in = Files.newInputStream(moduleInfo.toPath())) {
            return JavaModuleInfoReader.read(in);
        } catch (IOException e) {
            throw new EnforcerRuleException("Failed to read " + moduleInfo + ": " + e.getMessage(), e);
        }
    }

    /**
     * {@code true} if the directory holds at least one compiled class other than the module descriptor.
     *
     * @throws EnforcerRuleException if the directory cannot be scanned
     */
    protected static boolean hasCompiledClasses(File outputDirectory) throws EnforcerRuleException {
        Path root = outputDirectory.toPath();
        if (!Files.isDirectory(root)) {
            return false;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .anyMatch(name -> name.endsWith(".class") && !name.equals("module-info.class"));
        } catch (UncheckedIOException | IOException e) {
            throw new EnforcerRuleException("Failed to scan " + outputDirectory + ": " + e.getMessage(), e);
        }
    }

    /**
     * Warn when the rule had nothing at all to inspect, so that it cannot report success silently.
     *
     * <p>These rules read the compiled module descriptor, which only exists once the project has been
     * built, and -- where a plugin injects the descriptor into the packaged artifact -- only once it has
     * been packaged. An {@code enforce} execution bound too early therefore finds nothing and passes
     * without having checked anything, which is indistinguishable from a clean result in the log.
     *
     * <p>Stays silent where having nothing to inspect is the documented, correct outcome: a project
     * that builds no JAR (a {@code pom} aggregator compiles no classes, and no other packaging can carry
     * a module descriptor), and any project that was built but is simply not modular -- recognised by
     * the presence of compiled classes.
     */
    protected void warnIfNothingInspected(List<ModuleOutput> outputs) throws EnforcerRuleException {
        if (!buildsJarArtifact()) {
            return;
        }
        boolean anyClasses = false;
        for (ModuleOutput output : outputs) {
            if (output.moduleInfo() != null) {
                return;
            }
            anyClasses |= hasCompiledClasses(output.root());
        }
        String rule = configurationName();
        Optional<String> injector = descriptorInjectingPlugin();
        if (anyClasses && !injector.isPresent()) {
            // Built, no descriptor anywhere, nothing suggests one is injected later: the project simply
            // is not a module, which only requireExplicitModules has an opinion about.
            return;
        }
        String cause = injector.map(plugin -> "the descriptor appears to be injected into the packaged artifact by "
                        + plugin + ", which has not run yet")
                .orElse("there are no compiled classes under " + outputDirectory());
        getLog().warn(rule + " checked nothing: " + cause
                + ". Bind the enforce execution to a phase at which this project's module descriptor is "
                + "already available; see " + DOCUMENTATION_BASE_URL + rule + ".html");
    }

    /**
     * The {@code artifactId} of a configured plugin known to inject a module descriptor after compilation,
     * recognised by coordinates or by its goal. Affects only the wording of a warning, never a verdict.
     */
    private Optional<String> descriptorInjectingPlugin() {
        for (Plugin plugin : project.getBuildPlugins()) {
            if (DESCRIPTOR_INJECTING_PLUGINS.contains(plugin.getKey())) {
                return Optional.of(plugin.getArtifactId());
            }
            for (PluginExecution execution : plugin.getExecutions()) {
                if (execution.getGoals().contains(DESCRIPTOR_INJECTING_GOAL)) {
                    return Optional.of(plugin.getArtifactId());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * {@code true} if the project builds a JAR, the only kind of artifact that can carry a Java module
     * descriptor. Derived from the artifact handler rather than a list of packaging names, so packagings
     * that produce a JAR under another name -- {@code maven-plugin}, {@code bundle}, {@code ejb} -- are
     * covered, while {@code pom}, {@code war} and {@code ear} are not.
     */
    private boolean buildsJarArtifact() {
        Artifact artifact = project.getArtifact();
        ArtifactHandler handler = artifact != null ? artifact.getArtifactHandler() : null;
        return handler != null && "jar".equals(handler.getExtension());
    }

    /** The rule's name as it is written in a plugin configuration, such as {@code requireMinimalExports}. */
    private String configurationName() {
        String name = ruleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /** Short rule name for log messages, such as {@code RequireMinimalExports}. */
    protected String ruleName() {
        return getClass().getSimpleName();
    }
}
