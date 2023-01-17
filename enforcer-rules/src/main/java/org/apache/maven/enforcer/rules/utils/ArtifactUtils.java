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
package org.apache.maven.enforcer.rules.utils;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.eclipse.aether.graph.DependencyNode;

import static java.util.Optional.ofNullable;

/**
 *
 * @author Robert Scholte
 * @since 3.0.0
 */
public final class ArtifactUtils {

    /**
     * Converts {@link DependencyNode} to {@link Artifact}; in comparison
     * to {@link RepositoryUtils#toArtifact(org.eclipse.aether.artifact.Artifact)}, this method
     * assigns {@link Artifact#getScope()} and {@link Artifact#isOptional()} based on
     * the dependency information from the node.
     *
     * @param node {@link DependencyNode} to convert to {@link Artifact}
     * @return target artifact
     */
    public static Artifact toArtifact(DependencyNode node) {
        Artifact artifact = RepositoryUtils.toArtifact(node.getArtifact());
        ofNullable(node.getDependency()).ifPresent(dependency -> {
            ofNullable(dependency.getScope()).ifPresent(artifact::setScope);
            artifact.setOptional(dependency.isOptional());
        });
        return artifact;
    }

    /**
     * Returns a subset of dependency artifacts that match the given collection of patterns
     *
     * @param dependencies dependency artifacts to match against patterns
     * @param patterns patterns to match against the artifacts
     * @return a set containing artifacts matching one of the patterns or <code>null</code>
     * @throws EnforcerRuleException the enforcer rule exception
     */
    public static Set<Artifact> filterDependencyArtifacts(Set<Artifact> dependencies, Collection<String> patterns)
            throws EnforcerRuleException {
        try {
            return ofNullable(patterns)
                    .map(collection -> collection.stream()
                            .map(p -> p.split(":"))
                            .map(StringUtils::stripAll)
                            .map(arr -> String.join(":", arr))
                            .flatMap(pattern ->
                                    dependencies.stream().filter(artifact -> compareDependency(pattern, artifact)))
                            .collect(Collectors.toSet()))
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            if (e.getCause() instanceof InvalidVersionSpecificationException) {
                throw new EnforcerRuleException(e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Checks if the given dependency artifact matches the given collection of patterns
     *
     * @param artifact dependency artifact to match against patterns
     * @param patterns patterns to match against the artifacts
     * @return {@code true} if the given artifact matches the set of patterns
     */
    public static boolean matchDependencyArtifact(Artifact artifact, Collection<String> patterns) {
        try {
            return ofNullable(patterns)
                    .map(collection -> collection.stream()
                            .map(p -> p.split(":"))
                            .map(StringUtils::stripAll)
                            .map(arr -> String.join(":", arr))
                            .anyMatch(pattern -> compareDependency(pattern, artifact)))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            if (e.getCause() instanceof InvalidVersionSpecificationException) {
                throw new IllegalArgumentException(e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Compares the given pattern against the given artifact. The pattern should follow the format
     * <code>groupId:artifactId:version:type:scope:classifier</code>.
     *
     * @param pattern The pattern to compare the artifact with.
     * @param artifact the artifact
     * @return <code>true</code> if the artifact matches one of the patterns
     */
    public static boolean compareDependency(String pattern, Artifact artifact) {
        return new ArtifactMatcher.Pattern(pattern).match(artifact);
    }
}
