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
package org.apache.maven.plugins.enforcer.utils;

import static java.util.Optional.ofNullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugins.enforcer.utils.ArtifactMatcher.Pattern;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

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
     * Retrieves the {@link DependencyNode} instance containing the result of the transitive dependency
     * for the current {@link MavenProject}.
     *
     * @param helper (may not be null) an instance of the {@link EnforcerRuleHelper} class
     * @param selectors zero or more {@link DependencySelector} instances
     * @return a Dependency Node which is the root of the project's dependency tree
     * @throws EnforcerRuleException thrown if the lookup fails
     */
    public static DependencyNode resolveTransitiveDependencies(
            EnforcerRuleHelper helper, DependencySelector... selectors) throws EnforcerRuleException {
        try {
            RepositorySystem repositorySystem = helper.getComponent(RepositorySystem.class);
            MavenSession session = (MavenSession) helper.evaluate("${session}");
            MavenProject project = session.getCurrentProject();
            ArtifactTypeRegistry artifactTypeRegistry =
                    session.getRepositorySession().getArtifactTypeRegistry();

            DefaultRepositorySystemSession repositorySystemSession =
                    new DefaultRepositorySystemSession(session.getRepositorySession());
            repositorySystemSession.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true);
            repositorySystemSession.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
            if (selectors.length > 0) {
                repositorySystemSession.setDependencySelector(new AndDependencySelector(selectors));
            }

            CollectRequest collectRequest = new CollectRequest(
                    project.getDependencies().stream()
                            .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                            .collect(Collectors.toList()),
                    ofNullable(project.getDependencyManagement())
                            .map(DependencyManagement::getDependencies)
                            .map(list -> list.stream()
                                    .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                                    .collect(Collectors.toList()))
                            .orElse(null),
                    project.getRemoteProjectRepositories());
            Artifact artifact = project.getArtifact();
            collectRequest.setRootArtifact(RepositoryUtils.toArtifact(artifact));

            return repositorySystem
                    .collectDependencies(repositorySystemSession, collectRequest)
                    .getRoot();
        } catch (ExpressionEvaluationException | ComponentLookupException e) {
            throw new EnforcerRuleException("Unable to lookup a component " + e.getLocalizedMessage(), e);
        } catch (DependencyCollectionException e) {
            throw new EnforcerRuleException("Could not build dependency tree " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * <p>Retrieves all <em>child</em> dependency artifacts from the given {@link DependencyNode} and returns them
     * as a set of {@link Artifact}.</p>
     * <p><u>Note:</u>&nbsp;Thus, the result will not contain the root artifact.</p>
     * @param node root node
     * @return set of all <em>child</em> dependency artifacts
     */
    public static Set<Artifact> getDependencyArtifacts(DependencyNode node) {
        return getDependencyArtifacts(node, new HashSet<>());
    }

    private static Set<Artifact> getDependencyArtifacts(DependencyNode node, Set<Artifact> set) {
        node.getChildren().forEach(child -> {
            set.add(toArtifact(child));
            getDependencyArtifacts(child, set);
        });
        return set;
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
    static boolean compareDependency(String pattern, Artifact artifact) {
        return new Pattern(pattern).match(artifact);
    }
}
