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
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
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
        return Collections.singletonList(new ModuleOutput(outputDirectory, null));
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
        for (ModuleOutput output : outputs) {
            if (output.moduleInfo() != null || hasCompiledClasses(output.root())) {
                return;
            }
        }
        String rule = configurationName();
        getLog().warn(rule + " found no compiled classes under " + outputDirectory()
                + " and therefore checked nothing. Bind the enforce execution to a phase at which this "
                + "project's module descriptor is already available; see " + DOCUMENTATION_BASE_URL + rule
                + ".html");
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
