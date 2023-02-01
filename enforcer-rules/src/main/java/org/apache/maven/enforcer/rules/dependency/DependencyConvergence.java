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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;

/**
 * @author <a href="mailto:rex@e-hoffman.org">Rex Hoffman</a>
 */
@Named("dependencyConvergence")
public final class DependencyConvergence extends AbstractStandardEnforcerRule {

    private boolean uniqueVersions;

    private List<String> includes;

    private List<String> excludes;

    private List<String> scopes = Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME, Artifact.SCOPE_SYSTEM);

    private DependencyVersionMap dependencyVersionMap;

    private final ResolveUtil resolveUtil;

    @Inject
    public DependencyConvergence(ResolveUtil resolveUtil) {
        this.resolveUtil = Objects.requireNonNull(resolveUtil);
    }

    @Override
    public void execute() throws EnforcerRuleException {

        DependencyNode node = resolveUtil.resolveTransitiveDependencies(
                // TODO: use a modified version of ExclusionDependencySelector to process excludes and includes
                new DependencySelector() {
                    @Override
                    public boolean selectDependency(Dependency dependency) {
                        // regular OptionalDependencySelector only discriminates optional dependencies at level 2+
                        return !dependency.isOptional()
                                // regular scope selectors only discard transitive dependencies
                                // and always allow direct dependencies
                                && scopes.contains(dependency.getScope());
                    }

                    @Override
                    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
                        return this;
                    }
                },
                // process dependency exclusions
                new ExclusionDependencySelector());
        dependencyVersionMap = new DependencyVersionMap().setUniqueVersions(uniqueVersions);
        node.accept(dependencyVersionMap);

        List<CharSequence> errorMsgs = new ArrayList<>(
                getConvergenceErrorMsgs(dependencyVersionMap.getConflictedVersionNumbers(includes, excludes)));
        for (CharSequence errorMsg : errorMsgs) {
            getLog().warnOrError(errorMsg);
        }
        if (errorMsgs.size() > 0) {
            throw new EnforcerRuleException(
                    "Failed while enforcing releasability. " + "See above detailed error message.");
        }
    }

    private StringBuilder buildTreeString(DependencyNode node) {
        List<String> loc = new ArrayList<>();
        DependencyNode currentNode = node;
        while (currentNode != null) {
            // ArtifactUtils.toArtifact(node) adds scope and optional information, if present
            loc.add(ArtifactUtils.toArtifact(currentNode).toString());
            currentNode = dependencyVersionMap.getParent(currentNode);
        }
        Collections.reverse(loc);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < loc.size(); i++) {
            for (int j = 0; j < i; j++) {
                builder.append("  ");
            }
            builder.append("+-").append(loc.get(i)).append(System.lineSeparator());
        }
        return builder;
    }

    private List<String> getConvergenceErrorMsgs(List<List<DependencyNode>> errors) {
        List<String> errorMsgs = new ArrayList<>();
        for (List<DependencyNode> nodeList : errors) {
            errorMsgs.add(buildConvergenceErrorMsg(nodeList));
        }
        return errorMsgs;
    }

    private String buildConvergenceErrorMsg(List<DependencyNode> nodeList) {
        StringBuilder builder = new StringBuilder();
        builder.append(System.lineSeparator())
                .append("Dependency convergence error for ")
                .append(nodeList.get(0).getArtifact().toString())
                .append(" paths to dependency are:")
                .append(System.lineSeparator());
        if (nodeList.size() > 0) {
            builder.append(buildTreeString(nodeList.get(0)));
        }
        for (DependencyNode node : nodeList.subList(1, nodeList.size())) {
            builder.append("and").append(System.lineSeparator()).append(buildTreeString(node));
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return String.format(
                "DependencyConvergence[includes=%s, excludes=%s, uniqueVersions=%b, scopes=%s]",
                includes, excludes, uniqueVersions, String.join(",", scopes));
    }
}
