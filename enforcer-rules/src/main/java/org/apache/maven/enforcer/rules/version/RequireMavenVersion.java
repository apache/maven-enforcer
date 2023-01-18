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
package org.apache.maven.enforcer.rules.version;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Objects;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.rtinfo.RuntimeInformation;

/**
 * This rule checks that the Maven version is allowed.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@Named("requireMavenVersion")
public final class RequireMavenVersion extends AbstractVersionEnforcer {

    private final RuntimeInformation runtimeInformation;

    @Inject
    public RequireMavenVersion(RuntimeInformation runtimeInformation) {
        this.runtimeInformation = Objects.requireNonNull(runtimeInformation);
    }

    @Override
    public void execute() throws EnforcerRuleException {
        String mavenVersion = runtimeInformation.getMavenVersion();
        getLog().debug("Detected Maven Version: " + mavenVersion);
        if (mavenVersion == null) {
            throw new EnforcerRuleException("Unable to detect Maven Version");
        }
        DefaultArtifactVersion detectedVersion = new DefaultArtifactVersion(mavenVersion);
        enforceVersion("Maven", getVersion(), detectedVersion);
    }

    @Override
    public String toString() {
        return String.format("%s[message=%s, version=%s]", getClass().getSimpleName(), getMessage(), getVersion());
    }
}
