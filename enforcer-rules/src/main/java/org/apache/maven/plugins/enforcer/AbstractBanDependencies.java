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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.ResolverHelper;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Abstract Rule for banning dependencies.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public abstract class AbstractBanDependencies
    extends AbstractNonCacheableEnforcerRule
{

    /** Specify if transitive dependencies should be searched (default) or only look at direct dependencies. */
    private boolean searchTransitive = true;

    private transient ResolverHelper resolverHelper;

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        MavenProject project;
        try
        {
            project = (MavenProject) Objects.requireNonNull( helper.evaluate( "${project}" ), "${project} is null" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenProject: ", eee );
        }

        resolverHelper = new ResolverHelper ( helper );

        // get the correct list of dependencies
        Map<Artifact, DependencyNode> dependencies = getDependenciesToCheck( helper, project );

        // look for banned dependencies
        Set<Artifact> foundExcludes = checkDependencies( dependencies.keySet(), helper.getLog() );

        // if any are found, fail the check but list all of them
        if ( foundExcludes != null && !foundExcludes.isEmpty() )
        {
            String message = getMessage();

            StringBuilder buf = new StringBuilder();
            if ( message != null )
            {
                buf.append( message + System.lineSeparator() );
            }
            for ( Artifact artifact : foundExcludes )
            {
                buf.append( getErrorMessage( artifact ) );
                if ( dependencies.get(artifact) != null )
                {
                    // emit location information
                }
                
            }
            // TODO: better location message
            
            message = buf.toString() + "Use 'mvn dependency:tree' to locate the source of the banned dependencies.";

            throw new EnforcerRuleException( message );
        }
    }

    /**
     * The project's remote repositories to use for the resolution of either plugins or dependencies.
     * Standard implementation returns the remote repositories for dependencies.
     * 
     * @throws EnforcerRuleException 
     */
    protected List<RemoteRepository> getRemoteRepositories( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        try
        {
            return (List<RemoteRepository>) Objects.requireNonNull( helper.evaluate( "${project.remoteProjectRepositories}" ),
                    "${project.remoteProjectRepositories} is null");
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve project's remote repositories", eee );
        }
    }

    protected CharSequence getErrorMessage( Artifact artifact )
    {
        return "Found Banned Dependency: " + artifact.getId() + System.lineSeparator();
    }

    private Map<Artifact, DependencyNode> getDependenciesToCheck( EnforcerRuleHelper helper,
            MavenProject project )
    {
        String cacheKey = project.getId() + "_" + searchTransitive;

        // check in the cache
        Map<Artifact, DependencyNode> dependencies =
                (Map<Artifact, DependencyNode>) helper.getCache( cacheKey, () -> {
                    // TODO: first check deprecated method
                    try {
                        return resolverHelper.getDependencies( project, searchTransitive );
                    } catch (DependencyCollectionException e) {
                        throw new RuntimeException( e );
                    }
                    
                } );

        return dependencies;
    }

    /**
     * Rather use
     * @param project
     * @return
     * @deprecated Use {@link #getDependencyMapToCheck(MavenProject)} instead
     */
    @Deprecated
    protected Set<Artifact> getDependenciesToCheck( ProjectBuildingRequest request )
    {
        return null;
    }

    /**
     * Checks the set of dependencies against the list of excludes.
     *
     * @param dependencies the dependencies
     * @param log the log
     * @return the sets the
     * @throws EnforcerRuleException the enforcer rule exception
     */
    protected abstract Set<Artifact> checkDependencies( Set<Artifact> dependencies, Log log )
        throws EnforcerRuleException;

    /**
     * Checks if is search transitive.
     *
     * @return the searchTransitive
     */
    public boolean isSearchTransitive()
    {
        return this.searchTransitive;
    }

    /**
     * Sets the search transitive.
     *
     * @param theSearchTransitive the searchTransitive to set
     */
    public void setSearchTransitive( boolean theSearchTransitive )
    {
        this.searchTransitive = theSearchTransitive;
    }

}
