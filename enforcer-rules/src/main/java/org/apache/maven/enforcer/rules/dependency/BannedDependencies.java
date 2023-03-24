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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rules.utils.ArtifactUtils;
import org.apache.maven.execution.MavenSession;

/**
 * This rule checks that lists of dependencies are not included.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@Named("bannedDependencies")
public final class BannedDependencies extends BannedDependenciesBase {

    @Inject
    BannedDependencies(MavenSession session, ResolverUtil resolverUtil) {
        super(session, resolverUtil);
    }

    @Override
    protected boolean validate(Artifact artifact) {
        return !ArtifactUtils.matchDependencyArtifact(artifact, getExcludes())
                || ArtifactUtils.matchDependencyArtifact(artifact, getIncludes());
    }

    @Override
    protected String getErrorMessage() {
        return "banned via the exclude/include list";
    }

    @Override
    public String toString() {
        return String.format(
                "BannedDependencies[message=%s, excludes=%s, includes=%s, searchTransitive=%b]",
                getMessage(), getExcludes(), getIncludes(), isSearchTransitive());
    }
}
