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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.apache.maven.enforcer.rules.utils.ParentNodeProvider;
import org.apache.maven.enforcer.rules.utils.ParentsVisitor;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;

import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

/**
 * Rule to enforce that the resolved dependency is also the most recent one of all transitive dependencies.
 *
 * @author Geoffrey De Smet
 * @since 1.1
 */
@Named("requireUpperBoundDeps")
public final class RequireUpperBoundDeps extends AbstractStandardEnforcerRule {

    /**
     * @since 1.3
     */
    private boolean uniqueVersions;

    /**
     * Dependencies to ignore.
     *
     * @since TBD
     */
    private List<String> excludes = null;

    /**
     * Dependencies to include.
     *
     * @since 3.0.0
     */
    private List<String> includes = null;

    /**
     * Scope to exclude.
     */
    private List<String> excludedScopes = Arrays.asList(SCOPE_TEST, SCOPE_PROVIDED);

    private RequireUpperBoundDepsVisitor upperBoundDepsVisitor;

    private final ResolverUtil resolverUtil;

    @Inject
    public RequireUpperBoundDeps(ResolverUtil resolverUtil) {
        this.resolverUtil = Objects.requireNonNull(resolverUtil);
    }

    /**
     * Sets dependencies to exclude.
     * @param excludes a list of {@code groupId:artifactId} names
     */
    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    /**
     * Sets dependencies to include.
     *
     * @param includes a list of {@code groupId:artifactId} names
     */
    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        DependencyNode node = resolverUtil.resolveTransitiveDependenciesVerbose(excludedScopes);
        upperBoundDepsVisitor = new RequireUpperBoundDepsVisitor()
                .setUniqueVersions(uniqueVersions)
                .setIncludes(includes);
        getLog().debug(() -> resolverUtil.dumpTree(node));
        node.accept(upperBoundDepsVisitor);
        List<String> errorMessages = buildErrorMessages(upperBoundDepsVisitor.getConflicts());
        if (!errorMessages.isEmpty()) {
            throw new EnforcerRuleException(
                    "Failed while enforcing RequireUpperBoundDeps. The error(s) are " + errorMessages);
        }
    }

    private List<String> buildErrorMessages(List<List<DependencyNode>> conflicts) {
        List<String> errorMessages = new ArrayList<>(conflicts.size());
        for (List<DependencyNode> conflict : conflicts) {
            org.eclipse.aether.artifact.Artifact artifact = conflict.get(0).getArtifact();
            String groupArt = artifact.getGroupId() + ":" + artifact.getArtifactId();
            if (excludes != null && excludes.contains(groupArt)) {
                getLog().info("Ignoring requireUpperBoundDeps in " + groupArt);
            } else {
                errorMessages.add(buildErrorMessage(conflict));
            }
        }
        return errorMessages;
    }

    private String buildErrorMessage(List<DependencyNode> conflict) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage
                .append(System.lineSeparator())
                .append("Require upper bound dependencies error for ")
                .append(getFullArtifactName(conflict.get(0), false))
                .append(" paths to dependency are:")
                .append(System.lineSeparator());
        if (conflict.size() > 0) {
            errorMessage.append(buildTreeString(conflict.get(0)));
        }
        for (DependencyNode node : conflict.subList(1, conflict.size())) {
            errorMessage.append("and").append(System.lineSeparator());
            errorMessage.append(buildTreeString(node));
        }
        return errorMessage.toString();
    }

    private StringBuilder buildTreeString(DependencyNode node) {
        List<String> loc = new ArrayList<>();
        DependencyNode currentNode = node;
        while (currentNode != null) {
            StringBuilder line = new StringBuilder(getFullArtifactName(currentNode, false));

            if (DependencyManagerUtils.getPremanagedVersion(currentNode) != null) {
                line.append(" (managed) <-- ");
                line.append(getFullArtifactName(currentNode, true));
            }

            loc.add(line.toString());
            currentNode = upperBoundDepsVisitor.getParent(currentNode);
        }
        Collections.reverse(loc);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < loc.size(); i++) {
            for (int j = 0; j < i; j++) {
                builder.append("  ");
            }
            builder.append("+-").append(loc.get(i));
            builder.append(System.lineSeparator());
        }
        return builder;
    }

    private String getFullArtifactName(DependencyNode node, boolean usePremanaged) {
        Artifact artifact = ArtifactUtils.toArtifact(node);

        String version = DependencyManagerUtils.getPremanagedVersion(node);
        if (!usePremanaged || version == null) {
            version = uniqueVersions ? artifact.getVersion() : artifact.getBaseVersion();
        }
        String result = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + version;

        String classifier = artifact.getClassifier();
        if (classifier != null && !classifier.isEmpty()) {
            result += ":" + classifier;
        }

        String scope = artifact.getScope();
        if (scope != null && !"compile".equals(scope)) {
            result += " [" + scope + ']';
        }

        return result;
    }

    private static class RequireUpperBoundDepsVisitor implements DependencyVisitor, ParentNodeProvider {

        private final ParentsVisitor parentsVisitor = new ParentsVisitor();
        private boolean uniqueVersions;
        private List<String> includes = null;

        public RequireUpperBoundDepsVisitor setUniqueVersions(boolean uniqueVersions) {
            this.uniqueVersions = uniqueVersions;
            return this;
        }

        public RequireUpperBoundDepsVisitor setIncludes(List<String> includes) {
            this.includes = includes;
            return this;
        }

        private final Map<String, List<DependencyNodeHopCountPair>> keyToPairsMap = new HashMap<>();

        @Override
        public boolean visitEnter(DependencyNode node) {
            parentsVisitor.visitEnter(node);
            DependencyNodeHopCountPair pair = new DependencyNodeHopCountPair(node, this);
            String key = pair.constructKey();

            if (includes != null && !includes.isEmpty() && !includes.contains(key)) {
                return true;
            }

            keyToPairsMap.computeIfAbsent(key, k1 -> new ArrayList<>()).add(pair);
            keyToPairsMap.get(key).sort(DependencyNodeHopCountPair::compareTo);
            return true;
        }

        @Override
        public boolean visitLeave(DependencyNode node) {
            return parentsVisitor.visitLeave(node);
        }

        public List<List<DependencyNode>> getConflicts() {
            List<List<DependencyNode>> output = new ArrayList<>();
            for (List<DependencyNodeHopCountPair> pairs : keyToPairsMap.values()) {
                if (containsConflicts(pairs)) {
                    List<DependencyNode> outputSubList = new ArrayList<>(pairs.size());
                    for (DependencyNodeHopCountPair pair : pairs) {
                        outputSubList.add(pair.getNode());
                    }
                    output.add(outputSubList);
                }
            }
            return output;
        }

        private boolean containsConflicts(List<DependencyNodeHopCountPair> pairs) {
            DependencyNodeHopCountPair resolvedPair = pairs.get(0);
            ArtifactVersion resolvedVersion = resolvedPair.extractArtifactVersion(uniqueVersions, false);

            for (DependencyNodeHopCountPair pair : pairs) {
                ArtifactVersion version = pair.extractArtifactVersion(uniqueVersions, true);
                if (resolvedVersion.compareTo(version) < 0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public DependencyNode getParent(DependencyNode node) {
            return parentsVisitor.getParent(node);
        }
    }

    private static class DependencyNodeHopCountPair implements Comparable<DependencyNodeHopCountPair> {
        private final DependencyNode node;
        private int hopCount;
        private final ParentNodeProvider parentNodeProvider;

        private DependencyNodeHopCountPair(DependencyNode node, ParentNodeProvider parentNodeProvider) {
            this.parentNodeProvider = parentNodeProvider;
            this.node = node;
            countHops();
        }

        private void countHops() {
            hopCount = 0;
            DependencyNode parent = parentNodeProvider.getParent(node);
            while (parent != null) {
                hopCount++;
                parent = parentNodeProvider.getParent(parent);
            }
        }

        private String constructKey() {
            Artifact artifact = ArtifactUtils.toArtifact(node);
            return artifact.getGroupId() + ":" + artifact.getArtifactId();
        }

        public DependencyNode getNode() {
            return node;
        }

        private ArtifactVersion extractArtifactVersion(boolean uniqueVersions, boolean usePremanagedVersion) {
            if (usePremanagedVersion && DependencyManagerUtils.getPremanagedVersion(node) != null) {
                return new DefaultArtifactVersion(DependencyManagerUtils.getPremanagedVersion(node));
            }

            Artifact artifact = ArtifactUtils.toArtifact(node);
            String version = uniqueVersions ? artifact.getVersion() : artifact.getBaseVersion();
            if (version != null) {
                return new DefaultArtifactVersion(version);
            }
            try {
                return artifact.getSelectedVersion();
            } catch (OverConstrainedVersionException e) {
                throw new RuntimeException("Version ranges problem with " + node.getArtifact(), e);
            }
        }

        public int getHopCount() {
            return hopCount;
        }

        public int compareTo(DependencyNodeHopCountPair other) {
            return Integer.compare(hopCount, other.getHopCount());
        }
    }
}
