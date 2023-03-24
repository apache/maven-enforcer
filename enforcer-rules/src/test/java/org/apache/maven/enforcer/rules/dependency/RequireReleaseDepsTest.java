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
package org.apache.maven.enforcer.rules.dependency;

import java.io.IOException;
import java.util.Collections;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.DependencyNodeBuilder;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.apache.maven.enforcer.rules.EnforcerTestUtils.getDependencyNodeWithMultipleSnapshots;
import static org.apache.maven.enforcer.rules.EnforcerTestUtils.getDependencyNodeWithMultipleTestSnapshots;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequireReleaseDeps}
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>, Andrzej Jarmoniuk
 */
@ExtendWith(MockitoExtension.class)
class RequireReleaseDepsTest {
    private static final ArtifactStubFactory ARTIFACT_STUB_FACTORY = new ArtifactStubFactory();

    @Mock
    private MavenProject project;

    @Mock
    private MavenSession session;

    @Mock
    private ResolverUtil resolverUtil;

    @InjectMocks
    private RequireReleaseDeps rule;

    @BeforeEach
    void setUp() {
        // ruleHelper = EnforcerTestUtils.getHelper(project);
        // rule = new RequireReleaseDeps();
    }

    @Test
    void testSearchNonTransitive() throws IOException {
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getDependencyArtifacts()).thenReturn(ARTIFACT_STUB_FACTORY.getScopedArtifacts());
        rule.setSearchTransitive(false);

        assertThatCode(rule::execute).doesNotThrowAnyException();

        verifyNoInteractions(resolverUtil);
    }

    @Test
    void testSearchTransitiveMultipleFailures() throws Exception {
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(getDependencyNodeWithMultipleSnapshots());
        rule.setSearchTransitive(true);

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining(
                        "default-group:childAA:jar:classifier:1.0.0-SNAPSHOT <--- is not a release dependency")
                .hasMessageContaining(
                        "default-group:childB:jar:classifier:2.0.0-SNAPSHOT <--- is not a release dependency");
    }

    @Test
    void testSearchTransitiveNoFailures() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder().build());

        rule.setSearchTransitive(true);
        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testShouldFailOnlyWhenRelease() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getArtifact()).thenReturn(ARTIFACT_STUB_FACTORY.getSnapshotArtifact());
        rule.setOnlyWhenRelease(true);

        assertThatCode(rule::execute).doesNotThrowAnyException();

        verifyNoInteractions(resolverUtil);
    }

    @Test
    void testWildcardExcludeTests() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(getDependencyNodeWithMultipleTestSnapshots());

        rule.setExcludes(Collections.singletonList("*:*:*:*:test"));
        rule.setSearchTransitive(true);

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testWildcardExcludeAll() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(getDependencyNodeWithMultipleTestSnapshots());

        rule.setExcludes(Collections.singletonList("*"));
        rule.setSearchTransitive(true);

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testExcludesAndIncludes() throws Exception {
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(getDependencyNodeWithMultipleTestSnapshots());

        rule.setExcludes(Collections.singletonList("*"));
        rule.setIncludes(Collections.singletonList("*:*:*:*:test"));
        rule.setSearchTransitive(true);

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining(
                        "default-group:childAA:jar:classifier:1.0.0-SNAPSHOT <--- is not a release dependency")
                .hasMessageContaining(
                        "default-group:childB:jar:classifier:2.0.0-SNAPSHOT <--- is not a release dependency");
    }

    /**
     * Test id.
     */
    @Test
    void testId() {
        assertThat(rule.getCacheId()).isNull();
    }

    @Test
    void testFailWhenParentIsSnapshot() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getParentArtifact()).thenReturn(ARTIFACT_STUB_FACTORY.getSnapshotArtifact());
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder().build());

        rule.setFailWhenParentIsSnapshot(true);

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("Parent Cannot be a snapshot: testGroupId:snapshot:jar:2.0-SNAPSHOT");
    }

    @Test
    void parentShouldBeExcluded() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getParentArtifact()).thenReturn(ARTIFACT_STUB_FACTORY.getSnapshotArtifact());
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder().build());

        rule.setFailWhenParentIsSnapshot(true);
        rule.setExcludes(Collections.singletonList("testGroupId:*"));

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }
}
