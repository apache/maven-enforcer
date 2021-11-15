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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.enforcer.utils.MockEnforcerExpressionEvaluator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.internal.DefaultDependencyNode;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.mockito.Mockito;

/**
 * The Class EnforcerTestUtils.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public final class EnforcerTestUtils
{
    /**
     * Gets the maven session.
     *
     * @return the maven session
     */
    public static MavenSession getMavenSession()
    {
        PlexusContainer mock = mock( PlexusContainer.class );

        MavenExecutionRequest mer = mock( MavenExecutionRequest.class );
        ProjectBuildingRequest buildingRequest = mock( ProjectBuildingRequest.class );
        when( buildingRequest.setRepositorySession( any() ) ).thenReturn( buildingRequest );
        when( mer.getProjectBuildingRequest() ).thenReturn( buildingRequest );

        Properties systemProperties = new Properties();
        systemProperties.put( "maven.version", "3.0" );
        when( mer.getUserProperties() ).thenReturn( new Properties() );
        when( mer.getSystemProperties() ).thenReturn( systemProperties );

        MavenExecutionResult meresult = mock( MavenExecutionResult.class );
        return new MavenSession( mock, null, mer, meresult );
    }

    /**
     * Gets the helper.
     *
     * @return the helper
     */
    public static EnforcerRuleHelper getHelper()
    {
        return getHelper( new MockProject(), false );
    }

    /**
     * Gets the helper.
     *
     * @param mockExpression the mock expression
     * @return the helper
     */
    public static EnforcerRuleHelper getHelper( boolean mockExpression )
    {
        return getHelper( new MockProject(), mockExpression );
    }

    /**
     * Gets the helper.
     *
     * @param project the project
     * @return the helper
     */
    public static EnforcerRuleHelper getHelper( MavenProject project )
    {
        return getHelper( project, false );
    }

    /**
     * Gets the helper.
     *
     * @param project the project
     * @param mockExpression the mock expression
     * @return the helper
     */
    public static EnforcerRuleHelper getHelper( MavenProject project, boolean mockExpression )
    {
        MavenSession session = getMavenSession();
        ExpressionEvaluator eval;
        if ( mockExpression )
        {
            eval = new MockEnforcerExpressionEvaluator( session );
        }
        else
        {
            MojoExecution mockExecution = mock( MojoExecution.class );
            session.setCurrentProject( project );
            eval = new PluginParameterExpressionEvaluator( session, mockExecution );
        }
        PlexusContainer container = Mockito.mock( PlexusContainer.class );

        Artifact artifact =
            new DefaultArtifact( "groupId", "artifactId", "version", "compile", "jar", "classifier", null );
        Artifact v1 = new DefaultArtifact( "groupId", "artifact", "1.0.0", "compile", "jar", "", null );
        Artifact v2 = new DefaultArtifact( "groupId", "artifact", "2.0.0", "compile", "jar", "", null );
        final DefaultDependencyNode node = new DefaultDependencyNode( artifact );
        DefaultDependencyNode child1 = new DefaultDependencyNode( node, v1, null, null, null );
        child1.setChildren( Collections.emptyList() );
        DefaultDependencyNode child2 = new DefaultDependencyNode( node, v2, null, null, null );
        child2.setChildren( Collections.emptyList() );
        node.setChildren( Arrays.asList( child1, child2 ) );

        try
        {
            when( container.lookup( DependencyCollectorBuilder.class ) )
                    .thenReturn( ( buildingRequest, filter ) -> node );
        }
        catch ( ComponentLookupException e )
        {
            // test will fail
        }
        return new DefaultEnforcementRuleHelper( session, eval, new SystemStreamLog(), container );
    }

    /**
     * Gets the helper.
     *
     * @param project the project
     * @param eval the expression evaluator to use
     * @return the helper
     */
    public static EnforcerRuleHelper getHelper( MavenProject project, ExpressionEvaluator eval )
    {
        MavenSession session = getMavenSession();
        return new DefaultEnforcementRuleHelper( session, eval, new SystemStreamLog(), null );
    }

    /**
     * New plugin.
     *
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @return the plugin
     */
    public static Plugin newPlugin( String groupId, String artifactId, String version )
    {
        InputSource inputSource = new InputSource();
        inputSource.setModelId( "unit" );

        Plugin plugin = new Plugin();
        plugin.setArtifactId( artifactId );
        plugin.setGroupId( groupId );
        plugin.setVersion( version );
        plugin.setLocation( "version", new InputLocation( 0, 0, inputSource ) );
        return plugin;
    }
}
