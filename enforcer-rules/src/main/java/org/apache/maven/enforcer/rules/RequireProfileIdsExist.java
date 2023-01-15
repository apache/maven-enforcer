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
import java.util.Objects;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Ensure that all profiles mentioned on the commandline do exist.
 *
 * @author Robert Scholte
 * @author Gabriel Belingueres
 */
@Named("requireProfileIdsExist")
public final class RequireProfileIdsExist extends AbstractStandardEnforcerRule {

    private final MavenSession session;

    @Inject
    public RequireProfileIdsExist(MavenSession session) {
        this.session = Objects.requireNonNull(session);
    }

    @Override
    public void execute() throws EnforcerRuleException {

        List<String> profileIds = new ArrayList<>();
        profileIds.addAll(session.getProjectBuildingRequest().getActiveProfileIds());
        profileIds.addAll(session.getProjectBuildingRequest().getInactiveProfileIds());

        for (MavenProject project : session.getProjects()) {
            // iterate over all parents
            MavenProject currentProject = project;
            do {
                for (org.apache.maven.model.Profile profile :
                        currentProject.getModel().getProfiles()) {
                    profileIds.remove(profile.getId());

                    if (profileIds.isEmpty()) {
                        return;
                    }
                }

                currentProject = currentProject.getParent();
            } while (currentProject != null);
        }

        for (org.apache.maven.settings.Profile profile : session.getSettings().getProfiles()) {
            profileIds.remove(profile.getId());
        }

        if (profileIds.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (profileIds.size() > 1) {
            sb.append("The requested profiles don't exist: ");
        } else {
            sb.append("The requested profile doesn't exist: ");
        }
        sb.append(StringUtils.join(profileIds.iterator(), ", "));

        throw new EnforcerRuleException(sb.toString());
    }
}
