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

import java.io.IOException;
import java.util.HashSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The Class TestRequireSameVersions.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
class TestRequireSameVersions {
    private static final ArtifactStubFactory ARTIFACT_FACTORY = new ArtifactStubFactory();

    private MavenProject project;
    private RequireSameVersions rule;

    @BeforeEach
    void setup() {
        project = mock(MavenProject.class);
        rule = new RequireSameVersions(project, mock(MavenSession.class));
    }

    @Test
    void projectWithSameVersionsInBuildAndReport() throws Exception {
        String version = "1.0.0";
        Artifact dependency = constructArtifact("acme-dependency", version);
        Artifact buildPluginOne = constructArtifact("acme-build-plugin-one", version);
        Artifact buildPluginTwo = constructArtifact("acme-build-plugin-two", version);
        Artifact reportPluginOne = constructArtifact("acme-report-plugin-one", version);
        Artifact reportPluginTwo = constructArtifact("acme-report-plugin-two", version);
        rule.addDependency(extractGaString(dependency));
        rule.addBuildPlugin(extractGaString(buildPluginOne));
        rule.addBuildPlugin(extractGaString(buildPluginTwo));
        rule.addReportPlugin(extractGaString(reportPluginOne));
        rule.addReportPlugin(extractGaString(reportPluginTwo));

        HashSet<Artifact> dependencies = new HashSet<>();
        dependencies.add(dependency);
        HashSet<Artifact> pluginArtifacts = new HashSet<>();
        pluginArtifacts.add(buildPluginOne);
        pluginArtifacts.add(buildPluginTwo);
        HashSet<Artifact> reportArtifacts = new HashSet<>();
        reportArtifacts.add(reportPluginOne);
        reportArtifacts.add(reportPluginTwo);
        when(project.getArtifacts()).thenReturn(dependencies);
        when(project.getPluginArtifacts()).thenReturn(pluginArtifacts);
        when(project.getReportArtifacts()).thenReturn(reportArtifacts);

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void projectWithSameVersionsInPlugins() throws Exception {
        String version = "1.0.0";
        Artifact dependency = constructArtifact("acme-dependency", version);
        Artifact buildPluginOne = constructArtifact("acme-build-plugin-one", version);
        Artifact buildPluginTwo = constructArtifact("acme-build-plugin-two", version);
        Artifact reportPluginOne = constructArtifact("acme-report-plugin-one", version);
        Artifact reportPluginTwo = constructArtifact("acme-report-plugin-two", version);
        rule.addDependency(extractGaString(dependency));
        rule.addPlugin(extractGaString(buildPluginOne));
        rule.addPlugin(extractGaString(buildPluginTwo));
        rule.addPlugin(extractGaString(reportPluginOne));
        rule.addPlugin(extractGaString(reportPluginTwo));

        HashSet<Artifact> dependencies = new HashSet<>();
        dependencies.add(dependency);
        HashSet<Artifact> pluginArtifacts = new HashSet<>();
        pluginArtifacts.add(buildPluginOne);
        pluginArtifacts.add(buildPluginTwo);
        HashSet<Artifact> reportArtifacts = new HashSet<>();
        reportArtifacts.add(reportPluginOne);
        reportArtifacts.add(reportPluginTwo);
        when(project.getArtifacts()).thenReturn(dependencies);
        when(project.getPluginArtifacts()).thenReturn(pluginArtifacts);
        when(project.getReportArtifacts()).thenReturn(reportArtifacts);

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void projectWithSameVersionsInBuildAndReportAndPlugins() throws Exception {
        String version = "1.0.0";
        Artifact dependency = constructArtifact("acme-dependency", version);
        Artifact buildPluginOne = constructArtifact("acme-build-plugin-one", version);
        Artifact buildPluginTwo = constructArtifact("acme-build-plugin-two", version);
        Artifact reportPluginOne = constructArtifact("acme-report-plugin-one", version);
        Artifact reportPluginTwo = constructArtifact("acme-report-plugin-two", version);
        rule.addDependency(extractGaString(dependency));
        rule.addBuildPlugin(extractGaString(buildPluginOne));
        rule.addPlugin(extractGaString(buildPluginTwo));
        rule.addReportPlugin(extractGaString(reportPluginOne));
        rule.addPlugin(extractGaString(reportPluginTwo));

        HashSet<Artifact> dependencies = new HashSet<>();
        dependencies.add(dependency);
        HashSet<Artifact> pluginArtifacts = new HashSet<>();
        pluginArtifacts.add(buildPluginOne);
        pluginArtifacts.add(buildPluginTwo);
        HashSet<Artifact> reportArtifacts = new HashSet<>();
        reportArtifacts.add(reportPluginOne);
        reportArtifacts.add(reportPluginTwo);
        when(project.getArtifacts()).thenReturn(dependencies);
        when(project.getPluginArtifacts()).thenReturn(pluginArtifacts);
        when(project.getReportArtifacts()).thenReturn(reportArtifacts);

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void projectWithDifferentPluginVersionsInBuildAndReport() throws Exception {
        String version = "1.0.0";
        Artifact dependency = constructArtifact("acme-dependency", version);
        Artifact buildPluginOne = constructArtifact("acme-build-plugin-one", version);
        Artifact buildPluginTwo = constructArtifact("acme-build-plugin-two", "1.0.1");
        Artifact reportPluginOne = constructArtifact("acme-report-plugin-one", version);
        Artifact reportPluginTwo = constructArtifact("acme-report-plugin-two", version);
        rule.addDependency(extractGaString(dependency));
        rule.addBuildPlugin(extractGaString(buildPluginOne));
        rule.addBuildPlugin(extractGaString(buildPluginTwo));
        rule.addReportPlugin(extractGaString(reportPluginOne));
        rule.addReportPlugin(extractGaString(reportPluginTwo));

        HashSet<Artifact> dependencies = new HashSet<>();
        dependencies.add(dependency);
        HashSet<Artifact> pluginArtifacts = new HashSet<>();
        pluginArtifacts.add(buildPluginOne);
        pluginArtifacts.add(buildPluginTwo);
        HashSet<Artifact> reportArtifacts = new HashSet<>();
        reportArtifacts.add(reportPluginOne);
        reportArtifacts.add(reportPluginTwo);
        when(project.getArtifacts()).thenReturn(dependencies);
        when(project.getPluginArtifacts()).thenReturn(pluginArtifacts);
        when(project.getReportArtifacts()).thenReturn(reportArtifacts);

        assertThatCode(rule::execute).isInstanceOf(EnforcerRuleException.class);
    }

    @Test
    void projectWithDifferentPluginVersionsInPlugins() throws Exception {
        String version = "1.0.0";
        Artifact dependency = constructArtifact("acme-dependency", version);
        Artifact buildPluginOne = constructArtifact("acme-build-plugin-one", version);
        Artifact buildPluginTwo = constructArtifact("acme-build-plugin-two", "1.0.1");
        Artifact reportPluginOne = constructArtifact("acme-report-plugin-one", version);
        Artifact reportPluginTwo = constructArtifact("acme-report-plugin-two", version);
        rule.addDependency(extractGaString(dependency));
        rule.addPlugin(extractGaString(buildPluginOne));
        rule.addPlugin(extractGaString(buildPluginTwo));
        rule.addPlugin(extractGaString(reportPluginOne));
        rule.addPlugin(extractGaString(reportPluginTwo));

        HashSet<Artifact> dependencies = new HashSet<>();
        dependencies.add(dependency);
        HashSet<Artifact> pluginArtifacts = new HashSet<>();
        pluginArtifacts.add(buildPluginOne);
        pluginArtifacts.add(buildPluginTwo);
        HashSet<Artifact> reportArtifacts = new HashSet<>();
        reportArtifacts.add(reportPluginOne);
        reportArtifacts.add(reportPluginTwo);
        when(project.getArtifacts()).thenReturn(dependencies);
        when(project.getPluginArtifacts()).thenReturn(pluginArtifacts);
        when(project.getReportArtifacts()).thenReturn(reportArtifacts);

        assertThatCode(rule::execute).isInstanceOf(EnforcerRuleException.class);
    }

    @Test
    void projectWithDifferentPluginVersionsInBuildAndReportAndPlugins() throws Exception {
        String version = "1.0.0";
        Artifact dependency = constructArtifact("acme-dependency", version);
        Artifact buildPluginOne = constructArtifact("acme-build-plugin-one", version);
        Artifact buildPluginTwo = constructArtifact("acme-build-plugin-two", "1.0.1");
        Artifact reportPluginOne = constructArtifact("acme-report-plugin-one", version);
        Artifact reportPluginTwo = constructArtifact("acme-report-plugin-two", version);
        rule.addDependency(extractGaString(dependency));
        rule.addBuildPlugin(extractGaString(buildPluginOne));
        rule.addPlugin(extractGaString(buildPluginTwo));
        rule.addReportPlugin(extractGaString(reportPluginOne));
        rule.addPlugin(extractGaString(reportPluginTwo));

        HashSet<Artifact> dependencies = new HashSet<>();
        dependencies.add(dependency);
        HashSet<Artifact> pluginArtifacts = new HashSet<>();
        pluginArtifacts.add(buildPluginOne);
        pluginArtifacts.add(buildPluginTwo);
        HashSet<Artifact> reportArtifacts = new HashSet<>();
        reportArtifacts.add(reportPluginOne);
        reportArtifacts.add(reportPluginTwo);
        when(project.getArtifacts()).thenReturn(dependencies);
        when(project.getPluginArtifacts()).thenReturn(pluginArtifacts);
        when(project.getReportArtifacts()).thenReturn(reportArtifacts);

        assertThatCode(rule::execute).isInstanceOf(EnforcerRuleException.class);
    }

    @Test
    void projectWithDifferentDependencyVersionsInBuildAndReportAndPlugins() throws Exception {
        String version = "1.0.0";
        Artifact dependency = constructArtifact("acme-dependency", "1.0.1");
        Artifact buildPluginOne = constructArtifact("acme-build-plugin-one", version);
        Artifact buildPluginTwo = constructArtifact("acme-build-plugin-two", version);
        Artifact reportPluginOne = constructArtifact("acme-report-plugin-one", version);
        Artifact reportPluginTwo = constructArtifact("acme-report-plugin-two", version);
        rule.addDependency(extractGaString(dependency));
        rule.addBuildPlugin(extractGaString(buildPluginOne));
        rule.addPlugin(extractGaString(buildPluginTwo));
        rule.addReportPlugin(extractGaString(reportPluginOne));
        rule.addPlugin(extractGaString(reportPluginTwo));

        HashSet<Artifact> dependencies = new HashSet<>();
        dependencies.add(dependency);
        HashSet<Artifact> pluginArtifacts = new HashSet<>();
        pluginArtifacts.add(buildPluginOne);
        pluginArtifacts.add(buildPluginTwo);
        HashSet<Artifact> reportArtifacts = new HashSet<>();
        reportArtifacts.add(reportPluginOne);
        reportArtifacts.add(reportPluginTwo);
        when(project.getArtifacts()).thenReturn(dependencies);
        when(project.getPluginArtifacts()).thenReturn(pluginArtifacts);
        when(project.getReportArtifacts()).thenReturn(reportArtifacts);

        assertThatCode(rule::execute).isInstanceOf(EnforcerRuleException.class);
    }

    private static Artifact constructArtifact(String artifactId, String version) throws IOException {
        return ARTIFACT_FACTORY.createArtifact("org.acme", artifactId, version);
    }

    private static String extractGaString(Artifact dependency) {
        return String.format("%s:%s", dependency.getGroupId(), dependency.getArtifactId());
    }
}
