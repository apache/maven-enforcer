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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

/**
 * Requires that a project with compiled classes is an <em>explicit</em> Java module, i.e. that its
 * main output contains a {@code module-info.class}. This prevents a modular application from silently
 * consuming a dependency as an <em>automatic module</em> (a plain JAR placed on the module path),
 * whose auto-derived name and "exports everything" semantics are unstable across releases.
 *
 * <p>The rule does nothing for projects that compile no classes (e.g. {@code pom} aggregators or a
 * module that only carries resources), so it can be enabled ecosystem-wide without special-casing.
 * In a Maven&nbsp;4 module source hierarchy every per-module output directory is checked.
 */
@Named("requireExplicitModules")
public final class RequireExplicitModules extends AbstractModuleInfoRule {

    @Inject
    public RequireExplicitModules(MavenProject project) {
        super(project);
    }

    @Override
    public void execute() throws EnforcerRuleException {
        List<String> violations = new ArrayList<>();
        for (ModuleOutput output : moduleOutputs()) {
            if (output.moduleInfo() != null) {
                continue;
            }
            if (!hasCompiledClasses(output.root())) {
                getLog().debug("No compiled classes in " + output.root() + "; skipping " + ruleName());
                continue;
            }
            violations.add(output.root().getPath());
        }
        if (!violations.isEmpty()) {
            String message = getMessage();
            throw new EnforcerRuleException(
                    message != null
                            ? message
                            : "Project '" + project.getArtifactId() + "' is not an explicit Java module: no "
                                    + "module-info.class in " + String.join(", ", violations)
                                    + ". Add a module-info.java so the "
                                    + "artifact is a named module and never resolves as an automatic module.");
        }
    }

    /** {@code true} if the output directory holds at least one compiled class other than the module descriptor. */
    private static boolean hasCompiledClasses(File outputDirectory) throws EnforcerRuleException {
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

    @Override
    public String toString() {
        return String.format("RequireExplicitModules[message=%s]", getMessage());
    }
}
