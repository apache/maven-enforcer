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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

/**
 * Requires that a project with compiled classes is an <em>explicit</em> Java module. Its main output
 * must contain a {@code module-info.class}. This prevents a modular application from silently consuming
 * a dependency as an <em>automatic module</em> (a plain JAR placed on the module path). Such a module's
 * auto-derived name and "exports everything" semantics are unstable across releases.
 *
 * <p>The rule does nothing for projects that compile no classes (such as {@code pom} aggregators or a
 * module that only carries resources), so it can be enabled ecosystem-wide without special-casing.
 * In a Maven 4 module source hierarchy every per-module output directory is checked.
 *
 * <p>Because the constraint is about the <em>artifact</em>, a descriptor injected after compilation
 * counts: if the compiled output has none but the packaged artifact does, the project passes. See
 * {@link #packagedArtifactIsModular()}.
 */
@Named("requireExplicitModules")
public final class RequireExplicitModules extends AbstractModuleInfoRule {

    @Inject
    public RequireExplicitModules(MavenProject project) {
        super(project);
    }

    @Override
    public void execute() throws EnforcerRuleException {
        List<ModuleOutput> outputs = moduleOutputs();
        List<String> violations = new ArrayList<>();
        for (ModuleOutput output : outputs) {
            if (output.moduleInfo() != null) {
                continue;
            }
            if (!hasCompiledClasses(output.root())) {
                getLog().debug("No compiled classes in " + output.root() + "; skipping " + ruleName());
                continue;
            }
            violations.add(output.root().getPath());
        }
        if (violations.isEmpty()) {
            warnIfNothingInspected(outputs);
            return;
        }
        String message = getMessage();
        throw new EnforcerRuleException(
                message != null
                        ? message
                        : "Project '" + project.getArtifactId() + "' is not an explicit Java module: no "
                                + "module-info.class in " + String.join(", ", violations)
                                + ". Add a module-info.java so the "
                                + "artifact is a named module and never resolves as an automatic module. "
                                + "If the descriptor is injected into the artifact after compilation (for "
                                + "example by Moditect), bind this rule after the package phase so the "
                                + "artifact itself can be inspected.");
    }

    @Override
    public String toString() {
        return String.format("RequireExplicitModules[message=%s]", getMessage());
    }
}
