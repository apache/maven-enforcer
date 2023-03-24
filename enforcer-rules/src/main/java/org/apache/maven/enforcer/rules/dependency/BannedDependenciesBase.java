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
package org.apache.maven.enforcer.rules.dependency;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.graph.DependencyNode;

/**
 * Abstract base class for rules which validate the transitive
 * dependency tree by traversing all children and validating every
 * dependency artifact.
 */
abstract class BannedDependenciesBase extends AbstractStandardEnforcerRule {

    /**
     * Specify the banned dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard
     * by using '*' (ie group:*:1.0) <br>
     * The rule will fail if any dependency matches any exclude, unless it also matches
     * an include rule.
     *
     * @see #setExcludes(List)
     * @see #getExcludes()
     */
    private List<String> excludes = null;

    /**
     * Specify the allowed dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard
     * by using '*' (ie group:*:1.0) <br>
     * Includes override the exclude rules. It is meant to allow wide exclusion rules
     * with wildcards and still allow a
     * smaller set of includes. <br>
     * For example, to ban all xerces except xerces-api -&gt; exclude "xerces", include "xerces:xerces-api"
     *
     * @see #setIncludes(List)
     * @see #getIncludes()
     */
    private List<String> includes = null;

    /** Specify if transitive dependencies should be searched (default) or only look at direct dependencies. */
    private boolean searchTransitive = true;

    private final MavenSession session;

    private final ResolverUtil resolverUtil;

    BannedDependenciesBase(MavenSession session, ResolverUtil resolverUtil) {
        this.session = Objects.requireNonNull(session);
        this.resolverUtil = Objects.requireNonNull(resolverUtil);
    }

    protected MavenSession getSession() {
        return session;
    }

    @Override
    public void execute() throws EnforcerRuleException {

        if (!searchTransitive) {
            String result = session.getCurrentProject().getDependencyArtifacts().stream()
                    .filter(a -> !validate(a))
                    .collect(
                            StringBuilder::new,
                            (messageBuilder, node) -> messageBuilder
                                    .append(System.lineSeparator())
                                    .append(node.getId())
                                    .append(" <--- ")
                                    .append(getErrorMessage()),
                            (m1, m2) -> m1.append(m2.toString()))
                    .toString();
            if (!result.isEmpty()) {
                String message = "";
                if (getMessage() != null) {
                    message = getMessage() + System.lineSeparator();
                }
                throw new EnforcerRuleException(message + result);
            }
        } else {
            StringBuilder messageBuilder = new StringBuilder();
            DependencyNode rootNode = resolverUtil.resolveTransitiveDependenciesVerbose(Collections.emptyList());
            if (!validate(rootNode, 0, messageBuilder)) {
                String message = "";
                if (getMessage() != null) {
                    message = getMessage() + System.lineSeparator();
                }
                throw new EnforcerRuleException(message + messageBuilder);
            }
        }
    }

    protected boolean validate(DependencyNode node, int level, StringBuilder messageBuilder) {
        boolean rootFailed = level > 0 && !validate(ArtifactUtils.toArtifact(node));
        StringBuilder childMessageBuilder = new StringBuilder();
        if (rootFailed
                || !node.getChildren().stream()
                        .map(childNode -> validate(childNode, level + 1, childMessageBuilder))
                        .reduce(true, Boolean::logicalAnd)) {
            messageBuilder
                    .append(StringUtils.repeat("   ", level))
                    .append(ArtifactUtils.toArtifact(node).getId());
            if (rootFailed) {
                messageBuilder.append(" <--- ").append(getErrorMessage());
            }
            messageBuilder.append(System.lineSeparator()).append(childMessageBuilder);
            return false;
        }
        return true;
    }

    protected abstract String getErrorMessage();

    /**
     * Validates a dependency artifact if it fulfills the enforcer rule
     *
     * @param dependency dependency to be checked against the list of excludes
     * @return {@code true} if the dependency <b>passes</b> the rule, {@code false} if the dependency
     * triggers a validation error
     */
    protected abstract boolean validate(Artifact dependency);

    /**
     * Sets the search transitive.
     *
     * @param theSearchTransitive the searchTransitive to set
     */
    public void setSearchTransitive(boolean theSearchTransitive) {
        this.searchTransitive = theSearchTransitive;
    }

    /**
     * Gets the excludes.
     *
     * @return the excludes
     */
    public List<String> getExcludes() {
        return this.excludes;
    }

    /**
     * Specify the banned dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard
     * by using '*' (ie group:*:1.0) <br>
     * The rule will fail if any dependency matches any exclude, unless it also matches an
     * include rule.
     *
     * @see #getExcludes()
     * @param theExcludes the excludes to set
     */
    public void setExcludes(List<String> theExcludes) {
        this.excludes = theExcludes;
    }

    /**
     * Gets the includes.
     *
     * @return the includes
     */
    public List<String> getIncludes() {
        return this.includes;
    }

    /**
     * Specify the allowed dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard
     * by using '*' (ie group:*:1.0) <br>
     * Includes override the exclude rules. It is meant to allow wide exclusion rules with
     * wildcards and still allow a
     * smaller set of includes. <br>
     * For example, to ban all xerces except xerces-api → exclude "xerces",
     * include "xerces:xerces-api"
     *
     * @see #setIncludes(List)
     * @param theIncludes the includes to set
     */
    public void setIncludes(List<String> theIncludes) {
        this.includes = theIncludes;
    }

    public boolean isSearchTransitive() {
        return searchTransitive;
    }
}
