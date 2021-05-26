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
import java.util.Map;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
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

    /** Comma separated list of profiles to check.
     *  
     * @see {@link #setProfiles(String)}
     * @see {@link #getProfiles()}
     */
    private String profiles = null;

    /** If all profiles must be active. If false, only one must be active
     *
     * @see {@link #setAll(boolean)}
     * @see {@link #isAll()}
     */
    private boolean all = true;
    
    public final String getProfiles()
    {
        return profiles;
    }
    
    public final void setProfiles( String profiles )
    {
        this.profiles = profiles;
    }
    
    public final boolean isAll()
    {
        return all;
    }
    
    public final void setAll( boolean all )
    {
        this.all = all;
    }

    @Override
    public void execute( EnforcerRuleHelper theHelper )
        throws EnforcerRuleException
    {
        List<String> missingProfiles = new ArrayList<>();
        try
        {
            MavenProject project = (MavenProject) theHelper.evaluate( "${project}" );
            if ( StringUtils.isNotEmpty( profiles ) )
            {
                String[] profileIds = profiles.split( "," );
                for ( String profileId : profileIds )
                {
                    if ( !isProfileActive( project, profileId ) )
                    {
                        missingProfiles.add( profileId );
                    }
                }

                boolean fail = false;
                if ( !missingProfiles.isEmpty() )
                {
                    if ( all || missingProfiles.size() == profileIds.length )
                    {
                      fail = true;
                    }
                }

                if ( fail )
                {
                    String message = getMessage();
                    StringBuilder buf = new StringBuilder();
                    if ( message != null )
                    {
                        buf.append( message + System.lineSeparator() );
                    }

                    for ( String profile : missingProfiles )
                    {
                        buf.append( "Profile \"" + profile + "\" is not activated." + System.lineSeparator() );
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
     * @param profileId the profile name
     * @return <code>true</code> if profile is active, otherwise <code>false</code>
     */
    protected boolean isProfileActive( MavenProject project, String profileId )
    {
        for ( Map.Entry<String, List<String>> entry : project.getInjectedProfileIds().entrySet() )
        {
            if ( entry.getValue().contains( profileId ) )
            {
                return true;
            }
        }
        return false;
    }
}
