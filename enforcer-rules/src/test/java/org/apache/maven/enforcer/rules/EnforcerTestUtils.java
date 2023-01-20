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

import java.util.Properties;

import org.apache.maven.enforcer.rules.utils.DependencyNodeBuilder;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.graph.DependencyNode;

import static org.apache.maven.artifact.Artifact.SCOPE_TEST;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The Class EnforcerTestUtils.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public final class EnforcerTestUtils {

    /**
     * Gets the maven session.
     *
     * @return the maven session
     */
    public static MavenSession getMavenSession() {
        PlexusContainer mock = mock(PlexusContainer.class);

        MavenExecutionRequest mer = mock(MavenExecutionRequest.class);
        ProjectBuildingRequest buildingRequest = mock(ProjectBuildingRequest.class);
        when(buildingRequest.setRepositorySession(any())).thenReturn(buildingRequest);
        when(mer.getProjectBuildingRequest()).thenReturn(buildingRequest);

        Properties systemProperties = new Properties();
        systemProperties.put("maven.version", "3.0");
        when(mer.getUserProperties()).thenReturn(new Properties());
        when(mer.getSystemProperties()).thenReturn(systemProperties);

        MavenExecutionResult meResult = mock(MavenExecutionResult.class);

        return new MavenSession(mock, new DefaultRepositorySystemSession(), mer, meResult);
    }

    /**
     * New plugin.
     *
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @return the plugin
     */
    public static Plugin newPlugin(String groupId, String artifactId, String version) {
        InputSource inputSource = new InputSource();
        inputSource.setModelId("unit");

        Plugin plugin = new Plugin();
        plugin.setArtifactId(artifactId);
        plugin.setGroupId(groupId);
        plugin.setVersion(version);
        plugin.setLocation("version", new InputLocation(0, 0, inputSource));
        return plugin;
    }

    public static DependencyNode getDependencyNodeWithMultipleSnapshots() {
        return new DependencyNodeBuilder()
                .withType(DependencyNodeBuilder.Type.POM)
                .withChildNode(new DependencyNodeBuilder()
                        .withArtifactId("childA")
                        .withVersion("1.0.0")
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("childAA")
                                .withVersion("1.0.0-SNAPSHOT")
                                .build())
                        .build())
                .withChildNode(new DependencyNodeBuilder()
                        .withArtifactId("childB")
                        .withVersion("2.0.0-SNAPSHOT")
                        .build())
                .build();
    }

    public static DependencyNode getDependencyNodeWithMultipleTestSnapshots() {
        return new DependencyNodeBuilder()
                .withType(DependencyNodeBuilder.Type.POM)
                .withChildNode(new DependencyNodeBuilder()
                        .withArtifactId("childA")
                        .withVersion("1.0.0")
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("childAA")
                                .withVersion("1.0.0-SNAPSHOT")
                                .withScope(SCOPE_TEST)
                                .build())
                        .build())
                .withChildNode(new DependencyNodeBuilder()
                        .withArtifactId("childB")
                        .withVersion("2.0.0-SNAPSHOT")
                        .withScope(SCOPE_TEST)
                        .build())
                .build();
    }

    public static ClassRealm getTestClassRealm() {
        ClassWorld classWorld = new ClassWorld("test", EnforcerTestUtils.class.getClassLoader());
        return classWorld.getClassRealm("test");
    }
}
