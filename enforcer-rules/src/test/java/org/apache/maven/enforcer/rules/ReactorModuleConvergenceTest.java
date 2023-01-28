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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Check reactorModuleConvergence rule.
 *
 * @author <a href="mailto:khmarbaise@apache.org">Karl Heinz Marbaise</a>
 */
class ReactorModuleConvergenceTest {
    private MavenSession session;

    private ReactorModuleConvergence rule;

    @BeforeEach
    public void before() throws ExpressionEvaluationException {
        session = mock(MavenSession.class);
        rule = new ReactorModuleConvergence(session);
        rule.setLog(mock(EnforcerLogger.class));
    }

    private void setupSortedProjects(List<MavenProject> projectList) {
        ProjectDependencyGraph pdg = mock(ProjectDependencyGraph.class);
        when(session.getProjectDependencyGraph()).thenReturn(pdg);
        when(pdg.getSortedProjects()).thenReturn(projectList);
    }

    @Test
    void shouldNotFailWithNoProject() throws EnforcerRuleException {
        setupSortedProjects(Collections.emptyList());

        rule.execute();
    }

    @Test
    void shouldNotFailWithAValidProject() throws EnforcerRuleException {
        MavenProject mp1 = createProjectParent();
        MavenProject mp2 = createProjectChild1(mp1);
        MavenProject mp3 = createProjectChild2(mp1);

        List<MavenProject> theList = Arrays.asList(mp1, mp2, mp3);
        setupSortedProjects(theList);

        rule.execute();
    }

    @Test
    void shouldFailWithWrongVersionInOneChild() {
        assertThrows(EnforcerRuleException.class, () -> {
            MavenProject mp1 = createProjectParent();
            MavenProject mp2 = createProjectChild1(mp1);
            MavenProject mp3 = createProjectChild2WithWrongVersion(mp1);

            List<MavenProject> theList = Arrays.asList(mp1, mp2, mp3);
            setupSortedProjects(theList);

            rule.execute();

            // intentionally no assertTrue() cause we expect getting an exception.
        });

        // intentionally no assertTrue() cause we expect getting an exception.
    }

    @Test
    void shouldFailWithWrongParent() {
        assertThrows(EnforcerRuleException.class, () -> {
            MavenProject mp1 = createProjectParent();

            MavenProject wrongParentVerison = mock(MavenProject.class);
            when(wrongParentVerison.getGroupId()).thenReturn("org.apache.enforcer");
            when(wrongParentVerison.getArtifactId()).thenReturn("m1");
            when(wrongParentVerison.getVersion()).thenReturn("1.1-SNAPSHOT");
            when(wrongParentVerison.getId()).thenReturn("org.apache.enforcer:m1:jar:1.1-SNAPSHOT");
            when(wrongParentVerison.getDependencies()).thenReturn(Collections.emptyList());

            MavenProject mp2 = createProjectChild2(wrongParentVerison);
            MavenProject mp3 = createProjectChild2(mp1);

            List<MavenProject> theList = Arrays.asList(mp1, mp2, mp3);
            setupSortedProjects(theList);

            rule.execute();

            // intentionally no assertTrue() cause we expect getting an exception.
        });

        // intentionally no assertTrue() cause we expect getting an exception.
    }

    @Test
    void shouldNotFailWithACompanyParent() throws EnforcerRuleException {
        MavenProject companyParent = createCompanyParent();
        MavenProject mp1 = createProjectParent(companyParent);

        MavenProject mp2 = createProjectChild1(mp1);
        MavenProject mp3 = createProjectChild2(mp1);

        List<MavenProject> theList = Arrays.asList(mp1, mp2, mp3);
        setupSortedProjects(theList);

        rule.execute();
    }

    @Test
    void shouldFailWithMissingParentsInReactory() {
        assertThrows(EnforcerRuleException.class, () -> {
            MavenProject mp1 = createProjectParent();
            MavenProject mp2 = createProjectChild1(mp1);
            MavenProject mp3 = createProjectChild2(null);

            List<MavenProject> theList = Arrays.asList(mp1, mp2, mp3);
            setupSortedProjects(theList);

            rule.execute();
        });
    }

    @Test
    void shouldFailWithAParentWhichIsNotPartOfTheReactory() {
        assertThrows(EnforcerRuleException.class, () -> {
            MavenProject mp1 = createProjectParent();

            MavenProject wrongParentVerison = mock(MavenProject.class);
            when(wrongParentVerison.getGroupId()).thenReturn("org.apache");
            when(wrongParentVerison.getArtifactId()).thenReturn("m1");
            when(wrongParentVerison.getVersion()).thenReturn("1.0-SNAPSHOT");
            when(wrongParentVerison.getId()).thenReturn("org.apache.enforcer:m1:jar:1.0-SNAPSHOT");
            when(wrongParentVerison.getDependencies()).thenReturn(Collections.emptyList());

            MavenProject mp2 = createProjectChild2(wrongParentVerison);
            MavenProject mp3 = createProjectChild2(mp1);

            List<MavenProject> theList = Arrays.asList(mp1, mp2, mp3);
            setupSortedProjects(theList);

            rule.execute();

            // intentionally no assertTrue() cause we expect getting an exception.
        });

        // intentionally no assertTrue() cause we expect getting an exception.
    }

    @Test
    void shouldNotFailWithDependencyInReactory() throws EnforcerRuleException {
        MavenProject mp1 = createProjectParent();
        MavenProject mp2 = createProjectChild1(mp1);

        Dependency goodDependency = createDependency("org.junit", "junit", "2.0");
        List<Dependency> depListMP2 = Arrays.asList(goodDependency);
        when(mp2.getDependencies()).thenReturn(depListMP2);

        MavenProject mp3 = createProjectChild2(mp1);
        Dependency dep1_MP3 = createDependency("org.apache.commons", "commons-io", "1.0.4");
        List<Dependency> depListMP3 = Arrays.asList(dep1_MP3);
        when(mp3.getDependencies()).thenReturn(depListMP3);

        List<MavenProject> theList = Arrays.asList(mp1, mp2, mp3);
        setupSortedProjects(theList);

        rule.execute();
    }

    @Test
    void shouldFailWithWrongDependencyInReactor() {
        assertThrows(EnforcerRuleException.class, () -> {
            MavenProject mp1 = createProjectParent();
            MavenProject mp2 = createProjectChild1(mp1);

            Dependency goodDependency = createDependency("org.junit", "junit", "2.0");

            Dependency wrongDepFromReactor = createDependency("org.apache.enforcer", "m2", "1.1-SNAPSHOT");
            List<Dependency> depList = Arrays.asList(goodDependency, wrongDepFromReactor);
            when(mp2.getDependencies()).thenReturn(depList);

            MavenProject mp3 = createProjectChild2(mp1);

            List<MavenProject> theList = Arrays.asList(mp1, mp2, mp3);
            setupSortedProjects(theList);

            rule.execute();

            // intentionally no assertTrue() cause we expect getting an exception.
        });

        // intentionally no assertTrue() cause we expect getting an exception.
    }

    @Test
    void cacheIdShouldBeSet() {
        assertThat(rule.getCacheId()).isNotEmpty();
    }

    /**
     * This small setup is equivalent to the following situation:
     *
     * <pre>
     *  &lt;parent&gt;
     *    &lt;groupId&gt;...&lt;/groupId&gt;
     *    &lt;artifactId&gt;...&lt;/artifactId&gt;
     *    &lt;version&gt;1.0-SNAPSHOT&lt;/version&gt;
     *  &lt;/parent&gt;
     *
     *  &lt;version&gt;1.1-SNAPSHOT&lt;/version&gt;
     * </pre>
     *
     * @param parent
     * @return Create MavenProject mock.
     */
    private MavenProject createProjectChild2WithWrongVersion(MavenProject parent) {
        MavenProject mp2 = mock(MavenProject.class);
        when(mp2.getParent()).thenReturn(parent);
        when(mp2.getGroupId()).thenReturn("org.apache.enforcer");
        when(mp2.getArtifactId()).thenReturn("m1");
        when(mp2.getVersion()).thenReturn("1.1-SNAPSHOT");
        when(mp2.getId()).thenReturn("org.apache.enforcer:m1:jar:1.1-SNAPSHOT");
        when(mp2.getDependencies()).thenReturn(Collections.emptyList());
        return mp2;
    }

    private MavenProject createProjectChild2(MavenProject parent) {
        MavenProject mp3 = mock(MavenProject.class);
        when(mp3.getParent()).thenReturn(parent);
        when(mp3.getGroupId()).thenReturn("org.apache.enforcer");
        when(mp3.getArtifactId()).thenReturn("m2");
        when(mp3.getVersion()).thenReturn("1.0-SNAPSHOT");
        when(mp3.getId()).thenReturn("org.apache.enforcer:m2:jar:1.0-SNAPSHOT");
        when(mp3.getDependencies()).thenReturn(Collections.emptyList());
        return mp3;
    }

    private MavenProject createProjectChild1(MavenProject parent) {
        MavenProject mp2 = mock(MavenProject.class);
        when(mp2.getParent()).thenReturn(parent);
        when(mp2.getGroupId()).thenReturn("org.apache.enforcer");
        when(mp2.getArtifactId()).thenReturn("m1");
        when(mp2.getVersion()).thenReturn("1.0-SNAPSHOT");
        when(mp2.getId()).thenReturn("org.apache.enforcer:m1:jar:1.0-SNAPSHOT");
        when(mp2.getDependencies()).thenReturn(Collections.emptyList());
        return mp2;
    }

    private MavenProject createCompanyParent() {
        MavenProject nonReactorParent = mock(MavenProject.class);
        when(nonReactorParent.getGroupId()).thenReturn("org.apache.enforcer.parent");
        when(nonReactorParent.getArtifactId()).thenReturn("parent");
        when(nonReactorParent.getVersion()).thenReturn("1.1");
        when(nonReactorParent.getId()).thenReturn("org.apache.enforcer.parent:parent:jar:1.1");
        when(nonReactorParent.getDependencies()).thenReturn(Collections.emptyList());
        return nonReactorParent;
    }

    private MavenProject createProjectParent(MavenProject nonReactorParent) {
        MavenProject m = createProjectParent();
        when(m.isExecutionRoot()).thenReturn(true);
        when(m.getParent()).thenReturn(nonReactorParent);
        return m;
    }

    private MavenProject createProjectParent() {
        MavenProject mp1 = mock(MavenProject.class);
        when(mp1.isExecutionRoot()).thenReturn(true);
        when(mp1.getParent()).thenReturn(null);
        when(mp1.getGroupId()).thenReturn("org.apache.enforcer");
        when(mp1.getArtifactId()).thenReturn("parent");
        when(mp1.getVersion()).thenReturn("1.0-SNAPSHOT");
        when(mp1.getId()).thenReturn("org.apache.enforcer:parent:pom:1.0-SNAPSHOT");
        when(mp1.getDependencies()).thenReturn(Collections.emptyList());
        return mp1;
    }

    private Dependency createDependency(String groupId, String artifactId, String version) {
        Dependency dep = mock(Dependency.class);
        when(dep.getGroupId()).thenReturn(groupId);
        when(dep.getArtifactId()).thenReturn(artifactId);
        when(dep.getVersion()).thenReturn(version);
        return dep;
    }
}
