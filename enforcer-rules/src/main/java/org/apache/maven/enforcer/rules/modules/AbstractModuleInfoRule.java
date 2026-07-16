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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
 *   <li><b>Module source hierarchy</b> (Maven&nbsp;4, POM model 4.1.0): one Maven project compiles
 *       several modules, each to its own subdirectory
 *       {@code ${project.build.outputDirectory}/<module-name>/module-info.class}.</li>
 * </ul>
 *
 * Subclasses call {@link #moduleOutputs()} and enforce a specific constraint on each returned
 * {@link ModuleOutput}.
 */
abstract class AbstractModuleInfoRule extends AbstractStandardEnforcerRule {

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
     * {@code module-info.class} (Maven&nbsp;4 module source hierarchy), every first-level
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
        try (InputStream in = Files.newInputStream(moduleInfo.toPath())) {
            return JavaModuleInfoReader.read(in);
        } catch (IOException e) {
            throw new EnforcerRuleException("Failed to read " + moduleInfo + ": " + e.getMessage(), e);
        }
    }

    /** Short rule name for log messages, e.g. {@code RequireMinimalExports}. */
    protected String ruleName() {
        return getClass().getSimpleName();
    }
}
