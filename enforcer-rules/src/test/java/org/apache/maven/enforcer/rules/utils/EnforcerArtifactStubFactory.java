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
package org.apache.maven.enforcer.rules.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * Builds detached {@link Artifact} stubs for rule tests. No file is created and none is attached; a test that
 * needs one calls {@link Artifact#setFile}.
 */
public class EnforcerArtifactStubFactory {

    /**
     * @return {@code testGroupId:release:jar:1.0}, compile scope
     */
    public Artifact getReleaseArtifact() {
        return createArtifact("testGroupId", "release", "1.0");
    }

    /**
     * @return {@code testGroupId:snapshot:jar:2.0-SNAPSHOT}, compile scope
     */
    public Artifact getSnapshotArtifact() {
        return createArtifact("testGroupId", "snapshot", "2.0-SNAPSHOT");
    }

    /**
     * @return one jar per scope: {@code g:compile}, {@code g:provided}, {@code g:test}, {@code g:runtime},
     *         {@code g:system}, all at 1.0
     */
    public Set<Artifact> getScopedArtifacts() {
        Set<Artifact> set = new HashSet<>();
        set.add(createArtifact("g", "compile", "1.0", Artifact.SCOPE_COMPILE, "jar", ""));
        set.add(createArtifact("g", "provided", "1.0", Artifact.SCOPE_PROVIDED, "jar", ""));
        set.add(createArtifact("g", "test", "1.0", Artifact.SCOPE_TEST, "jar", ""));
        set.add(createArtifact("g", "runtime", "1.0", Artifact.SCOPE_RUNTIME, "jar", ""));
        set.add(createArtifact("g", "system", "1.0", Artifact.SCOPE_SYSTEM, "jar", ""));
        return set;
    }

    /**
     * @return one compile-scoped artifact per type: {@code g:a:war}, {@code g:b:jar}, {@code g:c:sources},
     *         {@code g:d:zip}, {@code g:e:rar}, all at 1.0
     */
    public Set<Artifact> getTypedArtifacts() {
        Set<Artifact> set = new HashSet<>();
        set.add(createArtifact("g", "a", "1.0", Artifact.SCOPE_COMPILE, "war", null));
        set.add(createArtifact("g", "b", "1.0", Artifact.SCOPE_COMPILE, "jar", null));
        set.add(createArtifact("g", "c", "1.0", Artifact.SCOPE_COMPILE, "sources", null));
        set.add(createArtifact("g", "d", "1.0", Artifact.SCOPE_COMPILE, "zip", null));
        set.add(createArtifact("g", "e", "1.0", Artifact.SCOPE_COMPILE, "rar", null));
        return set;
    }

    /**
     * @return a compile-scoped jar for the given coordinates
     */
    public Artifact createArtifact(String groupId, String artifactId, String version) {
        return createArtifact(groupId, artifactId, version, Artifact.SCOPE_COMPILE, "jar", "");
    }

    private Artifact createArtifact(
            String groupId, String artifactId, String version, String scope, String type, String classifier) {
        DefaultArtifactHandler handler = new DefaultArtifactHandler(type);
        Artifact artifact = new DefaultArtifact(
                groupId, artifactId, VersionRange.createFromVersion(version), scope, type, classifier, handler, false);
        artifact.setRelease(!artifact.isSnapshot());
        return artifact;
    }
}
