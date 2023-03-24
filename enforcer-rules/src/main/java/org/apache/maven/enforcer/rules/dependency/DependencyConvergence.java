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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.eclipse.aether.graph.DependencyNode;

import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

/**
 * @author <a href="mailto:rex@e-hoffman.org">Rex Hoffman</a>
 */
@Named("dependencyConvergence")
public final class DependencyConvergence extends AbstractStandardEnforcerRule {

    // parameters

    private boolean uniqueVersions;

    private List<String> includes;

    private List<String> excludes;

    private List<String> excludedScopes = Arrays.asList(SCOPE_TEST, SCOPE_PROVIDED);

    // parameters - end

    private DependencyVersionMap dependencyVersionMap;

    private final ResolverUtil resolverUtil;

    @Inject
    public DependencyConvergence(ResolverUtil resolverUtil) {
        this.resolverUtil = Objects.requireNonNull(resolverUtil);
    }

    @Override
    public void execute() throws EnforcerRuleException {

        DependencyNode node = resolverUtil.resolveTransitiveDependenciesVerbose(excludedScopes);
        dependencyVersionMap = new DependencyVersionMap().setUniqueVersions(uniqueVersions);
        node.accept(dependencyVersionMap);

        List<String> errorMsgs =
                getConvergenceErrorMsgs(dependencyVersionMap.getConflictedVersionNumbers(includes, excludes));

        if (!errorMsgs.isEmpty()) {
            throw new EnforcerRuleException("Failed while enforcing releasability." + System.lineSeparator()
                    + String.join(System.lineSeparator(), errorMsgs));
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
                "DependencyConvergence[includes=%s, excludes=%s, uniqueVersions=%b]",
                includes, excludes, uniqueVersions);
    }
}
