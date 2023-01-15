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

import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.apache.maven.execution.MavenSession;

/**
 * This rule checks that lists of plugins are not included.
 *
 * @author <a href="mailto:velo.br@gmail.com">Marvin Froeder</a>
 */
@Named("bannedPlugins")
public final class BannedPlugins extends AbstractStandardEnforcerRule {

    /**
     * Specify the banned plugins. This can be a list of plugins in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard
     * by using '*' (ie group:*:1.0) <br>
     * The rule will fail if any plugin matches any exclude, unless it also matches
     * an include rule.
     */
    private List<String> excludes = null;

    /**
     * Specify the allowed plugins. This can be a list of plugins in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard
     * by using '*' (ie group:*:1.0) <br>
     * Includes override the exclude rules. It is meant to allow wide exclusion rules
     * with wildcards and still allow a
     * smaller set of includes. <br>
     * For example, to ban all xerces except xerces-api -&gt; exclude "xerces", include "xerces:xerces-api"
     */
    private List<String> includes = null;

    private final MavenSession session;

    @Inject
    public BannedPlugins(MavenSession session) {
        this.session = Objects.requireNonNull(session);
    }

    @Override
    public void execute() throws EnforcerRuleException {

        String result = session.getCurrentProject().getPluginArtifacts().stream()
                .filter(a -> !validate(a))
                .collect(
                        StringBuilder::new,
                        (messageBuilder, node) ->
                                messageBuilder.append(node.getId()).append(" <--- banned plugin"),
                        (m1, m2) -> m1.append(m2.toString()))
                .toString();
        if (!result.isEmpty()) {
            throw new EnforcerRuleException(result);
        }
    }

    private boolean validate(Artifact artifact) {
        return !ArtifactUtils.matchDependencyArtifact(artifact, excludes)
                || ArtifactUtils.matchDependencyArtifact(artifact, includes);
    }

    @Override
    public String toString() {
        return String.format("BannedPlugins[excludes=%s, includes=%s]", excludes, includes);
    }
}
