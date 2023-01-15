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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for the RequireSnapshotVersion rule.
 */
@ExtendWith(MockitoExtension.class)
class TestRequireSnapshotVersion {

    @Mock
    private MavenProject project;

    private ArtifactStubFactory factory;

    @InjectMocks
    private RequireSnapshotVersion rule;

    @BeforeEach
    public void before() {
        factory = new ArtifactStubFactory();
    }

    @Test
    void shouldFailForRelease() throws Exception {
        when(project.getArtifact()).thenReturn(factory.getReleaseArtifact());

        assertThatCode(rule::execute).isInstanceOf(EnforcerRuleException.class);
    }

    @Test
    void shouldPassForSnapshot() throws Exception {
        when(project.getArtifact()).thenReturn(factory.getSnapshotArtifact());

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void shouldFailForReleaseParent() throws Exception {
        when(project.getArtifact()).thenReturn(factory.getSnapshotArtifact());

        MavenProject parent = mock(MavenProject.class);
        when(parent.getArtifact()).thenReturn(factory.getReleaseArtifact());

        when(project.getParent()).thenReturn(parent);
        when(project.hasParent()).thenReturn(true);

        rule.setFailWhenParentIsRelease(true);

        assertThatCode(rule::execute).isInstanceOf(EnforcerRuleException.class);
    }

    @Test
    void shouldPassForSnapshotParent() throws Exception {
        when(project.getArtifact()).thenReturn(factory.getSnapshotArtifact());

        MavenProject parent = mock(MavenProject.class);
        when(parent.getArtifact()).thenReturn(factory.getSnapshotArtifact());

        when(project.getParent()).thenReturn(parent);
        when(project.hasParent()).thenReturn(true);

        rule.setFailWhenParentIsRelease(true);

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void parentShouldNotBeChecked() throws Exception {
        when(project.getArtifact()).thenReturn(factory.getSnapshotArtifact());

        rule.setFailWhenParentIsRelease(false);

        assertThatCode(rule::execute).doesNotThrowAnyException();

        verify(project).getArtifact();
        verifyNoMoreInteractions(project);
    }
}
