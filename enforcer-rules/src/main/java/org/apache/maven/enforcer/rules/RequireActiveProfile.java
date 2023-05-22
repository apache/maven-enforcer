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

import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

/**
 * This rule checks that some profiles are active.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@Named("requireActiveProfile")
public final class RequireActiveProfile extends AbstractStandardEnforcerRule {

    /** Comma separated list of profiles to check.
     */
    private String profiles = null;

    /** If all profiles must be active. If false, only one must be active
     */
    private boolean all = true;

    private final MavenProject project;

    @Inject
    public RequireActiveProfile(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(String profiles) {
        this.profiles = profiles;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        List<String> missingProfiles = new ArrayList<>();
        if (profiles != null && !profiles.isEmpty()) {
            String[] profileIds = profiles.split(",");
            for (String profileId : profileIds) {
                if (!isProfileActive(project, profileId)) {
                    missingProfiles.add(profileId);
                }
            }

            boolean fail = false;
            if (!missingProfiles.isEmpty()) {
                if (all || missingProfiles.size() == profileIds.length) {
                    fail = true;
                }
            }

            if (fail) {
                String message = getMessage();
                StringBuilder buf = new StringBuilder();
                if (message != null) {
                    buf.append(message + System.lineSeparator());
                }

                for (String profile : missingProfiles) {
                    buf.append("Profile \"" + profile + "\" is not activated." + System.lineSeparator());
                }

                throw new EnforcerRuleException(buf.toString());
            }
        }
    }

    /**
     * Checks if profile is active.
     *
     * @param project the project
     * @param profileId the profile name
     * @return <code>true</code> if profile is active, otherwise <code>false</code>
     */
    private boolean isProfileActive(MavenProject project, String profileId) {
        for (Map.Entry<String, List<String>> entry :
                project.getInjectedProfileIds().entrySet()) {
            if (entry.getValue().contains(profileId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("RequireActiveProfile[message=%s, profiles=%s, all=%b]", getMessage(), profiles, all);
    }
}
