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

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugins.enforcer.utils.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

import static java.util.Optional.ofNullable;
import static org.apache.maven.plugins.enforcer.utils.ArtifactUtils.matchDependencyArtifact;

/**
 * This rule checks that no snapshots are included.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class RequireReleaseDeps extends BannedDependenciesBase {

    /**
     * Allows this rule to execute only when this project is a release.
     *
     * @see {@link #setOnlyWhenRelease(boolean)}
     * @see {@link #isOnlyWhenRelease()}
     *
     */
    private boolean onlyWhenRelease = false;

    /**
     * Allows this rule to fail when the parent is defined as a snapshot.
     *
     * @see {@link #setFailWhenParentIsSnapshot(boolean)}
     * @see {@link #isFailWhenParentIsSnapshot()}
     */
    private boolean failWhenParentIsSnapshot = true;

    // Override parent to allow optional ignore of this rule.
    @Override
    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        boolean callSuper;
        MavenProject project = null;
        if (onlyWhenRelease) {
            // get the project
            project = getProject(helper);

            // only call super if this project is a release
            callSuper = !project.getArtifact().isSnapshot();
        } else {
            callSuper = true;
        }

        if (callSuper) {
            super.execute(helper);
            if (failWhenParentIsSnapshot) {
                if (project == null) {
                    project = getProject(helper);
                }

                Artifact parentArtifact = project.getParentArtifact();

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

    /**
     * @param helper
     * @return The evaluated {@link MavenProject}.
     * @throws EnforcerRuleException
     */
    private MavenProject getProject(EnforcerRuleHelper helper) throws EnforcerRuleException {
        try {
            return (MavenProject) helper.evaluate("${project}");
        } catch (ExpressionEvaluationException eee) {
            throw new EnforcerRuleException("Unable to retrieve the MavenProject: ", eee);
        }
    }

    @Override
    protected boolean validate(Artifact artifact) {
        // only check isSnapshot() if the artifact does not match (excludes minus includes)
        // otherwise true
        return (matchDependencyArtifact(artifact, excludes) && !matchDependencyArtifact(artifact, includes))
                || !artifact.isSnapshot();
    }

    /*
     * Filter the dependency artifacts according to the includes and excludes
     * If includes and excludes are both null, the original set is returned.
     *
     * @param dependencies the list of dependencies to filter
     * @return the resulting set of dependencies
     */
    protected Set<Artifact> filterArtifacts(Set<Artifact> dependencies) throws EnforcerRuleException {
        if (includes != null) {
            dependencies = ArtifactUtils.filterDependencyArtifacts(dependencies, includes);
        }

        if (dependencies != null && excludes != null) {
            ofNullable(ArtifactUtils.filterDependencyArtifacts(dependencies, excludes))
                    .ifPresent(dependencies::removeAll);
        }

        return dependencies;
    }

    public final boolean isOnlyWhenRelease() {
        return onlyWhenRelease;
    }

    public final void setOnlyWhenRelease(boolean onlyWhenRelease) {
        this.onlyWhenRelease = onlyWhenRelease;
    }

    public final boolean isFailWhenParentIsSnapshot() {
        return failWhenParentIsSnapshot;
    }

    public final void setFailWhenParentIsSnapshot(boolean failWhenParentIsSnapshot) {
        this.failWhenParentIsSnapshot = failWhenParentIsSnapshot;
    }
}
