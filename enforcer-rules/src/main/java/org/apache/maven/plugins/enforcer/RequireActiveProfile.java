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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that some profiles are active.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class RequireActiveProfile
    extends AbstractNonCacheableEnforcerRule
{

    /** Comma separated list of profiles to check. */
    private String profiles = null;

    /** If all profiles must be active. If false, only one must be active */
    private boolean all = true;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
     */
    public void execute( EnforcerRuleHelper theHelper )
        throws EnforcerRuleException
    {
        List<String> missingProfiles = new ArrayList<String>();
        try
        {
            MavenProject project = (MavenProject) theHelper.evaluate( "${project}" );
            if ( StringUtils.isNotEmpty( profiles ) )
            {
                String[] profs = profiles.split( "," );
                for ( String profile : profs )
                {
                    if ( !isProfileActive( project, profile ) )
                    {
                        missingProfiles.add( profile );
                    }
                }

                boolean fail = false;
                if ( !missingProfiles.isEmpty() )
                {
                    fail = true;
                    // if (all && missingProfiles.size() != profs.length)
                    // {
                    // fail = true;
                    // }
                    // else
                    // {
                    // if (!all && missingProfiles.size() >= (profs.length -1))
                    // {
                    // fail = true;
                    // }
                    // }
                }

                if ( fail )
                {
                    String message = getMessage();
                    StringBuilder buf = new StringBuilder();
                    if ( message != null )
                    {
                        buf.append( message + "\n" );
                    }

                    for ( String profile : missingProfiles )
                    {
                        buf.append( "Profile \"" + profile + "\" is not activated.\n" );
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
     * @param project the project
     * @param profileName the profile name
     * @return <code>true</code> if profile is active, otherwise <code>false</code>
     */
    protected boolean isProfileActive( MavenProject project, String profileName )
    {
        @SuppressWarnings( "unchecked" )
        List<Profile> activeProfiles = project.getActiveProfiles();
        if ( activeProfiles != null && !activeProfiles.isEmpty() )
        {
            for ( Profile profile : activeProfiles )
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
