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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;

import static java.util.Optional.ofNullable;
import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

/**
 * Resolver helper class.
 */
@Named
class ResolverUtil {

    private final RepositorySystem repositorySystem;

    private final MavenSession session;

    /**
     * Default constructor
     */
    @Inject
    ResolverUtil(RepositorySystem repositorySystem, MavenSession session) {
        this.repositorySystem = Objects.requireNonNull(repositorySystem);
        this.session = Objects.requireNonNull(session);
    }

    /**
     * Retrieves the {@link DependencyNode} instance containing the result of the transitive dependency
     * for the current {@link MavenProject} in verbose mode.
     * <p>
     * In verbose mode all nodes participating in a conflict are retained.
     * </p>
     * <p>
     * Please consult {@link ConflictResolver} and {@link DependencyManagerUtils}>
     * </p>
     *
     * @param excludedScopes the scopes of direct dependencies to ignore
     * @return a Dependency Node which is the root of the project's dependency tree
     * @throws EnforcerRuleException thrown if the lookup fails
     */
    DependencyNode resolveTransitiveDependenciesVerbose(List<String> excludedScopes) throws EnforcerRuleException {
        return resolveTransitiveDependencies(true, true, excludedScopes);
    }

    /**
     * Retrieves the {@link DependencyNode} instance containing the result of the transitive dependency
     * for the current {@link MavenProject}.
     *
     * @return a Dependency Node which is the root of the project's dependency tree
     * @throws EnforcerRuleException thrown if the lookup fails
     */
    DependencyNode resolveTransitiveDependencies() throws EnforcerRuleException {
        return resolveTransitiveDependencies(false, true, Arrays.asList(SCOPE_TEST, SCOPE_PROVIDED));
    }

    /**
     * Retrieves the {@link DependencyNode} instance containing the result of the transitive dependency
     * for the current {@link MavenProject}.
     *
     * @param excludeOptional ignore optional project artifacts
     * @param excludedScopes the scopes of direct dependencies to ignore
     * @return a Dependency Node which is the root of the project's dependency tree
     * @throws EnforcerRuleException thrown if the lookup fails
     */
    DependencyNode resolveTransitiveDependencies(boolean excludeOptional, List<String> excludedScopes)
            throws EnforcerRuleException {
        return resolveTransitiveDependencies(false, excludeOptional, excludedScopes);
    }

    DependencyNode resolveTransitiveDependencies(boolean verbose, boolean excludeOptional, List<String> excludedScopes)
            throws EnforcerRuleException {

        try {
            RepositorySystemSession repositorySystemSession = session.getRepositorySession();

            if (verbose) {
                DefaultRepositorySystemSession defaultRepositorySystemSession =
                        new DefaultRepositorySystemSession(repositorySystemSession);
                defaultRepositorySystemSession.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true);
                defaultRepositorySystemSession.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
                repositorySystemSession = defaultRepositorySystemSession;
            }

            MavenProject project = session.getCurrentProject();
            ArtifactTypeRegistry artifactTypeRegistry =
                    session.getRepositorySession().getArtifactTypeRegistry();

            List<Dependency> dependencies = project.getDependencies().stream()
                    .filter(d -> !(excludeOptional && d.isOptional()))
                    .filter(d -> !excludedScopes.contains(d.getScope()))
                    .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                    .collect(Collectors.toList());

            List<Dependency> managedDependencies = ofNullable(project.getDependencyManagement())
                    .map(DependencyManagement::getDependencies)
                    .map(list -> list.stream()
                            .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                            .collect(Collectors.toList()))
                    .orElse(null);

            CollectRequest collectRequest =
                    new CollectRequest(dependencies, managedDependencies, project.getRemoteProjectRepositories());
            collectRequest.setRootArtifact(RepositoryUtils.toArtifact(project.getArtifact()));

            return repositorySystem
                    .collectDependencies(repositorySystemSession, collectRequest)
                    .getRoot();

        } catch (DependencyCollectionException e) {
            throw new EnforcerRuleException("Could not build dependency tree " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Dump a {@link DependencyNode} as a tree.
     *
     * @param rootNode node to inspect
     * @return dependency tree as String
     */
    public CharSequence dumpTree(DependencyNode rootNode) {
        StringBuilder result = new StringBuilder(System.lineSeparator());

        rootNode.accept(new TreeDependencyVisitor(new DependencyVisitor() {
            String indent = "";

            @Override
            public boolean visitEnter(org.eclipse.aether.graph.DependencyNode dependencyNode) {
                result.append(indent);
                result.append("Node: ").append(dependencyNode);
                result.append(" data map: ").append(dependencyNode.getData());
                result.append(System.lineSeparator());
                indent += "  ";
                return true;
            }

            @Override
            public boolean visitLeave(org.eclipse.aether.graph.DependencyNode dependencyNode) {
                indent = indent.substring(0, indent.length() - 2);
                return true;
            }
        }));

        return result;
    }
}
