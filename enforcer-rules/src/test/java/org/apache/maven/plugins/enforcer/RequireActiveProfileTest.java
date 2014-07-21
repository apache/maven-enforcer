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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.junit.Before;
import org.junit.Test;

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

    @Before
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
        when( project.getActiveProfiles() ).thenReturn( Collections.<Profile> emptyList() );

        rule.execute( helper );

        assertTrue( true );
    }

    @Test
    public void testActiveProfileAndExpectedActiveProfile()
        throws EnforcerRuleException
    {
        List<Profile> profiles = Collections.<Profile> singletonList( createProfile( "profile-2" ) );

        when( project.getActiveProfiles() ).thenReturn( profiles );

        rule.setProfiles( "profile-2" );

        rule.execute( helper );
        assertTrue( true );
    }

    @Test( expected = EnforcerRuleException.class )
    public void testNoActiveProfileButTheRuleRequestedAnActiveProfile()
        throws EnforcerRuleException
    {
        when( project.getActiveProfiles() ).thenReturn( Collections.<Profile> emptyList() );

        rule.setProfiles( "profile-2" );

        rule.execute( helper );
        // intentionally no assertTrue(...)
    }

    @Test( expected = EnforcerRuleException.class )
    public void testNoActiveProfileButWeExpectToGetAnExceptionWithAll()
        throws EnforcerRuleException
    {
        when( project.getActiveProfiles() ).thenReturn( Collections.<Profile> emptyList() );

        rule.setProfiles( "profile-2" );
        rule.setAll( true );

        rule.execute( helper );
        // intentionally no assertTrue(...)
    }

    @Test
    public void testTwoActiveProfilesWithOneRequiredProfile()
        throws EnforcerRuleException
    {
        List<Profile> profiles = Arrays.asList( createProfile( "profile-1" ), createProfile( "profile-2" ) );

        when( project.getActiveProfiles() ).thenReturn( profiles );

        rule.setProfiles( "profile-2" );

        rule.execute( helper );
        assertTrue( true );
    }

    @Test
    public void testTwoActiveProfilesWhereOneProfileIsRequiredToBeActivated()
        throws EnforcerRuleException
    {
        List<Profile> profiles = Arrays.asList( createProfile( "profile-1" ), createProfile( "profile-2" ) );

        when( project.getActiveProfiles() ).thenReturn( profiles );

        rule.setProfiles( "profile-2" );
        rule.setAll( true );

        rule.execute( helper );
        assertTrue( true );
    }

    @Test( expected = EnforcerRuleException.class )
    public void testTwoActiveProfilesWithTwoRequiredProfilesWhereOneOfThemIsNotPartOfTheActiveProfiles()
        throws EnforcerRuleException, ExpressionEvaluationException
    {

        List<Profile> profiles = Arrays.asList( createProfile( "profile-X" ), createProfile( "profile-Y" ) );

        when( project.getActiveProfiles() ).thenReturn( profiles );

        rule.setProfiles( "profile-Z,profile-X" );
        rule.setAll( true );

        rule.execute( helper );
        // intentionally no assertTrue(..)
    }

    @Test( expected = EnforcerRuleException.class )
    public void testOneActiveProfilesWithTwoRequiredProfiles()
        throws EnforcerRuleException, ExpressionEvaluationException
    {
        List<Profile> profiles = Collections.singletonList( createProfile( "profile-X" ) );

        when( project.getActiveProfiles() ).thenReturn( profiles );

        rule.setProfiles( "profile-X,profile-Y" );
        rule.setAll( true );

        rule.execute( helper );
        // intentionally no assertTrue(..)
    }

    @Test
    public void testOneActiveProfileWithTwoProfilesButNotAll()
        throws EnforcerRuleException, ExpressionEvaluationException
    {
        List<Profile> profiles = Collections.singletonList( createProfile( "profile-X" ) );

        when( project.getActiveProfiles() ).thenReturn( profiles );

        rule.setProfiles( "profile-X,profile-Y" );
        rule.setAll( false );

        rule.execute( helper );
        // intentionally no assertTrue(..)
    }

    private Profile createProfile( String profileId )
    {
        Profile p = new Profile();
        p.setId( profileId );
        return p;
    }

}
