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

import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
import org.apache.maven.enforcer.rules.utils.ArtifactMatcher;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

import static java.util.Optional.ofNullable;

/**
 * This rule bans all transitive dependencies. There is a configuration option to exclude certain artifacts from being
 * checked.
 *
 * @author Jakub Senko
 */
@Named("banTransitiveDependencies")
public final class BanTransitiveDependencies extends AbstractStandardEnforcerRule {

    /**
     * Specify the dependencies that will be ignored. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version][:type][:scope]</code>. Wildcard '*' can be used to in place of specific
     * section (ie group:*:1.0 will match both 'group:artifact:1.0' and 'group:anotherArtifact:1.0') <br>
     * You can override this patterns by using includes. Version is a string representing standard maven version range.
     * Empty patterns will be ignored.
     */
    private List<String> excludes;

    /**
     * Specify the dependencies that will be checked. These are exceptions to excludes intended for more convenient and
     * finer settings. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version][:type][:scope]</code>. Wildcard '*' can be used to in place of specific
     * section (ie group:*:1.0 will match both 'group:artifact:1.0' and 'group:anotherArtifact:1.0') <br>
     * Version is a string representing standard maven version range. Empty patterns will be ignored.
     */
    private List<String> includes;

    private final MavenSession session;

    private final ResolverUtil resolverUtil;

    @Inject
    public BanTransitiveDependencies(MavenSession session, ResolverUtil resolverUtil) {
        this.session = Objects.requireNonNull(session);
        this.resolverUtil = Objects.requireNonNull(resolverUtil);
    }

    /**
     * Searches dependency tree recursively for transitive dependencies that are not excluded, while generating nice
     * info message along the way.
     */
    private static boolean searchTree(
            DependencyNode node,
            int level,
            ArtifactMatcher excludes,
            Set<Dependency> directDependencies,
            StringBuilder message) {

        List<DependencyNode> children = node.getChildren();

        /*
         * if the node is deeper than direct dependency and is empty, it is transitive.
         */
        boolean hasTransitiveDependencies = level > 1;

        boolean excluded = false;

        /*
         * holds recursive message from children, will be appended to current message if this node has any transitive
         * descendants if message is null, don't generate recursive message.
         */
        StringBuilder messageFromChildren = message == null ? null : new StringBuilder();

        if (excludes.match(ArtifactUtils.toArtifact(node))) {
            // is excluded, we don't care about descendants
            excluded = true;
            hasTransitiveDependencies = false;
        } else if (directDependencies.contains(node.getDependency())) {
            hasTransitiveDependencies = false;
        } else {
            for (DependencyNode childNode : children) {
                /*
                 * if any of the children has transitive d. so does the parent
                 */
                hasTransitiveDependencies = hasTransitiveDependencies
                        || searchTree(childNode, level + 1, excludes, directDependencies, messageFromChildren);
            }
        }

        if ((excluded || hasTransitiveDependencies) && message != null) // then generate message
        {
            message.append(StringUtils.repeat("   ", level)).append(node.getArtifact());

            if (excluded) {
                message.append(" [excluded]").append(System.lineSeparator());
            }

            if (hasTransitiveDependencies) {
                if (level > 0) {
                    message.append(" has transitive dependencies:");
                }

                message.append(System.lineSeparator()).append(messageFromChildren);
            }
        }

        return hasTransitiveDependencies;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        ArtifactTypeRegistry artifactTypeRegistry =
                session.getRepositorySession().getArtifactTypeRegistry();
        ArtifactMatcher exclusions = new ArtifactMatcher(excludes, includes);
        Set<Dependency> directDependencies = session.getCurrentProject().getDependencies().stream()
                .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                .collect(Collectors.toSet());

        DependencyNode rootNode = resolverUtil.resolveTransitiveDependencies();
        StringBuilder generatedMessage = new StringBuilder();
        if (searchTree(rootNode, 0, exclusions, directDependencies, generatedMessage)) {
            throw new EnforcerRuleException(ofNullable(getMessage()).orElse(generatedMessage.toString()));
        }
    }

    @Override
    public String toString() {
        return String.format("BanTransitiveDependencies[message=%s, excludes=%s]", getMessage(), excludes);
    }
}
