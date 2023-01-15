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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Class TestRequireReleaseVersion.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@ExtendWith(MockitoExtension.class)
class TestRequireReleaseVersion {

    @Mock
    private MavenProject project;

    @InjectMocks
    private RequireReleaseVersion rule;

    @Test
    void testProjectWithReleaseVersion() throws Exception {
        ArtifactStubFactory factory = new ArtifactStubFactory();

        when(project.getArtifact()).thenReturn(factory.getReleaseArtifact());

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testProjectWithSnapshotVersion() throws Exception {
        ArtifactStubFactory factory = new ArtifactStubFactory();

        when(project.getArtifact()).thenReturn(factory.getSnapshotArtifact());

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("This project cannot be a snapshot");
    }

    @Test
    void shouldFailWhenParentIsSnapshot() throws Exception {
        ArtifactStubFactory factory = new ArtifactStubFactory();

        when(project.getArtifact()).thenReturn(factory.getReleaseArtifact());
        when(project.getParentArtifact()).thenReturn(factory.getSnapshotArtifact());

        rule.setFailWhenParentIsSnapshot(true);

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("Parent Cannot be a snapshot");
    }

    @Test
    void shouldAllowParentSnapshot() throws Exception {
        ArtifactStubFactory factory = new ArtifactStubFactory();

        when(project.getArtifact()).thenReturn(factory.getReleaseArtifact());

        rule.setFailWhenParentIsSnapshot(false);

        assertThatCode(rule::execute).doesNotThrowAnyException();

        verify(project, never()).getParentArtifact();
    }

    /**
     * Test cache.
     */
    @Test
    void testCache() {
        assertThat(rule.getCacheId()).isNull();
    }
}
