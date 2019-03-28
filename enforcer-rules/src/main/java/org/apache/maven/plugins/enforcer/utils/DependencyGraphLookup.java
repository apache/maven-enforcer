package org.apache.maven.plugins.enforcer.utils;

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

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.concurrent.ExecutionException;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Collects a dependency graph, and caches it for reuse by any rules in the same project.
 */
@Named
@Singleton
public class DependencyGraphLookup
{
    private ProjectBuildingRequest sessionRequest;
    private DependencyGraphBuilder dependencyGraphBuilder;
    private LoadingCache<MavenProject, DependencyNode> projectDependencyGraphCache = CacheBuilder.newBuilder()
            .maximumSize( 128 )
            .build( new CacheLoader<MavenProject, DependencyNode>()
            {
                @Override
                public DependencyNode load( MavenProject project ) throws Exception
                {
                    return getDependencyGraph( project );
                }
            } );

    private synchronized void init( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        if ( dependencyGraphBuilder == null )
        {
            try
            {
                MavenSession session = helper.getComponent( MavenSession.class );
                this.sessionRequest = session.getProjectBuildingRequest();
            }
            catch ( ComponentLookupException e )
            {
                throw new EnforcerRuleException( "Unable to load MavenSession: ", e );
            }
            try
            {
                this.dependencyGraphBuilder = helper.getComponent( DependencyGraphBuilder.class );
            }
            catch ( ComponentLookupException e )
            {
                throw new EnforcerRuleException( "Unable to load DependencyGraphBuilder: ", e );
            }
        }
    }

    public synchronized DependencyNode getDependencyGraph( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        init( helper );
        MavenProject project;
        try
        {
            project = ( MavenProject ) helper.evaluate( "${project}" );
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to load MavenProject: ", e );
        }
        try
        {
            return projectDependencyGraphCache.get( project );
        }
        catch ( ExecutionException e )
        {
            Throwables.propagateIfInstanceOf( e.getCause(), EnforcerRuleException.class );
            throw new EnforcerRuleException( "Unable to build DependencyNode graph: ", e.getCause() );
        }
    }

    private DependencyNode getDependencyGraph( MavenProject project ) throws EnforcerRuleException
    {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest( sessionRequest );
        buildingRequest.setProject( project );
        try
        {
            return dependencyGraphBuilder.buildDependencyGraph( buildingRequest, null );
        }
        catch ( DependencyGraphBuilderException e )
        {
            throw new EnforcerRuleException( "Unable to build DependencyNode graph: ", e );
        }
    }
}
