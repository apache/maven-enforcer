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
import java.util.List;
import java.util.Map;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Check the profile rule.
 *
 * @author <a href="mailto:khmarbaise@apache.org">Karl Heinz Marbaise</a>
 */
@ExtendWith(MockitoExtension.class)
class RequireActiveProfileTest {

    @Mock
    private MavenProject project;

    @InjectMocks
    private RequireActiveProfile rule;

    @Test
    void testNoActiveProfilesInProjectAndNoProfilesExpectedToBeActivated() throws EnforcerRuleException {

        rule.execute();
    }

    @Test
    void testActiveProfileAndExpectedActiveProfile() throws EnforcerRuleException {
        Map<String, List<String>> profiles = Collections.singletonMap("pom", Arrays.asList("profile-2"));

        when(project.getInjectedProfileIds()).thenReturn(profiles);

        rule.setProfiles("profile-2");

        rule.execute();
    }

    @Test
    void testNoActiveProfileButTheRuleRequestedAnActiveProfile() {
        assertThrows(EnforcerRuleException.class, () -> {
            when(project.getInjectedProfileIds()).thenReturn(Collections.emptyMap());

            rule.setProfiles("profile-2");

            rule.execute();
            // intentionally no assertTrue(...)
        });
        // intentionally no assertTrue(...)
    }

    @Test
    void testNoActiveProfileButWeExpectToGetAnExceptionWithAll() {
        assertThrows(EnforcerRuleException.class, () -> {
            when(project.getInjectedProfileIds()).thenReturn(Collections.emptyMap());

            rule.setProfiles("profile-2");
            rule.setAll(true);

            rule.execute();
            // intentionally no assertTrue(...)
        });
        // intentionally no assertTrue(...)
    }

    @Test
    void testTwoActiveProfilesWithOneRequiredProfile() throws EnforcerRuleException {
        Map<String, List<String>> profiles = Collections.singletonMap("pom", Arrays.asList("profile-1", "profile-2"));

        when(project.getInjectedProfileIds()).thenReturn(profiles);

        rule.setProfiles("profile-2");

        rule.execute();
    }

    @Test
    void testTwoActiveProfilesWhereOneProfileIsRequiredToBeActivated() throws EnforcerRuleException {
        Map<String, List<String>> profiles = Collections.singletonMap("pom", Arrays.asList("profile-1", "profile-2"));

        when(project.getInjectedProfileIds()).thenReturn(profiles);

        rule.setProfiles("profile-2");
        rule.setAll(true);

        rule.execute();
    }

    @Test
    void testTwoActiveProfilesWithTwoRequiredProfilesWhereOneOfThemIsNotPartOfTheActiveProfiles() {
        assertThrows(EnforcerRuleException.class, () -> {
            Map<String, List<String>> profiles =
                    Collections.singletonMap("pom", Arrays.asList("profile-X", "profile-Y"));

            when(project.getInjectedProfileIds()).thenReturn(profiles);

            rule.setProfiles("profile-Z,profile-X");
            rule.setAll(true);

            rule.execute();
            // intentionally no assertTrue(..)
        });
        // intentionally no assertTrue(..)
    }

    @Test
    void testOneActiveProfilesWithTwoRequiredProfiles() {
        assertThrows(EnforcerRuleException.class, () -> {
            Map<String, List<String>> profiles = Collections.singletonMap("pom", Arrays.asList("profile-X"));

            when(project.getInjectedProfileIds()).thenReturn(profiles);

            rule.setProfiles("profile-X,profile-Y");
            rule.setAll(true);

            rule.execute();
            // intentionally no assertTrue(..)
        });
        // intentionally no assertTrue(..)
    }

    @Test
    void testOneActiveProfileWithTwoProfilesButNotAll() throws EnforcerRuleException {
        Map<String, List<String>> profiles = Collections.singletonMap("pom", Arrays.asList("profile-X"));

        when(project.getInjectedProfileIds()).thenReturn(profiles);

        rule.setProfiles("profile-X,profile-Y");
        rule.setAll(false);

        rule.execute();
        // intentionally no assertTrue(..)
    }
}
