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
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the "banned repositories" rule.
 *
 * @author <a href="mailto:wangyf2010@gmail.com">Simon Wang</a>
 */
@ExtendWith(MockitoExtension.class)
class TestBannedRepositories {

    @Mock
    private MavenProject project;

    @InjectMocks
    private BannedRepositories rule;

    @BeforeEach
    public void setUp() {
        rule.setLog(mock(EnforcerLogger.class));
    }

    @Test
    void testNoCheckRules() throws EnforcerRuleException {
        ArtifactRepository repo1 = new MavenArtifactRepository("repo1", "http://repo1/", null, null, null);
        List<ArtifactRepository> repos = new ArrayList<>();
        repos.add(repo1);

        when(project.getRemoteArtifactRepositories()).thenReturn(repos);
        when(project.getPluginArtifactRepositories()).thenReturn(repos);

        rule.execute();
    }

    @Test
    void testBannedRepositories() {
        ArtifactRepository repo1 = new MavenArtifactRepository("repo1", "http://repo1/", null, null, null);
        ArtifactRepository repo2 = new MavenArtifactRepository("repo1", "http://repo1/test", null, null, null);
        ArtifactRepository repo3 = new MavenArtifactRepository("repo1", "http://repo2/test", null, null, null);
        List<ArtifactRepository> repos = new ArrayList<>();
        repos.add(repo1);
        repos.add(repo2);
        repos.add(repo3);

        when(project.getRemoteArtifactRepositories()).thenReturn(repos);
        when(project.getPluginArtifactRepositories()).thenReturn(repos);

        List<String> bannedRepositories = new ArrayList<>();
        String pattern1 = "http://repo1/*";

        bannedRepositories.add(pattern1);

        rule.setBannedRepositories(bannedRepositories);

        try {
            rule.execute();
            fail("should throw exception");
        } catch (EnforcerRuleException e) {
        }
    }

    @Test
    void testAllowedRepositoriesAllOK() throws EnforcerRuleException {
        ArtifactRepository repo1 = new MavenArtifactRepository("repo1", "http://repo1/", null, null, null);
        ArtifactRepository repo2 = new MavenArtifactRepository("repo1", "http://repo1/test", null, null, null);

        List<ArtifactRepository> repos = new ArrayList<>();
        repos.add(repo1);
        repos.add(repo2);

        when(project.getRemoteArtifactRepositories()).thenReturn(repos);
        when(project.getPluginArtifactRepositories()).thenReturn(repos);

        List<String> bannedRepositories = new ArrayList<>();
        String pattern1 = "http://repo1/*";

        bannedRepositories.add(pattern1);

        rule.setAllowedRepositories(bannedRepositories);
        rule.setAllowedPluginRepositories(bannedRepositories);

        rule.execute();
    }

    @Test
    void testAllowedRepositoriesException() {
        ArtifactRepository repo1 = new MavenArtifactRepository("repo1", "http://repo1/", null, null, null);
        ArtifactRepository repo2 = new MavenArtifactRepository("repo1", "http://repo1/test", null, null, null);
        ArtifactRepository repo3 = new MavenArtifactRepository("repo1", "http://repo2/test", null, null, null);
        List<ArtifactRepository> repos = new ArrayList<>();
        repos.add(repo1);
        repos.add(repo2);
        repos.add(repo3);

        when(project.getRemoteArtifactRepositories()).thenReturn(repos);
        when(project.getPluginArtifactRepositories()).thenReturn(repos);

        List<String> patterns = new ArrayList<>();
        String pattern1 = "http://repo1/*";

        patterns.add(pattern1);

        rule.setAllowedPluginRepositories(patterns);
        rule.setAllowedRepositories(patterns);

        try {
            rule.execute();
            fail("should throw exception");
        } catch (EnforcerRuleException e) {
        }
    }
}
