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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Check the profile rule.
 *
 * @author <a href="mailto:khmarbaise@apache.org">Karl Heinz Marbaise</a>
 */
public class RequireActiveProfileTest
{
    private MavenProject project;

    private EnforcerRuleHelper helper;

    private RequireActiveProfile rule;

    @BeforeEach
    public void before()
        throws ExpressionEvaluationException
    {
        project = mock( MavenProject.class );
        helper = mock( EnforcerRuleHelper.class );
        when( helper.evaluate( "${project}" ) ).thenReturn( project );
        rule = new RequireActiveProfile();
    }

    @Test
    public void testNoActiveProfilesInProjectAndNoProfilesExpectedToBeActivated()
        throws EnforcerRuleException
    {
        when( project.getInjectedProfileIds() ).thenReturn( Collections.emptyMap() );

        rule.execute( helper );
    }

    @Test
    public void testActiveProfileAndExpectedActiveProfile()
        throws EnforcerRuleException
    {
        Map<String, List<String>> profiles = Collections.singletonMap( "pom", Arrays.asList( "profile-2" ) );

        when( project.getInjectedProfileIds() ).thenReturn( profiles );

        rule.setProfiles( "profile-2" );

        rule.execute( helper );
    }

    @Test
    public void testNoActiveProfileButTheRuleRequestedAnActiveProfile()
    {
        assertThrows( EnforcerRuleException.class, () -> {
            when( project.getInjectedProfileIds() ).thenReturn( Collections.emptyMap() );

            rule.setProfiles( "profile-2" );

            rule.execute( helper );
            // intentionally no assertTrue(...)
        } );
        // intentionally no assertTrue(...)
    }

    @Test
    public void testNoActiveProfileButWeExpectToGetAnExceptionWithAll()
    {
        assertThrows( EnforcerRuleException.class, () -> {
            when( project.getInjectedProfileIds() ).thenReturn( Collections.emptyMap() );

            rule.setProfiles( "profile-2" );
            rule.setAll( true );

            rule.execute( helper );
            // intentionally no assertTrue(...)
        } );
        // intentionally no assertTrue(...)
    }

    @Test
    public void testTwoActiveProfilesWithOneRequiredProfile()
        throws EnforcerRuleException
    {
        Map<String, List<String>> profiles =
            Collections.singletonMap( "pom", Arrays.asList( "profile-1", "profile-2" ) );

        when( project.getInjectedProfileIds() ).thenReturn( profiles );

        rule.setProfiles( "profile-2" );

        rule.execute( helper );
    }

    @Test
    public void testTwoActiveProfilesWhereOneProfileIsRequiredToBeActivated()
        throws EnforcerRuleException
    {
        Map<String, List<String>> profiles =
            Collections.singletonMap( "pom", Arrays.asList( "profile-1", "profile-2" ) );

        when( project.getInjectedProfileIds() ).thenReturn( profiles );

        rule.setProfiles( "profile-2" );
        rule.setAll( true );

        rule.execute( helper );
    }

    @Test
    public void testTwoActiveProfilesWithTwoRequiredProfilesWhereOneOfThemIsNotPartOfTheActiveProfiles()
    {
        assertThrows( EnforcerRuleException.class, () -> {
            Map<String, List<String>> profiles =
                Collections.singletonMap( "pom", Arrays.asList( "profile-X", "profile-Y" ) );

            when( project.getInjectedProfileIds() ).thenReturn( profiles );

            rule.setProfiles( "profile-Z,profile-X" );
            rule.setAll( true );

            rule.execute( helper );
            // intentionally no assertTrue(..)
        } );
        // intentionally no assertTrue(..)
    }

    @Test
    public void testOneActiveProfilesWithTwoRequiredProfiles()
    {
        assertThrows( EnforcerRuleException.class, () -> {
            Map<String, List<String>> profiles = Collections.singletonMap( "pom", Arrays.asList( "profile-X" ) );

            when( project.getInjectedProfileIds() ).thenReturn( profiles );

            rule.setProfiles( "profile-X,profile-Y" );
            rule.setAll( true );

            rule.execute( helper );
            // intentionally no assertTrue(..)
        } );
        // intentionally no assertTrue(..)
    }

    @Test
    public void testOneActiveProfileWithTwoProfilesButNotAll()
        throws EnforcerRuleException
    {
        Map<String, List<String>> profiles = Collections.singletonMap( "pom", Arrays.asList( "profile-X" ) );

        when( project.getInjectedProfileIds() ).thenReturn( profiles );

        rule.setProfiles( "profile-X,profile-Y" );
        rule.setAll( false );

        rule.execute( helper );
        // intentionally no assertTrue(..)
    }
}
