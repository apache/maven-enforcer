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

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.apache.maven.execution.MavenSession;

import static java.util.Optional.ofNullable;
import static org.apache.maven.enforcer.rules.utils.ArtifactUtils.matchDependencyArtifact;

/**
 * This rule checks that no snapshots are included.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@Named("requireReleaseDeps")
public final class RequireReleaseDeps extends BannedDependenciesBase {

    /**
     * Allows this rule to execute only when this project is a release.
     */
    private boolean onlyWhenRelease = false;

    /**
     * Allows this rule to fail when the parent is defined as a snapshot.
     */
    private boolean failWhenParentIsSnapshot = true;

    @Inject
    public RequireReleaseDeps(MavenSession session, ResolverUtil resolverUtil) {
        super(session, resolverUtil);
    }

    // Override parent to allow optional ignore of this rule.
    @Override
    public void execute() throws EnforcerRuleException {
        boolean callSuper;
        if (onlyWhenRelease) {
            // only call super if this project is a release
            callSuper = !getSession().getCurrentProject().getArtifact().isSnapshot();
        } else {
            callSuper = true;
        }

        if (callSuper) {
            super.execute();
            if (failWhenParentIsSnapshot) {

                Artifact parentArtifact = getSession().getCurrentProject().getParentArtifact();

                if (parentArtifact != null) {
                    Set<Artifact> singletonArtifact = new HashSet<>();
                    singletonArtifact.add(parentArtifact);
                    Set<Artifact> artifacts = filterArtifacts(singletonArtifact);
                    parentArtifact = ofNullable(artifacts)
                            .flatMap(s -> s.stream().findFirst())
                            .orElse(null);
                }

                if (parentArtifact != null && parentArtifact.isSnapshot()) {
                    throw new EnforcerRuleException("Parent Cannot be a snapshot: " + parentArtifact.getId());
                }
            }
        }
    }

    @Override
    protected String getErrorMessage() {
        return "is not a release dependency";
    }

    @Override
    protected boolean validate(Artifact artifact) {
        // only check isSnapshot() if the artifact does not match (excludes minus includes)
        // otherwise true
        return (matchDependencyArtifact(artifact, getExcludes()) && !matchDependencyArtifact(artifact, getIncludes()))
                || !artifact.isSnapshot();
    }

    /**
     * Filter the dependency artifacts according to the includes and excludes
     * If includes and excludes are both null, the original set is returned.
     *
     * @param dependencies the list of dependencies to filter
     * @return the resulting set of dependencies
     */
    private Set<Artifact> filterArtifacts(Set<Artifact> dependencies) throws EnforcerRuleException {
        if (getIncludes() != null) {
            dependencies = ArtifactUtils.filterDependencyArtifacts(dependencies, getIncludes());
        }

        if (dependencies != null && getExcludes() != null) {
            ofNullable(ArtifactUtils.filterDependencyArtifacts(dependencies, getExcludes()))
                    .ifPresent(dependencies::removeAll);
        }

        return dependencies;
    }

    public void setOnlyWhenRelease(boolean onlyWhenRelease) {
        this.onlyWhenRelease = onlyWhenRelease;
    }

    public void setFailWhenParentIsSnapshot(boolean failWhenParentIsSnapshot) {
        this.failWhenParentIsSnapshot = failWhenParentIsSnapshot;
    }

    @Override
    public String toString() {
        return String.format(
                "RequireReleaseDeps[message=%s, excludes=%s, includes=%s, searchTransitive=%b, onlyWhenRelease=%b, failWhenParentIsSnapshot=%b]",
                getMessage(),
                getExcludes(),
                getIncludes(),
                isSearchTransitive(),
                onlyWhenRelease,
                failWhenParentIsSnapshot);
    }
}
