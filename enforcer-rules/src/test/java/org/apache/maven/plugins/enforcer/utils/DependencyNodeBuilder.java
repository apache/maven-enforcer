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
package org.apache.maven.plugins.enforcer.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

import static java.util.Optional.ofNullable;
import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;

public class DependencyNodeBuilder {
    public enum Type {
        WAR(RepositoryUtils.newArtifactType("war", new DefaultArtifactHandler("war"))),
        JAR(RepositoryUtils.newArtifactType("jar", new DefaultArtifactHandler("jar"))),
        POM(RepositoryUtils.newArtifactType("pom", new DefaultArtifactHandler("pom")));

        private final ArtifactType artifactType;

        Type(ArtifactType artifactType) {
            this.artifactType = artifactType;
        }

        ArtifactType asArtifactType() {
            return artifactType;
        }

        String asString() {
            return name().toLowerCase();
        }
    }

    private String groupId;

    private String artifactId;

    private String version;

    private Type type;

    private String scope;

    private boolean optional;

    private List<DependencyNode> children = new ArrayList<>();

    public DependencyNodeBuilder withGroupId(String val) {
        groupId = val;
        return this;
    }

    public DependencyNodeBuilder withArtifactId(String val) {
        artifactId = val;
        return this;
    }

    public DependencyNodeBuilder withVersion(String val) {
        version = val;
        return this;
    }

    public DependencyNodeBuilder withType(Type val) {
        type = val;
        return this;
    }

    public DependencyNodeBuilder withScope(String val) {
        scope = val;
        return this;
    }

    public DependencyNodeBuilder withOptional(boolean val) {
        optional = val;
        return this;
    }

    public DependencyNodeBuilder withChildNode(DependencyNode val) {
        children.add(val);
        return this;
    }

    public DependencyNode build() {
        Artifact artifact = new DefaultArtifact(
                ofNullable(groupId).orElse("default-group"),
                ofNullable(artifactId).orElse("default-artifact"),
                "classifier",
                ofNullable(type).map(Type::asString).orElse("pom"),
                ofNullable(version).orElse("default-version"),
                ofNullable(type).orElse(Type.JAR).asArtifactType());
        Dependency dependency = new Dependency(artifact, ofNullable(scope).orElse(SCOPE_COMPILE), optional);
        DependencyNode instance = new DefaultDependencyNode(dependency);
        instance.setChildren(children);
        return instance;
    }
}
