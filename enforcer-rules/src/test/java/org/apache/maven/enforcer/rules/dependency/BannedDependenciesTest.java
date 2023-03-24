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

import java.util.Collections;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.DependencyNodeBuilder;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * The Class BannedDependenciesTest.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@ExtendWith(MockitoExtension.class)
class BannedDependenciesTest {

    private static final ArtifactStubFactory ARTIFACT_STUB_FACTORY = new ArtifactStubFactory();

    @Mock
    private MavenProject project;

    @Mock
    private MavenSession session;

    @Mock
    private ResolverUtil resolverUtil;

    @InjectMocks
    private BannedDependencies rule;

    @Test
    void excludesDoNotUseTransitiveDependencies() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getDependencyArtifacts()).thenReturn(ARTIFACT_STUB_FACTORY.getScopedArtifacts());

        rule.setSearchTransitive(false);
        rule.setExcludes(Collections.singletonList("*"));

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("g:runtime:jar:1.0 <--- banned via the exclude/include list")
                .hasMessageContaining("g:compile:jar:1.0 <--- banned via the exclude/include list")
                .hasMessageContaining("g:provided:jar:1.0 <--- banned via the exclude/include list")
                .hasMessageContaining("g:test:jar:1.0 <--- banned via the exclude/include list")
                .hasMessageContaining("g:system:jar:1.0 <--- banned via the exclude/include list");
    }

    @Test
    void excludesAndIncludesDoNotUseTransitiveDependencies() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getDependencyArtifacts()).thenReturn(ARTIFACT_STUB_FACTORY.getScopedArtifacts());

        rule.setSearchTransitive(false);
        rule.setExcludes(Collections.singletonList("*"));
        rule.setIncludes(Collections.singletonList("g:compile"));

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("g:runtime:jar:1.0 <--- banned via the exclude/include list")
                .hasMessageContaining("g:provided:jar:1.0 <--- banned via the exclude/include list")
                .hasMessageContaining("g:test:jar:1.0 <--- banned via the exclude/include list")
                .hasMessageContaining("g:system:jar:1.0 <--- banned via the exclude/include list")
                .hasMessageNotContaining("g:compile:jar:1.0");
    }

    @Test
    void excludesUseTransitiveDependencies() throws Exception {

        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder()
                        .withType(DependencyNodeBuilder.Type.POM)
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("childA")
                                .withVersion("1.0.0")
                                .withChildNode(new DependencyNodeBuilder()
                                        .withType(DependencyNodeBuilder.Type.WAR)
                                        .withArtifactId("childAA")
                                        .withVersion("1.0.0-SNAPSHOT")
                                        .build())
                                .withChildNode(new DependencyNodeBuilder()
                                        .withType(DependencyNodeBuilder.Type.WAR)
                                        .withArtifactId("childAB")
                                        .withVersion("1.0.0-SNAPSHOT")
                                        .build())
                                .build())
                        .build());

        rule.setSearchTransitive(true);
        rule.setExcludes(Collections.singletonList("*:*:*:war"));

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining(
                        "default-group:childAA:war:classifier:1.0.0-SNAPSHOT <--- banned via the exclude/include list")
                .hasMessageContaining(
                        "default-group:childAB:war:classifier:1.0.0-SNAPSHOT <--- banned via the exclude/include list");
    }

    @Test
    void excludesAndIncludesUseTransitiveDependencies() throws Exception {

        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder()
                        .withType(DependencyNodeBuilder.Type.POM)
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("childA")
                                .withVersion("1.0.0")
                                .withChildNode(new DependencyNodeBuilder()
                                        .withType(DependencyNodeBuilder.Type.WAR)
                                        .withArtifactId("childAA")
                                        .withVersion("1.0.0-SNAPSHOT")
                                        .build())
                                .withChildNode(new DependencyNodeBuilder()
                                        .withType(DependencyNodeBuilder.Type.WAR)
                                        .withArtifactId("childAB")
                                        .withVersion("1.0.0-SNAPSHOT")
                                        .build())
                                .build())
                        .build());

        rule.setSearchTransitive(true);
        rule.setExcludes(Collections.singletonList("*:*:*:war"));
        rule.setIncludes(Collections.singletonList("*:childAB:*:war"));

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining(
                        "default-group:childAA:war:classifier:1.0.0-SNAPSHOT <--- banned via the exclude/include list")
                .hasMessageNotContaining("childAB");
    }

    @Test
    void invalidExcludeFormat() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getDependencyArtifacts()).thenReturn(ARTIFACT_STUB_FACTORY.getScopedArtifacts());

        rule.setSearchTransitive(false);
        rule.setExcludes(Collections.singletonList("::::::::::"));

        assertThatCode(rule::execute).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidIncludeFormat() throws Exception {
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getDependencyArtifacts()).thenReturn(ARTIFACT_STUB_FACTORY.getScopedArtifacts());

        rule.setSearchTransitive(false);
        rule.setExcludes(Collections.singletonList("*"));
        rule.setIncludes(Collections.singletonList("*:*:x:x:x:x:x:x:x:x:"));

        assertThatCode(rule::execute).isInstanceOf(IllegalArgumentException.class);
    }
}
