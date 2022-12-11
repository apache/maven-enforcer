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

import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.ArtifactUtils;
import org.apache.maven.plugins.enforcer.utils.DependencyVersionMap;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;

/**
 * @author <a href="mailto:rex@e-hoffman.org">Rex Hoffman</a>
 */
public class DependencyConvergence implements EnforcerRule {
    private static Log log;

    private boolean uniqueVersions;

    private List<String> includes;

    private List<String> excludes;

    private DependencyVersionMap dependencyVersionMap;

    public void setUniqueVersions(boolean uniqueVersions) {
        this.uniqueVersions = uniqueVersions;
    }

    @Override
    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        if (log == null) {
            log = helper.getLog();
        }
        try {
            DependencyNode node = ArtifactUtils.resolveTransitiveDependencies(
                    helper,
                    // TODO: use a modified version of ExclusionDependencySelector to process excludes and includes
                    new DependencySelector() {
                        @Override
                        public boolean selectDependency(Dependency dependency) {
                            // regular OptionalDependencySelector only discriminates optional dependencies at level 2+
                            return !dependency.isOptional()
                                    // regular scope selectors only discard transitive dependencies
                                    // and always allow direct dependencies
                                    && !dependency.getScope().equals(SCOPE_TEST)
                                    && !dependency.getScope().equals(SCOPE_PROVIDED);
                        }

                        @Override
                        public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
                            return this;
                        }
                    },
                    // process dependency exclusions
                    new ExclusionDependencySelector());
            dependencyVersionMap = new DependencyVersionMap(log).setUniqueVersions(uniqueVersions);
            node.accept(dependencyVersionMap);

            List<CharSequence> errorMsgs = new ArrayList<>(
                    getConvergenceErrorMsgs(dependencyVersionMap.getConflictedVersionNumbers(includes, excludes)));
            for (CharSequence errorMsg : errorMsgs) {
                log.warn(errorMsg);
            }
            if (errorMsgs.size() > 0) {
                throw new EnforcerRuleException(
                        "Failed while enforcing releasability. " + "See above detailed error message.");
            }
        } catch (Exception e) {
            throw new EnforcerRuleException(e.getLocalizedMessage(), e);
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
    public String getCacheId() {
        return "";
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    @Override
    public boolean isResultValid(EnforcerRule ignored) {
        return false;
    }
}
