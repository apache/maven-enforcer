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
package org.apache.maven.plugins.enforcer;

import java.io.IOException;
import java.util.Collections;

import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugins.enforcer.utils.EnforcerRuleUtilsHelper;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.maven.plugins.enforcer.EnforcerTestUtils.getDependencyNodeWithMultipleSnapshots;
import static org.apache.maven.plugins.enforcer.EnforcerTestUtils.getDependencyNodeWithMultipleTestSnapshots;
import static org.apache.maven.plugins.enforcer.EnforcerTestUtils.provideCollectDependencies;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequireReleaseDeps}
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>, Andrzej Jarmoniuk
 */
public class TestRequireReleaseDeps {
    private MavenProject project;
    private static final ArtifactStubFactory ARTIFACT_STUB_FACTORY = new ArtifactStubFactory();
    ;
    private EnforcerRuleHelper ruleHelper;
    private RequireReleaseDeps rule;

    @BeforeEach
    public void setUp() {
        project = new MockProject();
        ruleHelper = EnforcerTestUtils.getHelper(project);
        rule = new RequireReleaseDeps();
    }

    @Test
    public void testSearchNonTransitive() throws IOException {
        project.setDependencyArtifacts(ARTIFACT_STUB_FACTORY.getScopedArtifacts());
        rule.setSearchTransitive(false);
        EnforcerRuleUtilsHelper.execute(rule, ruleHelper, false);
    }

    @Test
    public void testSearchTransitiveMultipleFailures() {
        rule.setSearchTransitive(true);
        provideCollectDependencies(getDependencyNodeWithMultipleSnapshots());
        EnforcerRuleUtilsHelper.execute(rule, ruleHelper, true);
    }

    @Test
    public void testSearchTransitiveNoFailures() {
        rule.setSearchTransitive(true);
        EnforcerRuleUtilsHelper.execute(rule, ruleHelper, false);
    }

    @Test
    public void testShouldFailOnlyWhenRelease() throws IOException {
        project.setArtifact(ARTIFACT_STUB_FACTORY.getSnapshotArtifact());
        provideCollectDependencies(getDependencyNodeWithMultipleSnapshots());
        rule.setOnlyWhenRelease(true);
        EnforcerRuleUtilsHelper.execute(rule, ruleHelper, false);
    }

    @Test
    void testWildcardExcludeTests() throws Exception {
        rule.setExcludes(Collections.singletonList("*:*:*:*:test"));
        provideCollectDependencies(getDependencyNodeWithMultipleTestSnapshots());
        rule.setSearchTransitive(true);
        EnforcerRuleUtilsHelper.execute(rule, ruleHelper, false);
    }

    @Test
    void testWildcardExcludeAll() throws Exception {
        rule.setExcludes(Collections.singletonList("*"));
        provideCollectDependencies(getDependencyNodeWithMultipleSnapshots());
        rule.setSearchTransitive(true);
        EnforcerRuleUtilsHelper.execute(rule, ruleHelper, false);
    }

    @Test
    void testExcludesAndIncludes() throws Exception {
        rule.setExcludes(Collections.singletonList("*"));
        rule.setIncludes(Collections.singletonList("*:*:*:*:test"));
        provideCollectDependencies(getDependencyNodeWithMultipleTestSnapshots());
        rule.setSearchTransitive(true);
        EnforcerRuleUtilsHelper.execute(rule, ruleHelper, true);
    }

    /**
     * Test id.
     */
    @Test
    void testId() {
        assertThat(rule.getCacheId()).isEqualTo("0");
    }

    @Test
    void testFailWhenParentIsSnapshotFalse() throws IOException {
        MavenProject parent = new MockProject();
        parent.setArtifact(ARTIFACT_STUB_FACTORY.getSnapshotArtifact());
        project.setParent(parent);
        rule.setFailWhenParentIsSnapshot(false);
        EnforcerRuleUtilsHelper.execute(rule, ruleHelper, false);
    }

    @Test
    void parentShouldBeExcluded() throws IOException {
        MavenProject parent = new MockProject();
        parent.setArtifact(ARTIFACT_STUB_FACTORY.getSnapshotArtifact());
        project.setParent(parent);
        rule.setExcludes(Collections.singletonList(parent.getArtifact().getGroupId() + ":*"));
        EnforcerRuleUtilsHelper.execute(rule, ruleHelper, false);
    }
}
