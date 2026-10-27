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
import java.util.Collections;
import java.util.HashSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.EnforcerArtifactStubFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The Class TestRequireSameVersions.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
class TestRequireSameVersions {
    private static final EnforcerArtifactStubFactory ARTIFACT_FACTORY = new EnforcerArtifactStubFactory();

    private MavenProject project;
    private RequireSameVersions rule;

    @BeforeEach
    void setup() {
        project = mock(MavenProject.class);
        rule = new RequireSameVersions(project, mock(MavenSession.class));
    }

    @Test
    void testProjectWithSameVersionsInBuildAndReport() throws IOException {
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
    void testProjectWithSameVersionsInPlugins() throws IOException {
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
    void testProjectWithSameVersionsInBuildAndReportAndPlugins() throws IOException {
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
    void testProjectWithDifferentPluginVersionsInBuildAndReport() throws IOException {
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
    void testProjectWithDifferentPluginVersionsInPlugins() throws IOException {
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
    void testProjectWithDifferentPluginVersionsInBuildAndReportAndPlugins() throws IOException {
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
    void testProjectWithDifferentDependencyVersionsInBuildAndReportAndPlugins() throws IOException {
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

    @Test
    void testReportPluginUsesManagedVersionWhenArtifactVersionIsRelease() throws IOException {
        String version = "3.5.3";

        Artifact buildPlugin =
                ARTIFACT_FACTORY.createArtifact("org.apache.maven.plugins", "maven-surefire-plugin", version);
        Artifact reportPlugin =
                ARTIFACT_FACTORY.createArtifact("org.apache.maven.plugins", "maven-surefire-report-plugin", "RELEASE");

        rule.addBuildPlugin(extractGaString(buildPlugin));
        rule.addReportPlugin(extractGaString(reportPlugin));

        HashSet<Artifact> pluginArtifacts = new HashSet<>();
        pluginArtifacts.add(buildPlugin);

        HashSet<Artifact> reportArtifacts = new HashSet<>();
        reportArtifacts.add(reportPlugin);

        when(project.getArtifacts()).thenReturn(new HashSet<>());
        when(project.getPluginArtifacts()).thenReturn(pluginArtifacts);
        when(project.getReportArtifacts()).thenReturn(reportArtifacts);

        Plugin managedReportPlugin = new Plugin();
        managedReportPlugin.setGroupId("org.apache.maven.plugins");
        managedReportPlugin.setArtifactId("maven-surefire-report-plugin");
        managedReportPlugin.setVersion(version);

        Build build = new Build();
        PluginManagement pluginManagement = new PluginManagement();
        pluginManagement.setPlugins(Collections.singletonList(managedReportPlugin));
        build.setPluginManagement(pluginManagement);

        when(project.getBuild()).thenReturn(build);

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testReportPluginUsesBuildPluginVersionBeforeManagedVersion() throws IOException {
        String buildPluginVersion = "3.5.3";
        String managedVersion = "3.6.0";

        Artifact buildPlugin = ARTIFACT_FACTORY.createArtifact(
                "org.apache.maven.plugins", "maven-surefire-plugin", buildPluginVersion);
        Artifact reportPlugin =
                ARTIFACT_FACTORY.createArtifact("org.apache.maven.plugins", "maven-surefire-report-plugin", "RELEASE");

        rule.addBuildPlugin(extractGaString(buildPlugin));
        rule.addReportPlugin(extractGaString(reportPlugin));

        HashSet<Artifact> pluginArtifacts = new HashSet<>();
        pluginArtifacts.add(buildPlugin);

        HashSet<Artifact> reportArtifacts = new HashSet<>();
        reportArtifacts.add(reportPlugin);

        when(project.getArtifacts()).thenReturn(new HashSet<>());
        when(project.getPluginArtifacts()).thenReturn(pluginArtifacts);
        when(project.getReportArtifacts()).thenReturn(reportArtifacts);

        Plugin buildReportPlugin = new Plugin();
        buildReportPlugin.setGroupId("org.apache.maven.plugins");
        buildReportPlugin.setArtifactId("maven-surefire-report-plugin");
        buildReportPlugin.setVersion(buildPluginVersion);

        Plugin managedReportPlugin = new Plugin();
        managedReportPlugin.setGroupId("org.apache.maven.plugins");
        managedReportPlugin.setArtifactId("maven-surefire-report-plugin");
        managedReportPlugin.setVersion(managedVersion);

        Build build = new Build();
        build.setPlugins(Collections.singletonList(buildReportPlugin));

        PluginManagement pluginManagement = new PluginManagement();
        pluginManagement.setPlugins(Collections.singletonList(managedReportPlugin));
        build.setPluginManagement(pluginManagement);

        when(project.getBuild()).thenReturn(build);

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testReportPluginFailsWhenManagedVersionDiffersFromBuildPlugin() throws IOException {
        String buildPluginVersion = "3.5.3";
        String managedVersion = "3.6.0";

        Artifact buildPlugin = ARTIFACT_FACTORY.createArtifact(
                "org.apache.maven.plugins", "maven-surefire-plugin", buildPluginVersion);
        Artifact reportPlugin =
                ARTIFACT_FACTORY.createArtifact("org.apache.maven.plugins", "maven-surefire-report-plugin", "RELEASE");

        rule.addBuildPlugin(extractGaString(buildPlugin));
        rule.addReportPlugin(extractGaString(reportPlugin));

        HashSet<Artifact> pluginArtifacts = new HashSet<>();
        pluginArtifacts.add(buildPlugin);

        HashSet<Artifact> reportArtifacts = new HashSet<>();
        reportArtifacts.add(reportPlugin);

        when(project.getArtifacts()).thenReturn(new HashSet<>());
        when(project.getPluginArtifacts()).thenReturn(pluginArtifacts);
        when(project.getReportArtifacts()).thenReturn(reportArtifacts);

        Plugin managedReportPlugin = new Plugin();
        managedReportPlugin.setGroupId("org.apache.maven.plugins");
        managedReportPlugin.setArtifactId("maven-surefire-report-plugin");
        managedReportPlugin.setVersion(managedVersion);

        Build build = new Build();
        PluginManagement pluginManagement = new PluginManagement();
        pluginManagement.setPlugins(Collections.singletonList(managedReportPlugin));
        build.setPluginManagement(pluginManagement);

        when(project.getBuild()).thenReturn(build);

        assertThatThrownBy(rule::execute).isInstanceOf(EnforcerRuleException.class);
    }

    @Test
    void shouldOutputCustomMessageWhenVersionsDiffer() throws IOException {
        String customMessage = "Custom same versions message";
        rule.setMessage(customMessage);

        Artifact dep1 = constructArtifact("acme-dep", "1.0");
        Artifact dep2 = constructArtifact("acme-dep2", "2.0");

        HashSet<Artifact> dependencies = new HashSet<>();
        dependencies.add(dep1);
        dependencies.add(dep2);

        when(project.getArtifacts()).thenReturn(dependencies);
        when(project.getPluginArtifacts()).thenReturn(new HashSet<>());
        when(project.getReportArtifacts()).thenReturn(new HashSet<>());

        rule.addDependency("org.acme:acme-dep");
        rule.addDependency("org.acme:acme-dep2");

        assertThatThrownBy(() -> rule.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageStartingWith(customMessage);
    }

    private static Artifact constructArtifact(String artifactId, String version) throws IOException {
        return ARTIFACT_FACTORY.createArtifact("org.acme", artifactId, version);
    }

    private static String extractGaString(Artifact dependency) {
        return String.format("%s:%s", dependency.getGroupId(), dependency.getArtifactId());
    }
}
