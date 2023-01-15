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
package org.apache.maven.enforcer.rules;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Objects;
import java.util.Optional;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

/**
 * This rule checks that the current project is not a release.
 */
@Named("requireSnapshotVersion")
public final class RequireSnapshotVersion extends AbstractStandardEnforcerRule {

    /**
     * Allows this rule to fail when the parent is defined as a release.
     */
    private boolean failWhenParentIsRelease = true;

    private final MavenProject project;

    @Inject
    public RequireSnapshotVersion(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    @Override
    public void execute() throws EnforcerRuleException {

        Artifact artifact = project.getArtifact();

        if (!artifact.isSnapshot()) {
            String message = getMessage();
            StringBuilder sb = new StringBuilder();
            if (message != null) {
                sb.append(message).append(System.lineSeparator());
            }
            sb.append("This project cannot be a release:").append(artifact.getId());
            throw new EnforcerRuleException(sb.toString());
        }
        if (failWhenParentIsRelease && project.hasParent()) {
            Artifact parentArtifact = Optional.ofNullable(project.getParent())
                    .map(MavenProject::getArtifact)
                    .orElse(null);
            if (parentArtifact != null && !parentArtifact.isSnapshot()) {
                throw new EnforcerRuleException("Parent cannot be a release: " + parentArtifact.getId());
            }
        }
    }

    public void setFailWhenParentIsRelease(boolean failWhenParentIsRelease) {
        this.failWhenParentIsRelease = failWhenParentIsRelease;
    }

    @Override
    public String toString() {
        return String.format(
                "RequireSnapshotVersion[message=%s, failWhenParentIsRelease=%b]",
                getMessage(), failWhenParentIsRelease);
    }
}
