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

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * Abstract Rule for banning dependencies.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public abstract class AbstractBanDependencies extends AbstractNonCacheableEnforcerRule {

    /** Specify if transitive dependencies should be searched (default) or only look at direct dependencies. */
    private boolean searchTransitive = true;

    @Override
    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        MavenSession session;
        try {
            session = (MavenSession) helper.evaluate("${session}");
        } catch (ExpressionEvaluationException e) {
            throw new EnforcerRuleException("Cannot resolve MavenSession", e);
        }

        // get the correct list of dependencies
        Set<Artifact> dependencyArtifacts = searchTransitive
                ? ArtifactUtils.getDependencyArtifacts(ArtifactUtils.resolveTransitiveDependencies(helper))
                : session.getCurrentProject().getDependencyArtifacts();

        // look for banned dependencies
        Set<Artifact> bannedDependencies = checkDependencies(dependencyArtifacts, helper.getLog());

        // if any are found, fail the check but list all of them
        if (bannedDependencies != null && !bannedDependencies.isEmpty()) {
            String message = getMessage();

            StringBuilder buf = new StringBuilder();
            if (message != null) {
                buf.append(message).append(System.lineSeparator());
            }
            for (Artifact artifact : bannedDependencies) {
                buf.append(getErrorMessage(artifact));
            }
            message = buf.append("Use 'mvn dependency:tree' to locate the source of the banned dependencies.")
                    .toString();

            throw new EnforcerRuleException(message);
        }
    }

    protected CharSequence getErrorMessage(Artifact artifact) {
        return "Found Banned Dependency: " + artifact.getId() + System.lineSeparator();
    }

    /**
     * Checks the set of dependencies against the list of excludes.
     *
     * @param dependencies dependencies to be checked against the list of excludes
     * @param log          the log
     * @return the sets the
     * @throws EnforcerRuleException the enforcer rule exception
     */
    protected abstract Set<Artifact> checkDependencies(Set<Artifact> dependencies, Log log)
            throws EnforcerRuleException;

    /**
     * Checks if is search transitive.
     *
     * @return the searchTransitive
     */
    public boolean isSearchTransitive() {
        return this.searchTransitive;
    }

    /**
     * Sets the search transitive.
     *
     * @param theSearchTransitive the searchTransitive to set
     */
    public void setSearchTransitive(boolean theSearchTransitive) {
        this.searchTransitive = theSearchTransitive;
    }
}
