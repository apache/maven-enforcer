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

import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

import static java.util.Optional.ofNullable;
import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

/**
 * Resolver helper class.
 */
@Named
class ResolveUtil {

    private final RepositorySystem repositorySystem;
    private final MavenSession session;

    /**
     * Default constructor
     */
    @Inject
    ResolveUtil(RepositorySystem repositorySystem, MavenSession session) {
        this.repositorySystem = Objects.requireNonNull(repositorySystem);
        this.session = Objects.requireNonNull(session);
    }

    /**
     * Retrieves the {@link DependencyNode} instance containing the result of the transitive dependency
     * for the current {@link MavenProject}.
     *
     * @param selectors zero or more {@link DependencySelector} instances
     * @return a Dependency Node which is the root of the project's dependency tree
     * @throws EnforcerRuleException thrown if the lookup fails
     */
    DependencyNode resolveTransitiveDependencies(DependencySelector... selectors) throws EnforcerRuleException {
        if (selectors.length == 0) {
            selectors = new DependencySelector[] {
                new ScopeDependencySelector(SCOPE_TEST, SCOPE_PROVIDED),
                new OptionalDependencySelector(),
                new ExclusionDependencySelector()
            };
        }
        try {
            MavenProject project = session.getCurrentProject();
            ArtifactTypeRegistry artifactTypeRegistry =
                    session.getRepositorySession().getArtifactTypeRegistry();

            DefaultRepositorySystemSession repositorySystemSession =
                    new DefaultRepositorySystemSession(session.getRepositorySession());
            repositorySystemSession.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true);
            repositorySystemSession.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
            repositorySystemSession.setDependencySelector(new AndDependencySelector(selectors));

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
        } catch (DependencyCollectionException e) {
            throw new EnforcerRuleException("Could not build dependency tree " + e.getLocalizedMessage(), e);
        }
    }
}
