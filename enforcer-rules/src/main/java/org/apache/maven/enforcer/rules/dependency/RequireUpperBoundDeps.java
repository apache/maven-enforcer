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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.apache.maven.enforcer.rules.utils.ParentNodeProvider;
import org.apache.maven.enforcer.rules.utils.ParentsVisitor;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;

import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

/**
 * Rule to enforce that the resolved dependency is the most recent one of all transitive dependencies.
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
     * Scopes to exclude.
     */
    private List<String> excludedScopes = Arrays.asList(SCOPE_TEST, SCOPE_PROVIDED);

    /**
     * Fail when a version set in the project's own {@code dependencyManagement} (or its parents') is above every
     * version requested in the dependency tree. Versions managed by an imported BOM and dependencies declared
     * directly by the project are not checked.
     *
     * @since 3.7.1
     */
    private boolean failWhenManagedVersionIsAboveRequested;

    private RequireUpperBoundDepsVisitor upperBoundDepsVisitor;

    private final ResolverUtil resolverUtil;

    private final MavenProject project;

    @Inject
    public RequireUpperBoundDeps(ResolverUtil resolverUtil, MavenProject project) {
        this.resolverUtil = Objects.requireNonNull(resolverUtil);
        this.project = Objects.requireNonNull(project);
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

    /**
     * Sets whether to fail when a version managed by the project is above every requested version.
     *
     * @param failWhenManagedVersionIsAboveRequested {@code true} to fail in that case
     */
    public void setFailWhenManagedVersionIsAboveRequested(boolean failWhenManagedVersionIsAboveRequested) {
        this.failWhenManagedVersionIsAboveRequested = failWhenManagedVersionIsAboveRequested;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        // Test-scope paths can still win mediation for a dependency also used by an included path.
        List<String> scopesExcludedDuringResolution =
                excludedScopes == null ? new ArrayList<>() : new ArrayList<>(excludedScopes);
        scopesExcludedDuringResolution.removeAll(Collections.singleton(SCOPE_TEST));
        DependencyNode node = resolverUtil.resolveTransitiveDependenciesVerbose(scopesExcludedDuringResolution);
        upperBoundDepsVisitor = new RequireUpperBoundDepsVisitor()
                .setUniqueVersions(uniqueVersions)
                .setExcludedScopes(excludedScopes)
                .setIncludes(includes);
        getLog().debug(() -> resolverUtil.dumpTree(node));
        node.accept(upperBoundDepsVisitor);
        List<String> errorMessages = buildErrorMessages(upperBoundDepsVisitor.getConflicts());
        if (failWhenManagedVersionIsAboveRequested) {
            errorMessages.addAll(buildManagedVersionAboveRequestedErrorMessages(node));
        }
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
                errorMessages.add(buildErrorMessage(conflict, ""));
            }
        }
        return errorMessages;
    }

    private List<String> buildManagedVersionAboveRequestedErrorMessages(DependencyNode root) {
        DependencyManagement dependencyManagement = project.getDependencyManagement();
        if (dependencyManagement == null) {
            return Collections.emptyList();
        }
        Set<String> directDependencyKeys = root.getChildren().stream()
                .map(child -> child.getArtifact().getGroupId() + ":"
                        + child.getArtifact().getArtifactId())
                .collect(Collectors.toSet());
        Set<String> projectModelIds = collectProjectModelIds();
        List<String> errorMessages = new ArrayList<>();
        for (org.apache.maven.model.Dependency managedDependency : dependencyManagement.getDependencies()) {
            String key = managedDependency.getGroupId() + ":" + managedDependency.getArtifactId();
            if (directDependencyKeys.contains(key) || !isManagedBy(managedDependency, projectModelIds)) {
                continue;
            }
            List<DependencyNode> nodes =
                    upperBoundDepsVisitor.findNodesManagedAboveRequestedVersions(managedDependency);
            if (nodes.isEmpty()) {
                continue;
            }
            if (excludes != null && excludes.contains(key)) {
                getLog().info("Ignoring requireUpperBoundDeps in " + key);
                continue;
            }
            errorMessages.add(buildErrorMessage(nodes, ". The managed version is above every requested version"));
        }
        return errorMessages;
    }

    private Set<String> collectProjectModelIds() {
        Set<String> modelIds = new HashSet<>();
        for (MavenProject lineageProject = project;
                lineageProject != null;
                lineageProject = lineageProject.getParent()) {
            modelIds.add(toModelId(lineageProject.getOriginalModel()));
        }
        return modelIds;
    }

    /**
     * Builds the id Maven gives a raw model in the input locations it tracks.
     */
    private String toModelId(Model model) {
        String groupId = model.getGroupId();
        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }
        String version = model.getVersion();
        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }
        return groupId + ":" + model.getArtifactId() + ":" + version;
    }

    private boolean isManagedBy(org.apache.maven.model.Dependency managedDependency, Set<String> modelIds) {
        InputLocation location = managedDependency.getLocation("version");
        if (location == null || location.getSource() == null) {
            return true;
        }
        return modelIds.contains(location.getSource().getModelId());
    }

    private String buildErrorMessage(List<DependencyNode> conflict, String diagnosis) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage
                .append(System.lineSeparator())
                .append("Require upper bound dependencies error for ")
                .append(getFullArtifactName(conflict.get(0), false))
                .append(diagnosis)
                .append(". Paths to dependency are:")
                .append(System.lineSeparator());

        if (!conflict.isEmpty()) {
            errorMessage.append(buildTreeString(conflict.get(0)));
        }
        for (DependencyNode node : conflict.subList(1, conflict.size())) {
            errorMessage.append("and").append(System.lineSeparator());
            errorMessage.append(buildTreeString(node));
        }

        appendDependencyManagementSuggestion(errorMessage, conflict);

        return errorMessage.toString();
    }

    private void appendDependencyManagementSuggestion(StringBuilder errorMessage, List<DependencyNode> conflict) {
        if (conflict == null || conflict.isEmpty()) {
            return;
        }

        org.eclipse.aether.artifact.Artifact artifact = conflict.get(0).getArtifact();
        ArtifactVersion highestVersion = null;

        for (DependencyNode node : conflict) {
            String versionStr = DependencyManagerUtils.getPremanagedVersion(node);
            if (versionStr == null) {
                org.eclipse.aether.artifact.Artifact nodeArtifact = node.getArtifact();
                versionStr = uniqueVersions ? nodeArtifact.getVersion() : nodeArtifact.getBaseVersion();
            }

            ArtifactVersion version = new DefaultArtifactVersion(versionStr);
            if (highestVersion == null || version.compareTo(highestVersion) > 0) {
                highestVersion = version;
            }
        }

        if (highestVersion == null) {
            return;
        }

        errorMessage
                .append(System.lineSeparator())
                .append("To fix this, add the following to your <dependencyManagement> section:")
                .append(System.lineSeparator())
                .append("<dependencyManagement>")
                .append(System.lineSeparator())
                .append("  <dependencies>")
                .append(System.lineSeparator())
                .append("    <dependency>")
                .append(System.lineSeparator())
                .append("      <groupId>")
                .append(artifact.getGroupId())
                .append("</groupId>")
                .append(System.lineSeparator())
                .append("      <artifactId>")
                .append(artifact.getArtifactId())
                .append("</artifactId>")
                .append(System.lineSeparator())
                .append("      <version>")
                .append(highestVersion)
                .append("</version>")
                .append(System.lineSeparator());

        String classifier = artifact.getClassifier();
        if (classifier != null && !classifier.isEmpty()) {
            errorMessage
                    .append("      <classifier>")
                    .append(classifier)
                    .append("</classifier>")
                    .append(System.lineSeparator());
        }

        errorMessage
                .append("    </dependency>")
                .append(System.lineSeparator())
                .append("  </dependencies>")
                .append(System.lineSeparator())
                .append("</dependencyManagement>")
                .append(System.lineSeparator());
    }

    private StringBuilder buildTreeString(DependencyNode node) {
        List<String> loc = new ArrayList<>();
        DependencyNode currentNode = node;
        while (currentNode != null) {
            StringBuilder line = new StringBuilder(getFullArtifactName(currentNode, false));

            if (DependencyManagerUtils.getPremanagedVersion(currentNode) != null) {
                line.append(" (version managed from ");
                line.append(DependencyManagerUtils.getPremanagedVersion(currentNode));
                line.append(")");
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
        private List<String> excludedScopes = Collections.emptyList();
        private List<String> includes = null;

        public RequireUpperBoundDepsVisitor setUniqueVersions(boolean uniqueVersions) {
            this.uniqueVersions = uniqueVersions;
            return this;
        }

        public RequireUpperBoundDepsVisitor setExcludedScopes(List<String> excludedScopes) {
            this.excludedScopes = excludedScopes != null ? excludedScopes : Collections.emptyList();
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
                if (containsNonExcludedScope(pairs) && containsConflicts(pairs)) {
                    List<DependencyNode> outputSubList = new ArrayList<>(pairs.size());
                    for (DependencyNodeHopCountPair pair : pairs) {
                        outputSubList.add(pair.getNode());
                    }
                    output.add(outputSubList);
                }
            }
            return output;
        }

        /**
         * Finds the nodes managed to the version of {@code managedDependency} when that version is above
         * every version they request.
         *
         * @return the managed nodes, or an empty list when one of them requests the managed version or above
         */
        List<DependencyNode> findNodesManagedAboveRequestedVersions(
                org.apache.maven.model.Dependency managedDependency) {
            String key = managedDependency.getGroupId() + ":" + managedDependency.getArtifactId();
            String classifier = Objects.toString(managedDependency.getClassifier(), "");
            List<DependencyNodeHopCountPair> pairs = keyToPairsMap.getOrDefault(key, Collections.emptyList()).stream()
                    .filter(pair ->
                            classifier.equals(pair.getNode().getArtifact().getClassifier()))
                    .collect(Collectors.toList());
            if (pairs.isEmpty() || !containsNonExcludedScope(pairs)) {
                return Collections.emptyList();
            }
            ArtifactVersion managedVersion = new DefaultArtifactVersion(managedDependency.getVersion());
            if (pairs.stream().anyMatch(pair -> pair.requestsAtLeast(managedVersion, uniqueVersions))) {
                return Collections.emptyList();
            }
            return pairs.stream().map(DependencyNodeHopCountPair::getNode).collect(Collectors.toList());
        }

        private boolean containsNonExcludedScope(List<DependencyNodeHopCountPair> pairs) {
            return pairs.stream().anyMatch(pair -> {
                Dependency dependency = pair.getNode().getDependency();
                return dependency == null || !excludedScopes.contains(dependency.getScope());
            });
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

    private static final class DependencyNodeHopCountPair implements Comparable<DependencyNodeHopCountPair> {
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

        private boolean requestsAtLeast(ArtifactVersion managedVersion, boolean uniqueVersions) {
            String premanagedVersion = DependencyManagerUtils.getPremanagedVersion(node);
            if (premanagedVersion == null) {
                return extractArtifactVersion(uniqueVersions, false).compareTo(managedVersion) >= 0;
            }
            VersionRange requestedRange;
            try {
                requestedRange = VersionRange.createFromVersionSpec(premanagedVersion);
            } catch (InvalidVersionSpecificationException e) {
                throw new RuntimeException("Invalid version " + premanagedVersion + " requested for " + node, e);
            }
            if (requestedRange.getRecommendedVersion() == null) {
                return requestedRange.containsVersion(managedVersion);
            }
            return requestedRange.getRecommendedVersion().compareTo(managedVersion) >= 0;
        }

        public int getHopCount() {
            return hopCount;
        }

        @Override
        public int compareTo(DependencyNodeHopCountPair other) {
            return Integer.compare(hopCount, other.getHopCount());
        }
    }
}
