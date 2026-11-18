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

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.DependencyNodeBuilder;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequireUpperBoundDepsTest {

    @Mock
    private ResolverUtil resolverUtil;

    @Mock
    private MavenProject project;

    @InjectMocks
    private RequireUpperBoundDeps rule;

    @Test
    void testRule() throws Exception {

        rule.setLog(mock(EnforcerLogger.class));

        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder()
                        .withType(DependencyNodeBuilder.Type.POM)
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("childA")
                                .withVersion("1.0.0")
                                .build())
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("childA")
                                .withVersion("2.0.0")
                                .build())
                        .build());

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("default-group:childA:1.0.0:classifier")
                .hasMessageContaining("default-group:childA:2.0.0:classifier")
                .hasMessageContaining("<groupId>default-group</groupId>")
                .hasMessageContaining("<artifactId>childA</artifactId>")
                .hasMessageContaining("<version>2.0.0</version>")
                .hasMessageContaining("<classifier>classifier</classifier>");
    }

    @Test
    void testManagedVersion() throws Exception {

        rule.setLog(mock(EnforcerLogger.class));

        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder()
                        .withType(DependencyNodeBuilder.Type.POM)
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("childA")
                                .withVersion("1.0.0")
                                .withPremanagedVersion("2.0.0")
                                .build())
                        .build());

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("default-group:childA:1.0.0:classifier (version managed from 2.0.0)")
                .hasMessageContaining("<groupId>default-group</groupId>")
                .hasMessageContaining("<artifactId>childA</artifactId>")
                .hasMessageContaining("<version>2.0.0</version>");
    }

    @Test
    void testManagedVersionAboveEveryRequestedVersion() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(buildTreeRequestingUtil("1.4", "1.7", "2.0"));
        manageUtil("2.0", "default-group:project:1.0");

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("Require upper bound dependencies error for default-group:util:2.0:classifier. "
                        + "The managed version is above every requested version.")
                .hasMessageContaining("default-group:util:2.0:classifier (version managed from 1.4)")
                .hasMessageContaining("default-group:util:2.0:classifier (version managed from 1.7)")
                .hasMessageContaining("<version>1.7</version>");
    }

    @Test
    void testManagedVersionEqualToHighestRequestedVersion() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(buildTreeRequestingUtil("1.4", "1.7", "1.7"));
        manageUtil("1.7", "default-group:project:1.0");

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testManagedVersionAboveEveryRequestedVersionWithoutOption() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(buildTreeRequestingUtil("1.4", "1.7", "2.0"));

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testManagedVersionAboveEveryRequestedVersionInParent() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(buildTreeRequestingUtil("1.4", "1.7", "2.0"));
        manageUtil("2.0", "default-group:parent:1.0");
        MavenProject parent = mock(MavenProject.class);
        Model parentModel = new Model();
        parentModel.setGroupId("default-group");
        parentModel.setArtifactId("parent");
        parentModel.setVersion("1.0");
        when(parent.getOriginalModel()).thenReturn(parentModel);
        when(project.getParent()).thenReturn(parent);

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("default-group:util:2.0:classifier. "
                        + "The managed version is above every requested version.");
    }

    @Test
    void testManagedVersionAboveEveryRequestedVersionFromImportedBom() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(buildTreeRequestingUtil("1.4", "1.7", "2.0"));
        manageUtil("2.0", "default-group:bom:1.0");

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testManagedVersionAboveEveryRequestedVersionExcluded() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        rule.setExcludes(Collections.singletonList("default-group:util"));
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(buildTreeRequestingUtil("1.4", "1.7", "2.0"));
        manageUtil("2.0", "default-group:project:1.0");

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testManagedVersionInsideRequestedRange() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(buildTreeRequestingUtil("1.4", "[1.5,3.0)", "2.0"));
        manageUtil("2.0", "default-group:project:1.0");

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testManagedVersionAboveDirectDependency() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder()
                        .withType(DependencyNodeBuilder.Type.POM)
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("util")
                                .withVersion("2.0")
                                .build())
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("lib-a")
                                .withVersion("1.0")
                                .withChildNode(new DependencyNodeBuilder()
                                        .withArtifactId("util")
                                        .withVersion("2.0")
                                        .withPremanagedVersion("1.4")
                                        .build())
                                .build())
                        .build());
        manageUtil("2.0", "default-group:project:1.0");

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testManagedVersionAboveEveryRequestedVersionWithoutDependencyManagement() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(buildTreeRequestingUtil("1.4", "1.7", "2.0"));

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testManagedDependencyAbsentFromTree() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder()
                        .withType(DependencyNodeBuilder.Type.POM)
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("lib-a")
                                .withVersion("1.0")
                                .build())
                        .build());
        manageUtil("2.0", "default-group:project:1.0");

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testManagedVersionAboveEveryRequestedVersionWithoutRecordedSource() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(buildTreeRequestingUtil("1.4", "1.7", "2.0"));
        manageUtil("2.0", "default-group:project:1.0");
        project.getDependencyManagement().getDependencies().get(0).setLocation("version", null);

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("default-group:util:2.0:classifier. "
                        + "The managed version is above every requested version.");
    }

    @Test
    void testManagedVersionAboveEveryRequestedVersionInExcludedScope() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder()
                        .withType(DependencyNodeBuilder.Type.POM)
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("lib-a")
                                .withVersion("1.0")
                                .withScope("test")
                                .withChildNode(new DependencyNodeBuilder()
                                        .withArtifactId("util")
                                        .withVersion("2.0")
                                        .withPremanagedVersion("1.4")
                                        .withScope("test")
                                        .build())
                                .build())
                        .build());
        manageUtil("2.0", "default-group:project:1.0");

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    @Test
    void testManagedVersionAboveEveryRequestedVersionOfOtherClassifier() throws Exception {
        rule.setLog(mock(EnforcerLogger.class));
        rule.setFailWhenManagedVersionIsAboveRequested(true);
        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder()
                        .withType(DependencyNodeBuilder.Type.POM)
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("lib-a")
                                .withVersion("1.0")
                                .withChildNode(new DependencyNodeBuilder()
                                        .withArtifactId("util")
                                        .withClassifier("tests")
                                        .withVersion("2.0")
                                        .withPremanagedVersion("1.4")
                                        .build())
                                .build())
                        .build());
        manageUtil("2.0", "default-group:project:1.0");

        assertThatCode(rule::execute).doesNotThrowAnyException();
    }

    private DependencyNode buildTreeRequestingUtil(
            String libAUtilVersion, String libBUtilVersion, String managedUtilVersion) {
        return new DependencyNodeBuilder()
                .withType(DependencyNodeBuilder.Type.POM)
                .withChildNode(new DependencyNodeBuilder()
                        .withArtifactId("lib-a")
                        .withVersion("1.0")
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("util")
                                .withVersion(managedUtilVersion)
                                .withPremanagedVersion(libAUtilVersion)
                                .build())
                        .build())
                .withChildNode(new DependencyNodeBuilder()
                        .withArtifactId("lib-b")
                        .withVersion("1.0")
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("util")
                                .withVersion(managedUtilVersion)
                                .withPremanagedVersion(libBUtilVersion)
                                .build())
                        .build())
                .build();
    }

    private void manageUtil(String version, String declaringModelId) {
        Model model = new Model();
        model.setGroupId("default-group");
        model.setArtifactId("project");
        model.setVersion("1.0");
        when(project.getOriginalModel()).thenReturn(model);

        InputSource source = new InputSource();
        source.setModelId(declaringModelId);
        Dependency util = new Dependency();
        util.setGroupId("default-group");
        util.setArtifactId("util");
        util.setClassifier("classifier");
        util.setVersion(version);
        util.setLocation("version", new InputLocation(1, 1, source));
        DependencyManagement dependencyManagement = new DependencyManagement();
        dependencyManagement.addDependency(util);
        when(project.getDependencyManagement()).thenReturn(dependencyManagement);
    }
}
