package org.apache.maven.plugins.enforcer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Check the BanProfiles rule.
 */
public class BanProfilesTest {
    private MavenProject project;

    private EnforcerRuleHelper helper;

    private BanProfiles rule;

    @Before
    public void before()
            throws ExpressionEvaluationException {
        project = mock(MavenProject.class);
        helper = mock(EnforcerRuleHelper.class);
        when(helper.evaluate("${project}")).thenReturn(project);
        rule = new BanProfiles();
    }

    @Test
    public void testNoActiveProfilesInProjectAndNoBannedProfiles()
            throws EnforcerRuleException {
        when(project.getActiveProfiles()).thenReturn(Collections.<Profile>emptyList());

        rule.execute(helper);
        assertTrue(true);
    }

    @Test
    public void testNoActiveProfileAndOneBannedProfile()
            throws EnforcerRuleException {
        when(project.getActiveProfiles()).thenReturn(Collections.<Profile>emptyList());

        rule.setProfiles("profile-1");

        rule.execute(helper);
        assertTrue(true);
    }

    @Test
    public void testOneActiveProfileAndNoBannedProfile()
            throws EnforcerRuleException {
        when(project.getActiveProfiles()).thenReturn(Collections.singletonList(
                createProfile("profile-2")
        ));

        rule.execute(helper);
        assertTrue(true);
    }

    @Test(expected = EnforcerRuleException.class)
    public void testOneActiveProfileAndSameBannedProfile()
            throws EnforcerRuleException {
        when(project.getActiveProfiles()).thenReturn(Collections.singletonList(
                createProfile("profile-2")
        ));

        rule.setProfiles("profile-2");

        rule.execute(helper);
        // intentionally no assertTrue(...)
    }

    @Test(expected = EnforcerRuleException.class)
    public void testTwoActiveProfilesAndOneSameBannedProfile()
            throws EnforcerRuleException {

        when(project.getActiveProfiles()).thenReturn(Arrays.asList(
                createProfile("profile-1"),
                createProfile("profile-2")
        ));

        rule.setProfiles("profile-2");

        rule.execute(helper);
        // intentionally no assertTrue(...)
    }

    @Test(expected = EnforcerRuleException.class)
    public void testOneActiveProfileAndTwoBannedProfilesOneSame()
            throws EnforcerRuleException {
        when(project.getActiveProfiles()).thenReturn(Collections.singletonList(
                createProfile("profile-1")
        ));

        rule.setProfiles("profile-1,profile-2");

        rule.execute(helper);
        // intentionally no assertTrue(...)
    }

    @Test(expected = EnforcerRuleException.class)
    public void testThreeActiveProfilesAndThreeBannedProfilesThreeSame()
            throws EnforcerRuleException {
        when(project.getActiveProfiles()).thenReturn(Arrays.asList(
                createProfile("profile-1"),
                createProfile("profile-2"),
                createProfile("profile-3"),
                createProfile("profile-4")
        ));

        rule.setProfiles("profile-2,profile-3,profile-4");

        rule.execute(helper);
        // intentionally no assertTrue(...)
    }

    private Profile createProfile(String profileId) {
        Profile p = new Profile();
        p.setId(profileId);
        return p;
    }

}
