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

import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestRequireProfileIdsExist {

    @Mock
    private MavenSession session;

    private RequireProfileIdsExist rule;

    @BeforeEach
    void setUp() {
        rule = new RequireProfileIdsExist(session);
        rule.setLog(mock(EnforcerLogger.class));
    }

    @Test
    void shouldOutputCustomMessageWhenProfileNotExists() {
        String customMessage = "Custom profile exist message";
        rule.setMessage(customMessage);

        ProjectBuildingRequest buildingRequest = mock(ProjectBuildingRequest.class);
        when(buildingRequest.getActiveProfileIds()).thenReturn(Arrays.asList("non-existing-profile"));
        when(buildingRequest.getInactiveProfileIds()).thenReturn(Collections.emptyList());
        when(session.getProjectBuildingRequest()).thenReturn(buildingRequest);

        MavenProject project = mock(MavenProject.class);
        when(project.getModel()).thenReturn(mock(org.apache.maven.model.Model.class));
        when(session.getProjects()).thenReturn(Collections.singletonList(project));

        Settings settings = mock(Settings.class);
        when(settings.getProfiles()).thenReturn(Collections.emptyList());
        when(session.getSettings()).thenReturn(settings);

        assertThatThrownBy(() -> rule.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageStartingWith(customMessage)
                .hasMessageContaining("non-existing-profile");
    }
}
