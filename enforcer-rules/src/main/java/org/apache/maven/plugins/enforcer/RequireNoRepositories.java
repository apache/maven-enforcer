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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that this pom or its parents don't define a repository.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class RequireNoRepositories
    extends AbstractNonCacheableEnforcerRule
{
    private static final String VERSION = " version:";

    /**
     * Whether to ban non-plugin repositories. By default they are banned.
     * 
     * @see #setBanRepositories(boolean)
     */
    private boolean banRepositories = true;

    /**
     * Whether to ban plugin repositories. By default they are banned.
     * 
     * @see #setBanPluginRepositories(boolean)
     */
    private boolean banPluginRepositories = true;

    /**
     * Specify explicitly allowed non-plugin repositories. This is a list of ids.
     * 
     * @see #setAllowedRepositories(List)
     */
    private List<String> allowedRepositories = Collections.emptyList();

    /**
     * Specify explicitly allowed plugin repositories. This is a list of ids.
     * 
     * @see #setAllowedPluginRepositories(List)
     */
    private List<String> allowedPluginRepositories = Collections.emptyList();

    /**
     * Whether to allow repositories which only resolve snapshots. By default they are banned.
     * 
     * @see #setAllowSnapshotRepositories(boolean)
     */
    private boolean allowSnapshotRepositories = false;

    /**
     * Whether to allow plugin repositories which only resolve snapshots. By default they are banned.
     * 
     * @see {@link #setAllowSnapshotPluginRepositories(boolean)}
     */
    private boolean allowSnapshotPluginRepositories = false;

    public final void setBanRepositories( boolean banRepositories )
    {
        this.banRepositories = banRepositories;
    }
    
    public final void setBanPluginRepositories( boolean banPluginRepositories )
    {
        this.banPluginRepositories = banPluginRepositories;
    }
    
    public final void setAllowedRepositories( List<String> allowedRepositories )
    {
        this.allowedRepositories = allowedRepositories;
    }
    
    public final void setAllowedPluginRepositories( List<String> allowedPluginRepositories )
    {
        this.allowedPluginRepositories = allowedPluginRepositories;
    }
    
    public final void setAllowSnapshotRepositories( boolean allowSnapshotRepositories )
    {
        this.allowSnapshotRepositories = allowSnapshotRepositories;
    }
    
    public final void setAllowSnapshotPluginRepositories( boolean allowSnapshotPluginRepositories )
    {
        this.allowSnapshotPluginRepositories = allowSnapshotPluginRepositories;
    }
    
    private Log logger;

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        logger = helper.getLog();

        MavenSession session;
        try
        {
            session = (MavenSession) helper.evaluate( "${session}" );

            List<MavenProject> sortedProjects = session.getProjectDependencyGraph().getSortedProjects();

            List<Model> models = new ArrayList<Model>();
            for ( MavenProject mavenProject : sortedProjects )
            {
                logger.debug( "Scanning project: " + mavenProject.getGroupId() + ":" + mavenProject.getArtifactId()
                    + VERSION + mavenProject.getVersion() );
                models.add( mavenProject.getOriginalModel() );
            }
            
            List<Model> badModels = new ArrayList<Model>();

            StringBuilder newMsg = new StringBuilder();
            newMsg.append( "Some poms have repositories defined:\n" );

            for ( Model model : models )
            {
                if ( banRepositories )
                {
                    List<Repository> repos = model.getRepositories();
                    if ( repos != null && !repos.isEmpty() )
                    {
                        List<String> bannedRepos =
                            findBannedRepositories( repos, allowedRepositories, allowSnapshotRepositories );
                        if ( !bannedRepos.isEmpty() )
                        {
                            badModels.add( model );
                            newMsg.append(
                                model.getGroupId() + ":" + model.getArtifactId() + VERSION + model.getVersion()
                                    + " has repositories " + bannedRepos );
                        }
                    }
                }
                if ( banPluginRepositories )
                {
                    List<Repository> repos = model.getPluginRepositories();
                    if ( repos != null && !repos.isEmpty() )
                    {
                        List<String> bannedRepos =
                            findBannedRepositories( repos, allowedPluginRepositories, allowSnapshotPluginRepositories );
                        if ( !bannedRepos.isEmpty() )
                        {
                            badModels.add( model );
                            newMsg.append(
                                model.getGroupId() + ":" + model.getArtifactId() + VERSION + model.getVersion()
                                    + " has plugin repositories " + bannedRepos );
                        }
                    }
                }
            }

            // if anything was found, log it then append the
            // optional message.
            if ( !badModels.isEmpty() )
            {
                String message = getMessage();
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
    }

    /**
     * 
     * @param repos all repositories, never {@code null}
     * @param allowedRepos allowed repositories, never {@code null}
     * @param allowSnapshots 
     * @return List of banned repositoreis.
     */
    private static List<String> findBannedRepositories( List<Repository> repos, List<String> allowedRepos,
                                                        boolean allowSnapshots )
    {
        List<String> bannedRepos = new ArrayList<String>( allowedRepos.size() );
        for ( Repository r : repos )
        {
            if ( !allowedRepos.contains( r.getId() ) )
            {
                if ( !allowSnapshots || r.getReleases() == null || r.getReleases().isEnabled() )
                {
                    // if we are not allowing snapshots and this repo is enabled for releases
                    // it is banned.  We don't care whether it is enabled for snapshots
                    // if you define a repo and don't enable it for anything, then we have nothing 
                    // to worry about
                    bannedRepos.add( r.getId() );
                }
            }
        }
        return bannedRepos;
    }
}
