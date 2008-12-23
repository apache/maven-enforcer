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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.plugins.enforcer.utils.EnforcerRuleUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * This rule checks that this pom or its parents don't define a repository.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class RequireNoRepositories
    extends AbstractNonCacheableEnforcerRule
{
    /**
     * Whether to ban non-plugin repositories. By default they are banned.
     */
    public boolean banRepositories = true;

    /**
     * Whether to ban plugin repositories. By default they are banned.
     */
    public boolean banPluginRepositories = true;

    /**
     * Specify explicitly allowed non-plugin repositories. This is a list of ids.
     */
    public List allowedRepositories = Collections.EMPTY_LIST;

    /**
     * Specify explicitly allowed plugin repositories. This is a list of ids.
     */
    public List allowedPluginRepositories = Collections.EMPTY_LIST;

    /*
     * (non-Javadoc)
     * @see
     * org.apache.maven.enforcer.rule.api.EnforcerRule#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        EnforcerRuleUtils utils = new EnforcerRuleUtils( helper );

        MavenProject project;
        try
        {
            project = (MavenProject) helper.evaluate( "${project}" );

            List models =
                utils.getModelsRecursively( project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                            new File( project.getBasedir(), "pom.xml" ) );
            List badModels = new ArrayList();

            StringBuffer newMsg = new StringBuffer();
            newMsg.append( "Some poms have repositories defined:\n" );

            for ( Iterator i = models.iterator(); i.hasNext(); )
            {
                Model model = (Model) i.next();
                if ( banRepositories )
                {
                    List repos = model.getRepositories();
                    if ( repos != null && !repos.isEmpty() )
                    {
                        List bannedRepos = findBannedRepositories( repos, allowedRepositories );
                        if ( !bannedRepos.isEmpty() )
                        {
                            badModels.add( model );
                            newMsg.append( model.getGroupId() + ":" + model.getArtifactId() + " version:"
                                + model.getVersion() + " has repositories " + bannedRepos );
                        }
                    }
                }
                if ( banPluginRepositories )
                {
                    List repos = model.getPluginRepositories();
                    if ( repos != null && !repos.isEmpty() )
                    {
                        List bannedRepos = findBannedRepositories( repos, allowedPluginRepositories );
                        if ( !bannedRepos.isEmpty() )
                        {
                            badModels.add( model );
                            newMsg.append( model.getGroupId() + ":" + model.getArtifactId() + " version:"
                                + model.getVersion() + " has plugin repositories " + bannedRepos );
                        }
                    }
                }
            }

            // if anything was found, log it then append the
            // optional message.
            if ( !badModels.isEmpty() )
            {
                if ( StringUtils.isNotEmpty( message ) )
                {
                    newMsg.append( message );
                }

                throw new EnforcerRuleException( newMsg.toString() );
            }

        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( IOException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( XmlPullParserException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
    }

    private static List findBannedRepositories( List repos, List allowedRepos )
    {
        List bannedRepos = new ArrayList( allowedRepos.size() );
        for ( Iterator i = repos.iterator(); i.hasNext(); )
        {
            Repository r = (Repository) i.next();
            if ( !allowedRepos.contains( r.getId() ) )
            {
                bannedRepos.add( r.getId() );
            }
        }
        return bannedRepos;
    }
}
