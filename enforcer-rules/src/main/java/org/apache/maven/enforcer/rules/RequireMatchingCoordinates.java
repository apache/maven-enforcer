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
import java.util.regex.Pattern;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

/**
 * This rule checks that the Maven coordinates (i.e. the project's {@code groupId} and {@code artifactId}) each match a given pattern.
 * @since 3.5.0
 */
@Named("requireMatchingCoordinates")
public final class RequireMatchingCoordinates extends AbstractStandardEnforcerRule {

    private Pattern groupIdPattern;

    private Pattern artifactIdPattern;

    private boolean moduleNameMustMatchArtifactId;

    private final MavenProject project;

    @Inject
    public RequireMatchingCoordinates(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    @Override
    public void execute() throws EnforcerRuleException {
        StringBuilder msgBuilder = new StringBuilder();
        if (groupIdPattern != null
                && !groupIdPattern.matcher(project.getGroupId()).matches()) {
            msgBuilder
                    .append("Group ID must match pattern \"")
                    .append(groupIdPattern)
                    .append("\" but is \"")
                    .append(project.getGroupId())
                    .append("\"");
        }
        if (artifactIdPattern != null
                && !artifactIdPattern.matcher(project.getArtifactId()).matches()) {
            if (msgBuilder.length() > 0) {
                msgBuilder.append(System.lineSeparator());
            }
            msgBuilder
                    .append("Artifact ID must match pattern \"")
                    .append(artifactIdPattern)
                    .append("\" but is \"")
                    .append(project.getArtifactId())
                    .append("\"");
        }
        if (moduleNameMustMatchArtifactId
                && !project.isExecutionRoot()
                && !project.getBasedir().getName().equals(project.getArtifactId())) {
            if (msgBuilder.length() > 0) {
                msgBuilder.append(System.lineSeparator());
            }
            msgBuilder
                    .append("Module directory name must be equal to its artifact ID \"")
                    .append(project.getArtifactId())
                    .append("\" but is \"")
                    .append(project.getBasedir().getName())
                    .append("\"");
        }
        if (msgBuilder.length() > 0) {
            throw new EnforcerRuleException(msgBuilder.toString());
        }
    }

    public void setGroupIdPattern(String groupIdPattern) {
        this.groupIdPattern = Pattern.compile(groupIdPattern);
    }

    public void setArtifactIdPattern(String artifactIdPattern) {
        this.artifactIdPattern = Pattern.compile(artifactIdPattern);
    }

    public void setModuleNameMustMatchArtifactId(boolean moduleNameMustMatchArtifactId) {
        this.moduleNameMustMatchArtifactId = moduleNameMustMatchArtifactId;
    }

    @Override
    public String toString() {
        return String.format(
                "requireMatchingCoordinates[groupIdPattern=%s, artifactIdPattern=%s, moduleNameMustMatchArtifactId=%b]",
                groupIdPattern, artifactIdPattern, moduleNameMustMatchArtifactId);
    }
}
