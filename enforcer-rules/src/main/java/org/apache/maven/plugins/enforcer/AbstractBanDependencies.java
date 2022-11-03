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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.ArtifactUtils;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.HashSet;
import java.util.Set;

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

    private transient DependencyGraphBuilder graphBuilder;

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        MavenProject project;
        try
        {
            project = (MavenProject) helper.evaluate( "${project}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenProject: ", eee );
        }

        MavenSession session;
        try
        {
            session = (MavenSession) helper.evaluate( "${session}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve the reactor MavenProject: ", eee );
        }

        try
        {
            graphBuilder = helper.getComponent( DependencyGraphBuilder.class );
        }
        catch ( ComponentLookupException e )
        {
            throw new EnforcerRuleException( "Unable to lookup DependencyGraphBuilder: ", e );
        }
        
        ProjectBuildingRequest buildingRequest =
            new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );
        buildingRequest.setProject( project );

        // get the correct list of dependencies
        Set<Artifact> dependencies = getDependenciesToCheck( helper, buildingRequest );

        // look for banned dependencies
        Set<Artifact> foundExcludes = checkDependencies( dependencies, helper.getLog() );

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
            }
            message = buf.toString() + "Use 'mvn dependency:tree' to locate the source of the banned dependencies.";

            throw new EnforcerRuleException( message );
        }

    }

    protected CharSequence getErrorMessage( Artifact artifact )
    {
        return "Found Banned Dependency: " + artifact.getId() + System.lineSeparator();
    }

    private Set<Artifact> getDependenciesToCheck( EnforcerRuleHelper helper,
            ProjectBuildingRequest buildingRequest )
    {
        String cacheKey = buildingRequest.getProject().getId() + "_" + searchTransitive;

        // check in the cache
        Set<Artifact> dependencies =
                (Set<Artifact>) helper.getCache( cacheKey, () -> getDependenciesToCheck( buildingRequest ) );

        return dependencies;
    }

    protected Set<Artifact> getDependenciesToCheck( ProjectBuildingRequest buildingRequest )
    {
        Set<Artifact> dependencies = null;
        try
        {
            DependencyNode node = graphBuilder.buildDependencyGraph( buildingRequest, null );
            if ( searchTransitive )
            {
                dependencies = ArtifactUtils.getAllDescendants( node );
            }
            else if ( node.getChildren() != null )
            {
                dependencies = new HashSet<>();
                for ( DependencyNode depNode : node.getChildren() )
                {
                    dependencies.add( depNode.getArtifact() );
                }
            }
        }
        catch ( DependencyGraphBuilderException e )
        {
            // otherwise we need to change the signature of this protected method
            throw new RuntimeException( e );
        }
        return dependencies;
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
