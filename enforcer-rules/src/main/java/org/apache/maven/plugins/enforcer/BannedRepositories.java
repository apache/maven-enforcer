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
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that this project's maven session whether have banned repositories.
 * 
 * @author <a href="mailto:wangyf2010@gmail.com">Simon Wang</a>
 */
public class BannedRepositories
    extends AbstractNonCacheableEnforcerRule
{

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Specify explicitly banned non-plugin repositories. This is a list of repository url patterns. Support wildcard
     * "*".
     * 
     * @see {@link #setBannedRepositories(List)}
     */
    private List<String> bannedRepositories = Collections.emptyList();

    /**
     * Specify explicitly banned plugin repositories. This is a list of repository url patterns. Support wildcard "*".
     * 
     * @see {@link #setBannedPluginRepositories(List)}
     */
    private List<String> bannedPluginRepositories = Collections.emptyList();

    /**
     * Specify explicitly allowed non-plugin repositories, then all others repositories would be banned. This is a list
     * of repository url patterns. Support wildcard "*".
     * 
     * @see {@link #setAllowedRepositories(List)}
     */
    private List<String> allowedRepositories = Collections.emptyList();

    /**
     * Specify explicitly allowed plugin repositories, then all others repositories would be banned. This is a list of
     * repository url patterns. Support wildcard "*".
     * 
     * @see {@link #setAllowedPluginRepositories(List)}
     */
    private List<String> allowedPluginRepositories = Collections.emptyList();

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        MavenProject project;
        try
        {
            project = (MavenProject) helper.evaluate( "${project}" );

            List<ArtifactRepository> resultBannedRepos =
                checkRepositories( project.getRemoteArtifactRepositories(), this.allowedRepositories,
                                   this.bannedRepositories );

            List<ArtifactRepository> resultBannedPluginRepos =
                checkRepositories( project.getPluginArtifactRepositories(), this.allowedPluginRepositories,
                                   this.bannedPluginRepositories );

            String repoErrMsg = populateErrorMessage( resultBannedRepos, " " );
            String pluginRepoErrMsg = populateErrorMessage( resultBannedPluginRepos, " plugin " );

            String errMsg = repoErrMsg + pluginRepoErrMsg;

            if ( errMsg != null && !StringUtils.isEmpty( errMsg.toString() ) )
            {
                throw new EnforcerRuleException( errMsg.toString() );
            }

        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    protected void setBannedRepositories( List<String> bannedRepositories )
    {
        this.bannedRepositories = bannedRepositories;
    }

    protected void setBannedPluginRepositories( List<String> bannedPluginRepositories )
    {
        this.bannedPluginRepositories = bannedPluginRepositories;
    }

    protected void setAllowedRepositories( List<String> allowedRepositories )
    {
        this.allowedRepositories = allowedRepositories;
    }

    protected void setAllowedPluginRepositories( List<String> allowedPluginRepositories )
    {
        this.allowedPluginRepositories = allowedPluginRepositories;
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Check whether specified repositories have banned repositories.
     * 
     * @param repositories: candidate repositories.
     * @param includes : 'include' patterns.
     * @param excludes : 'exclude' patterns.
     * @return Banned repositories.
     */
    private List<ArtifactRepository> checkRepositories( List<ArtifactRepository> repositories, List<String> includes,
                                                        List<String> excludes )
    {
        List<ArtifactRepository> bannedRepos = new ArrayList<ArtifactRepository>();

        for ( ArtifactRepository repo : repositories )
        {
            String url = repo.getUrl().trim();
            if ( includes.size() > 0 && !match( url, includes ) )
            {
                bannedRepos.add( repo );
                continue;
            }

            if ( excludes.size() > 0 && match( url, excludes ) )
            {
                bannedRepos.add( repo );
            }

        }

        return bannedRepos;
    }

    private boolean match( String url, List<String> patterns )
    {
        for ( String pattern : patterns )
        {
            if ( this.match( url, pattern ) )
            {
                return true;
            }
        }

        return false;
    }

    private boolean match( String text, String pattern )
    {
        return text.matches( pattern.replace( "?", ".?" ).replace( "*", ".*?" ) );
    }

    private String populateErrorMessage( List<ArtifactRepository> resultBannedRepos, String errorMessagePrefix )
    {
        StringBuffer errMsg = new StringBuffer( "" );
        if ( !resultBannedRepos.isEmpty() )
        {
            errMsg.append( "Current maven session contains banned" + errorMessagePrefix
                + "repository urls, please double check your pom or settings.xml:\n"
                + getRepositoryUrlString( resultBannedRepos ) + "\n\n" );
        }

        return errMsg.toString();
    }

    private String getRepositoryUrlString( List<ArtifactRepository> resultBannedRepos )
    {
        StringBuffer urls = new StringBuffer( "" );
        for ( ArtifactRepository repo : resultBannedRepos )
        {
            urls.append( repo.getId() + " - " + repo.getUrl() + "\n" );
        }
        return urls.toString();
    }

}
