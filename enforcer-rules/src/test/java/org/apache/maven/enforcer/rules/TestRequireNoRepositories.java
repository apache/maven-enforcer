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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the "require no repositories" rule.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:khmarbaise@apache.org">Karl Heinz Marbaise</a>
 */
class TestRequireNoRepositories {

    private RequireNoRepositories rule;

    private MavenSession session;

    @BeforeEach
    void before() {
        session = mock(MavenSession.class);

        rule = new RequireNoRepositories(session);
        rule.setMessage("my message");
        rule.setLog(mock(EnforcerLogger.class));
    }

    private MavenProject createMavenProject() {
        MavenProject mp = mock(MavenProject.class);
        when(mp.getGroupId()).thenReturn("org.apache.maven.plugins.enforcer.test");
        when(mp.getArtifactId()).thenReturn("no-repositories-child");
        when(mp.getVersion()).thenReturn("1.0-SNAPSHOT");

        return mp;
    }

    private Model createOriginalModel() {
        Model m = mock(Model.class);
        when(m.getGroupId()).thenReturn("org.apache.maven.plugins.enforcer.test");
        when(m.getArtifactId()).thenReturn("no-repositories");
        when(m.getVersion()).thenReturn("1.0-SNAPSHOT");
        return m;
    }

    private MavenProject createStandAloneProject() {
        MavenProject mp = createMavenProject();
        Model originalModel = createOriginalModel();
        // This means the interpolated model is the same
        // as the non interpolated.
        when(mp.getModel()).thenReturn(originalModel);
        when(mp.getOriginalModel()).thenReturn(originalModel);
        return mp;
    }

    private void setupSortedProjects(List<MavenProject> projectList) {
        ProjectDependencyGraph pdg = mock(ProjectDependencyGraph.class);
        when(session.getProjectDependencyGraph()).thenReturn(pdg);
        when(pdg.getSortedProjects()).thenReturn(projectList);
        when(session.getSettings()).thenReturn(mock(Settings.class));
    }

    private Repository createRepository(String id, String url) {
        Repository r = new Repository();
        r.setId(id);
        r.setUrl(url);
        RepositoryPolicy snapshotPolicy = new RepositoryPolicy();
        snapshotPolicy.setEnabled(false);
        snapshotPolicy.setUpdatePolicy("daily");
        r.setSnapshots(snapshotPolicy);

        RepositoryPolicy releasePolicy = new RepositoryPolicy();
        releasePolicy.setEnabled(true);
        releasePolicy.setUpdatePolicy("never");
        r.setReleases(releasePolicy);

        return r;
    }

    private Repository createSnapshotRepository(String id, String url) {
        Repository r = new Repository();
        r.setId(id);
        r.setUrl(url);

        RepositoryPolicy snapshotPolicy = new RepositoryPolicy();
        snapshotPolicy.setEnabled(true);
        snapshotPolicy.setUpdatePolicy("daily");
        r.setSnapshots(snapshotPolicy);

        RepositoryPolicy releasePolicy = new RepositoryPolicy();
        releasePolicy.setEnabled(false);
        r.setReleases(releasePolicy);

        return r;
    }

    private MavenProject addRepository(MavenProject project, Repository r) {
        Model originalModel = project.getOriginalModel();
        List<Repository> repositories = new ArrayList<>();
        repositories.add(r);
        when(originalModel.getRepositories()).thenReturn(repositories);
        return project;
    }

    private MavenProject addEmptyRepository(MavenProject project) {
        Model originalModel = project.getOriginalModel();
        List<Repository> repositories = new ArrayList<>();
        when(originalModel.getRepositories()).thenReturn(repositories);
        return project;
    }

    private MavenProject addPluginRepository(MavenProject project, Repository r) {
        Model originalModel = project.getOriginalModel();
        List<Repository> repositories = new ArrayList<>();
        repositories.add(r);
        when(originalModel.getPluginRepositories()).thenReturn(repositories);
        return project;
    }

    private MavenProject addEmptyPluginRepository(MavenProject project) {
        Model originalModel = project.getOriginalModel();
        List<Repository> repositories = new ArrayList<>();
        when(originalModel.getPluginRepositories()).thenReturn(repositories);
        return project;
    }

    /**
     * This model contains a single module maven project without any repository.
     */
    @Test
    void testAllBannedNoRepositories() throws EnforcerRuleException {
        MavenProject baseProject = createStandAloneProject();
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    /**
     * The model contains a single repository which is is not allowed by the default rules.
     */
    @Test
    void testAllBannedWithRepository() {
        assertThrows(EnforcerRuleException.class, () -> {
            MavenProject baseProject = createStandAloneProject();
            addRepository(baseProject, createRepository("repo", "http://example.com/repo"));
            setupSortedProjects(Collections.singletonList(baseProject));

            rule.execute();
        });
    }

    /**
     * The model contains a single plugin repository which is is not allowed by the default rules.
     */
    @Test
    void testAllBannedWithPluginRepository() {
        assertThrows(EnforcerRuleException.class, () -> {
            MavenProject baseProject = createStandAloneProject();
            addPluginRepository(baseProject, createRepository("repo", "http://example.com/repo"));
            setupSortedProjects(Collections.singletonList(baseProject));

            rule.execute();
        });
    }

    /**
     * The model contains a single repository which is allowed by setting allowedRepositories to the id.
     */
    @Test
    void testAllBannedWithAllowedRepositories() throws EnforcerRuleException {
        final String repositoryId = "repo";
        rule.setAllowedRepositories(Collections.singletonList(repositoryId));

        MavenProject baseProject = createStandAloneProject();
        addRepository(baseProject, createRepository(repositoryId, "http://example.com/repo"));
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    /**
     * The model contains a single repository. Turned off ban repositories.
     */
    @Test
    void testRepositoriesNotBannedWithSingleRepository() throws EnforcerRuleException {
        final String repositoryId = "repo";

        rule.setBanRepositories(false);

        MavenProject baseProject = createStandAloneProject();
        addRepository(baseProject, createRepository(repositoryId, "http://example.com/repo"));
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    /**
     * The model contains no repository at all. Turned off ban repositories.
     */
    @Test
    void testRepositoriesNotBannedWithOutAnyRepository() throws EnforcerRuleException {
        rule.setBanRepositories(false);

        MavenProject baseProject = createStandAloneProject();
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    /**
     * This model contains a single plugin repository. The given plugin repository is added to the list of allowed
     * plugin repositories.
     */
    @Test
    void testAllBannedWithAllowedPluginRepositories() throws EnforcerRuleException {
        final String repositoryId = "repo";
        rule.setAllowedPluginRepositories(Collections.singletonList(repositoryId));

        MavenProject baseProject = createStandAloneProject();
        addPluginRepository(baseProject, createRepository(repositoryId, "http://example.com/repo"));
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    /**
     * The model contains a single plugin repository. Turned off ban plugin repositories.
     */
    @Test
    void testPluginRepositoriesNotBannedWithSinglePluginRepository() throws EnforcerRuleException {
        final String repositoryId = "repo";

        rule.setBanPluginRepositories(false);

        MavenProject baseProject = createStandAloneProject();
        addPluginRepository(baseProject, createRepository(repositoryId, "http://example.com/repo"));
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    /**
     * The model contains no repository at all. Turned off ban plugin repositories.
     */
    @Test
    void testPluginRepositoriesNotBannedWithOutAnyRepository() throws EnforcerRuleException {
        rule.setBanPluginRepositories(false);

        MavenProject baseProject = createStandAloneProject();
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    @Test
    void testAllBannedWithSnapshotRepository() {
        assertThrows(EnforcerRuleException.class, () -> {
            MavenProject baseProject = createStandAloneProject();
            addRepository(baseProject, createSnapshotRepository("repo", "http://example.com/repo"));
            setupSortedProjects(Collections.singletonList(baseProject));

            rule.execute();
        });
    }

    @Test
    void testAllBannedWithSnapshotRepositoryAllowedRepositories() throws EnforcerRuleException {
        final String repositoryId = "repo";
        rule.setAllowedRepositories(Collections.singletonList(repositoryId));

        MavenProject baseProject = createStandAloneProject();
        addRepository(baseProject, createSnapshotRepository(repositoryId, "http://example.com/repo"));
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    @Test
    void testAllBannedWithSnapshotRepositoryAndSetAllowSnapshotRepositories() throws EnforcerRuleException {
        final String repositoryId = "repo";
        rule.setAllowSnapshotRepositories(true);

        MavenProject baseProject = createStandAloneProject();
        addRepository(baseProject, createSnapshotRepository(repositoryId, "http://example.com/repo"));
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    @Test
    void testAllBannedWithSnapshotPluginRepositoryAndSetAllowSnapshotPluginRepositories() throws EnforcerRuleException {
        final String repositoryId = "repo";
        rule.setAllowSnapshotPluginRepositories(true);

        MavenProject baseProject = createStandAloneProject();
        addPluginRepository(baseProject, createSnapshotRepository(repositoryId, "http://example.com/repo"));
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    @Test
    void testAllBannedWithEmptyRepository() throws EnforcerRuleException {
        MavenProject baseProject = createStandAloneProject();
        addEmptyRepository(baseProject);
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }

    @Test
    void testAllBannedWithEmptyPluginRepository() throws EnforcerRuleException {
        MavenProject baseProject = createStandAloneProject();
        addEmptyPluginRepository(baseProject);
        setupSortedProjects(Collections.singletonList(baseProject));

        rule.execute();
    }
}
