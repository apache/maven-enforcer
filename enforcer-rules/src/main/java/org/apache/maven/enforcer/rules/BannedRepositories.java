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
package org.apache.maven.enforcer.rules;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

/**
 * This rule checks that this project's maven session whether have banned repositories.
 *
 * @author <a href="mailto:wangyf2010@gmail.com">Simon Wang</a>
 */
@Named("bannedRepositories")
public final class BannedRepositories extends AbstractStandardEnforcerRule {

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Specify explicitly banned non-plugin repositories. This is a list of repository url patterns. Support wildcard
     * "*".
     */
    private List<String> bannedRepositories = Collections.emptyList();

    /**
     * Specify explicitly banned plugin repositories. This is a list of repository url patterns. Support wildcard "*".
     */
    private List<String> bannedPluginRepositories = Collections.emptyList();

    /**
     * Specify explicitly allowed non-plugin repositories, then all others repositories would be banned. This is a list
     * of repository url patterns. Support wildcard "*".
     */
    private List<String> allowedRepositories = Collections.emptyList();

    /**
     * Specify explicitly allowed plugin repositories, then all others repositories would be banned. This is a list of
     * repository url patterns. Support wildcard "*".
     */
    private List<String> allowedPluginRepositories = Collections.emptyList();

    private final MavenProject project;

    @Inject
    public BannedRepositories(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public void execute() throws EnforcerRuleException {

        List<ArtifactRepository> resultBannedRepos = checkRepositories(
                project.getRemoteArtifactRepositories(), this.allowedRepositories, this.bannedRepositories);

        List<ArtifactRepository> resultBannedPluginRepos = checkRepositories(
                project.getPluginArtifactRepositories(), this.allowedPluginRepositories, this.bannedPluginRepositories);

        String repoErrMsg = populateErrorMessage(resultBannedRepos, " ");
        String pluginRepoErrMsg = populateErrorMessage(resultBannedPluginRepos, " plugin ");

        String errMsg = repoErrMsg + pluginRepoErrMsg;

        if (errMsg != null && !errMsg.isEmpty()) {
            throw new EnforcerRuleException(errMsg);
        }
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    protected void setBannedRepositories(List<String> bannedRepositories) {
        this.bannedRepositories = bannedRepositories;
    }

    protected void setAllowedRepositories(List<String> allowedRepositories) {
        this.allowedRepositories = allowedRepositories;
    }

    protected void setAllowedPluginRepositories(List<String> allowedPluginRepositories) {
        this.allowedPluginRepositories = allowedPluginRepositories;
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Check whether specified repositories have banned repositories.
     *
     * @param repositories: candidate repositories.
     * @param includes : 'include' patterns.
     * @param excludes : 'exclude' patterns.
     * @return Banned repositories.
     */
    private List<ArtifactRepository> checkRepositories(
            List<ArtifactRepository> repositories, List<String> includes, List<String> excludes) {

        getLog().debug(() -> String.format(
                "Check repositories: %s, for includes=%s and excludes=%s", repositories, includes, excludes));

        List<ArtifactRepository> bannedRepos = new ArrayList<>();

        for (ArtifactRepository repo : repositories) {
            String url = repo.getUrl().trim();
            if (includes.size() > 0 && !match(url, includes)) {
                bannedRepos.add(repo);
                continue;
            }

            if (excludes.size() > 0 && match(url, excludes)) {
                bannedRepos.add(repo);
            }
        }

        return bannedRepos;
    }

    private boolean match(String url, List<String> patterns) {
        for (String pattern : patterns) {
            if (this.match(url, pattern)) {
                return true;
            }
        }

        return false;
    }

    private boolean match(String text, String pattern) {
        return text.matches(pattern.replace("?", ".?").replace("*", ".*?"));
    }

    private String populateErrorMessage(List<ArtifactRepository> resultBannedRepos, String errorMessagePrefix) {
        StringBuffer errMsg = new StringBuffer("");
        if (!resultBannedRepos.isEmpty()) {
            errMsg.append("Current maven session contains banned" + errorMessagePrefix
                    + "repository urls, please double check your pom or settings.xml:" + System.lineSeparator()
                    + getRepositoryUrlString(resultBannedRepos) + System.lineSeparator() + System.lineSeparator());
        }

        return errMsg.toString();
    }

    private String getRepositoryUrlString(List<ArtifactRepository> resultBannedRepos) {
        StringBuilder urls = new StringBuilder("");
        for (ArtifactRepository repo : resultBannedRepos) {
            urls.append(repo.getId() + " - " + repo.getUrl() + System.lineSeparator());
        }
        return urls.toString();
    }

    @Override
    public String toString() {
        return String.format(
                "BannedRepositories[bannedRepositories=%s, bannedPluginRepositories=%s, allowedRepositories=%s, allowedPluginRepositories=%s",
                bannedRepositories, bannedPluginRepositories, allowedRepositories, allowedPluginRepositories);
    }
}
