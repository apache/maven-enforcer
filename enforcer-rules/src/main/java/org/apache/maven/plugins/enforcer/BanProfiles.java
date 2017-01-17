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
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This rule checks that one or more profiles are not active.
 */
public class BanProfiles
        extends AbstractNonCacheableEnforcerRule
{

    /**
     * Comma separated list of profiles to check.
     *
     * @see #setProfiles(String)
     * @see #getProfiles()
     */
    private String profiles;

    public final String getProfiles()
    {
        return profiles;
    }

    public final void setProfiles( final String profiles )
    {
        this.profiles = profiles;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
     */
    public void execute( final EnforcerRuleHelper theHelper )
            throws EnforcerRuleException
    {
        final List<String> bannedActiveProfiles = new ArrayList<String>();
        try
        {
            final MavenProject project = (MavenProject) theHelper.evaluate( "${project}" ) ;
            if ( StringUtils.isNotEmpty( profiles ) )
            {
                final String[] profs = profiles.split( "," );
                for ( final String profile : profs )
                {
                    if ( isProfileActive( project, profile ) )
                    {
                        bannedActiveProfiles.add( profile );
                    }
                }

                if ( !bannedActiveProfiles.isEmpty() )
                {
                    final String message = getMessage();
                    final StringBuilder buf = new StringBuilder();
                    if ( message != null )
                    {
                        buf.append( message ).append( '\n' );
                    }

                    for ( final String profile : bannedActiveProfiles )
                    {
                        buf.append( "Profile \"" ).append( profile ).append( "\" is active.\n" );
                    }

                    throw new EnforcerRuleException( buf.toString() );
                }
            }
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to retrieve the project.", e );
        }
    }

    /**
     * Checks if profile is active.
     *
     * @param project     the project
     * @param profileName the profile name
     * @return <code>true</code> if profile is active, otherwise <code>false</code>
     */
    private boolean isProfileActive( final MavenProject project, final String profileName )
    {
        @SuppressWarnings( "unchecked" )
        final List<Profile> activeProfiles = project.getActiveProfiles();
        if ( activeProfiles != null && !activeProfiles.isEmpty() )
        {
            for ( final Profile profile : activeProfiles )
            {
                if ( profile.getId().equals( profileName ) )
                {
                    return true;
                }
            }
        }

        return false;
    }
}
