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

import java.text.ChoiceFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
import org.apache.maven.enforcer.rules.utils.ArtifactMatcher;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionConstraint;

/**
 * This rule bans dependencies having a version which requires resolution (i.e. dynamic versions which might change with
 * each build). Dynamic versions are either
 * <ul>
 * <li>version ranges,</li>
 * <li>the special placeholders {@code LATEST} or {@code RELEASE} or</li>
 * <li>versions ending with {@code -SNAPSHOT}.
 * </ul>
 *
 * @since 3.2.0
 */
@Named("banDynamicVersions")
public final class BanDynamicVersions extends AbstractStandardEnforcerRule {

    private static final String RELEASE = "RELEASE";

    private static final String LATEST = "LATEST";

    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    /**
     * {@code true} if versions ending with {@code -SNAPSHOT} should be allowed
     */
    private boolean allowSnapshots;

    /**
     * {@code true} if versions using {@code LATEST} should be allowed
     */
    private boolean allowLatest;

    /**
     * {@code true} if versions using {@code RELEASE} should be allowed
     */
    private boolean allowRelease;

    /**
     * {@code true} if version ranges should be allowed
     */
    private boolean allowRanges;

    /**
     * {@code true} if ranges having the same upper and lower bound like {@code [1.0]} should be allowed.
     * Only applicable if {@link #allowRanges} is not set to {@code true}.
     */
    private boolean allowRangesWithIdenticalBounds;

    /**
     * {@code true} if optional dependencies should not be checked
     */
    private boolean excludeOptionals;

    /**
     * the scopes of dependencies which should be excluded from this rule
     */
    private List<String> excludedScopes = Collections.emptyList();

    /**
     * Specify the ignored dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId[:version[:type[:scope:[classifier]]]]]</code>.
     * Any of the sections can be a wildcard by using '*' (e.g. {@code group:*:1.0}).
     * <br>
     * Any of the ignored dependencies may have dynamic versions.
     */
    private List<String> ignores = null;

    /**
     * {@code true} if dependencies should be checked before Maven computes the final
     * dependency tree. Setting this property will make the rule check dependencies
     * before any conflicts are resolved. This is similar to the {@code verbose}
     * parameter for the {@code tree} goal for {@code maven-dependency-plugin}.
     */
    private boolean verbose;

    private final ResolverUtil resolverUtil;

    @Inject
    public BanDynamicVersions(
            MavenProject project, RepositorySystem repoSystem, MavenSession mavenSession, ResolverUtil resolverUtil) {
        this.resolverUtil = Objects.requireNonNull(resolverUtil);
    }

    private final class BannedDynamicVersionCollector implements DependencyFilter {

        private boolean isRoot = true;

        private List<String> violations;

        private final Predicate<DependencyNode> predicate;

        private GenericVersionScheme versionScheme;

        public List<String> getViolations() {
            return violations;
        }

        BannedDynamicVersionCollector(Predicate<DependencyNode> predicate) {
            this.predicate = predicate;
            this.violations = new ArrayList<>();
            this.versionScheme = new GenericVersionScheme();
        }

        private boolean isBannedDynamicVersion(VersionConstraint versionConstraint) {
            if (versionConstraint.getVersion() != null) {
                if (versionConstraint.getVersion().toString().equals(LATEST)) {
                    return !allowLatest;
                } else if (versionConstraint.getVersion().toString().equals(RELEASE)) {
                    return !allowRelease;
                } else if (versionConstraint.getVersion().toString().endsWith(SNAPSHOT_SUFFIX)) {
                    return !allowSnapshots;
                }
            } else if (versionConstraint.getRange() != null) {
                if (allowRangesWithIdenticalBounds
                        && Objects.equals(
                                versionConstraint.getRange().getLowerBound(),
                                versionConstraint.getRange().getUpperBound())) {
                    return false;
                }
                return !allowRanges;
            } else {
                getLog().warn("Unexpected version constraint found: " + versionConstraint);
            }
            return false;
        }

        @Override
        public boolean accept(DependencyNode node, List<DependencyNode> parents) {
            if (isRoot) {
                isRoot = false;
                return false;
            }
            getLog().debug("Found node " + node + " with version constraint " + node.getVersionConstraint());
            if (!predicate.test(node)) {
                return false;
            }
            VersionConstraint versionConstraint = node.getVersionConstraint();
            if (isBannedDynamicVersion(versionConstraint)) {
                addViolation(versionConstraint, node, parents);
                return true;
            }
            try {
                if (verbose) {
                    String premanagedVersion = DependencyManagerUtils.getPremanagedVersion(node);
                    if (premanagedVersion != null) {
                        VersionConstraint premanagedContraint = versionScheme.parseVersionConstraint(premanagedVersion);
                        if (isBannedDynamicVersion(premanagedContraint)) {
                            addViolation(premanagedContraint, node, parents);
                            return true;
                        }
                    }
                }
            } catch (InvalidVersionSpecificationException ex) {
                // This should never happen.
                throw new RuntimeException("Failed to parse version for " + node, ex);
            }
            return false;
        }

        private void addViolation(
                VersionConstraint versionContraint, DependencyNode node, List<DependencyNode> parents) {
            List<DependencyNode> intermediatePath = new ArrayList<>(parents);
            if (!intermediatePath.isEmpty()) {
                // This project is also included in the path, but we do
                // not want that in the report.
                intermediatePath.remove(intermediatePath.size() - 1);
            }
            violations.add("Dependency "
                    + node.getDependency()
                    + dumpIntermediatePath(intermediatePath)
                    + " is referenced with a banned dynamic version "
                    + versionContraint);
        }
    }

    @Override
    public void execute() throws EnforcerRuleException {

        try {
            DependencyNode rootDependency =
                    resolverUtil.resolveTransitiveDependencies(verbose, excludeOptionals, excludedScopes);

            List<String> violations = collectDependenciesWithBannedDynamicVersions(rootDependency);
            if (!violations.isEmpty()) {
                ChoiceFormat dependenciesFormat = new ChoiceFormat("1#dependency|1<dependencies");
                throw new EnforcerRuleException("Found " + violations.size() + " "
                        + dependenciesFormat.format(violations.size())
                        + " with dynamic versions." + System.lineSeparator()
                        + String.join(System.lineSeparator(), violations));
            }
        } catch (DependencyCollectionException e) {
            throw new EnforcerRuleException("Could not retrieve dependency metadata for project", e);
        }
    }

    private static String dumpIntermediatePath(Collection<DependencyNode> path) {
        if (path.isEmpty()) {
            return "";
        }
        return " via " + path.stream().map(n -> n.getArtifact().toString()).collect(Collectors.joining(" -> "));
    }

    private static final class ExcludeArtifactPatternsPredicate implements Predicate<DependencyNode> {

        private final ArtifactMatcher artifactMatcher;

        ExcludeArtifactPatternsPredicate(List<String> excludes) {
            this.artifactMatcher = new ArtifactMatcher(excludes, Collections.emptyList());
        }

        @Override
        public boolean test(DependencyNode depNode) {
            return !artifactMatcher.match(ArtifactUtils.toArtifact(depNode));
        }
    }

    private List<String> collectDependenciesWithBannedDynamicVersions(DependencyNode rootDependency)
            throws DependencyCollectionException {
        Predicate<DependencyNode> predicate;
        if (ignores != null && !ignores.isEmpty()) {
            predicate = new ExcludeArtifactPatternsPredicate(ignores);
        } else {
            predicate = d -> true;
        }
        BannedDynamicVersionCollector collector = new BannedDynamicVersionCollector(predicate);
        rootDependency.accept(new PathRecordingDependencyVisitor(collector));
        return collector.getViolations();
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public String toString() {
        return String.format(
                "BanDynamicVersions[allowSnapshots=%b, allowLatest=%b, allowRelease=%b, allowRanges=%b, allowRangesWithIdenticalBounds=%b, excludeOptionals=%b, excludedScopes=%s, ignores=%s, verbose=%b]",
                allowSnapshots,
                allowLatest,
                allowRelease,
                allowRanges,
                allowRangesWithIdenticalBounds,
                excludeOptionals,
                excludedScopes,
                ignores,
                verbose);
    }
}
