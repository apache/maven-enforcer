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
package org.apache.maven.plugins.enforcer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerRule2;
import org.apache.maven.model.InputLocation;
import org.apache.maven.project.MavenProject;

/**
 * The Class AbstractStandardEnforcerRule.
 */
public abstract class AbstractStandardEnforcerRule implements EnforcerRule2 {

    /**
     * Specify a friendly message if the rule fails.
     *
     * @see {@link #setMessage(String)}
     * @see {@link #getMessage()}
     */
    private String message;

    private EnforcerLevel level = EnforcerLevel.ERROR;

    public final void setMessage(String message) {
        this.message = message;
    }

    public final String getMessage() {
        return message;
    }

    @Override
    public EnforcerLevel getLevel() {
        return level;
    }

    public void setLevel(EnforcerLevel level) {
        this.level = level;
    }

    /**
     * Returns an identifier of a given project.
     * @param project the project
     * @return the identifier of the project in the format {@code <groupId>:<artifactId>:<version>}
     */
    private static String getProjectId(MavenProject project) {
        StringBuilder buffer = new StringBuilder(128);

        buffer.append(
                (project.getGroupId() != null && project.getGroupId().length() > 0)
                        ? project.getGroupId()
                        : "[unknown-group-id]");
        buffer.append(':');
        buffer.append(
                (project.getArtifactId() != null && project.getArtifactId().length() > 0)
                        ? project.getArtifactId()
                        : "[unknown-artifact-id]");
        buffer.append(':');
        buffer.append(
                (project.getVersion() != null && project.getVersion().length() > 0)
                        ? project.getVersion()
                        : "[unknown-version]");

        return buffer.toString();
    }

    /**
     * Creates a string with line/column information for problems originating directly from this POM. Inspired by
     * {@code o.a.m.model.building.ModelProblemUtils.formatLocation(...)}.
     *
     * @param project the current project.
     * @param location The location which should be formatted, must not be {@code null}.
     * @return The formatted problem location or an empty string if unknown, never {@code null}.
     */
    protected static String formatLocation(MavenProject project, InputLocation location) {
        StringBuilder buffer = new StringBuilder();

        if (!location.getSource().getModelId().equals(getProjectId(project))) {
            buffer.append(location.getSource().getModelId());

            if (location.getSource().getLocation().length() > 0) {
                if (buffer.length() > 0) {
                    buffer.append(", ");
                }
                buffer.append(location.getSource().getLocation());
            }
        }
        if (location.getLineNumber() > 0) {
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append("line ").append(location.getLineNumber());
        }
        if (location.getColumnNumber() > 0) {
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append("column ").append(location.getColumnNumber());
        }
        return buffer.toString();
    }
}
