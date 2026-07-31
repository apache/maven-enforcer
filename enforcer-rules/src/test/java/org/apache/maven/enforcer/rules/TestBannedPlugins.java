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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the "banned plugins" rule.
 */
@ExtendWith(MockitoExtension.class)
class TestBannedPlugins {

    @Mock
    private MavenSession session;

    private BannedPlugins rule;

    @BeforeEach
    void setUp() {
        rule = new BannedPlugins(session);
        rule.setLog(mock(EnforcerLogger.class));
    }

    @Test
    void shouldPassWhenNoPlugins() throws EnforcerRuleException {
        MavenProject project = mock(MavenProject.class);
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getPluginArtifacts()).thenReturn(Collections.emptySet());

        setField("excludes", Arrays.asList("*"));
        rule.execute();
    }

    @Test
    void shouldFailWhenBannedPluginFound() {
        MavenProject project = mock(MavenProject.class);
        when(session.getCurrentProject()).thenReturn(project);

        Artifact bannedPlugin = createArtifact("com.example", "banned-plugin", "1.0");
        when(project.getPluginArtifacts()).thenReturn(new HashSet<>(Arrays.asList(bannedPlugin)));

        setField("excludes", Arrays.asList("com.example:banned-plugin"));

        assertThatThrownBy(() -> rule.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("banned plugin");
    }

    @Test
    void shouldOutputCustomMessageWhenBanned() {
        String customMessage = "Custom banned plugins message";
        rule.setMessage(customMessage);

        MavenProject project = mock(MavenProject.class);
        when(session.getCurrentProject()).thenReturn(project);

        Artifact bannedPlugin = createArtifact("com.example", "banned-plugin", "1.0");
        when(project.getPluginArtifacts()).thenReturn(new HashSet<>(Arrays.asList(bannedPlugin)));

        setField("excludes", Arrays.asList("com.example:banned-plugin"));

        assertThatThrownBy(() -> rule.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageStartingWith(customMessage)
                .hasMessageContaining("banned plugin");
    }

    @Test
    void shouldPassWhenPluginIsIncluded() throws EnforcerRuleException {
        MavenProject project = mock(MavenProject.class);
        when(session.getCurrentProject()).thenReturn(project);

        Artifact allowedPlugin = createArtifact("com.example", "allowed-plugin", "1.0");
        when(project.getPluginArtifacts()).thenReturn(new HashSet<>(Arrays.asList(allowedPlugin)));

        setField("excludes", Arrays.asList("com.example:*"));
        setField("includes", Arrays.asList("com.example:allowed-plugin"));

        rule.execute(); // should not throw
    }

    private void setField(String fieldName, List<String> value) {
        try {
            Field field = BannedPlugins.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(rule, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Artifact createArtifact(String groupId, String artifactId, String version) {
        return new DefaultArtifact(
                groupId, artifactId, version, "compile", "jar", null, new DefaultArtifactHandler("jar"));
    }
}
