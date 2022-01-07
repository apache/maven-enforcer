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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.enforcer.utils.ArtifactUtils;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author Robert Scholte
 * @since 1.3
 */
public class RequireSameVersions
    extends AbstractNonCacheableEnforcerRule
{
    private boolean uniqueVersions;

    private boolean searchTransitiveDependencies;

    private Set<String> dependencies = new HashSet<>();

    private Set<String> plugins = new HashSet<>();

    private Set<String> buildPlugins = new HashSet<>();

    private Set<String> reportPlugins = new HashSet<>();

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        // get the project
        MavenProject project;
        try
        {
            project = (MavenProject) helper.evaluate( "${project}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenProject: ", eee );
        }

        DependencyNode node = getNode( helper );

        Set<Artifact> artifacts = searchTransitiveDependencies
            ? ArtifactUtils.getAllDescendants( node )
            : project.getArtifacts();

        // consider including profile based artifacts
        Map<String, List<String>> versionMembers = new LinkedHashMap<>();

        Set<String> buildPluginSet = new HashSet<>( buildPlugins );
        buildPluginSet.addAll( plugins );
        Set<String> reportPluginSet = new HashSet<>( reportPlugins );
        reportPluginSet.addAll( plugins );

        // CHECKSTYLE_OFF: LineLength
        versionMembers.putAll( collectVersionMembers( artifacts, dependencies, " (dependency)" ) );
        versionMembers.putAll( collectVersionMembers( project.getPluginArtifacts(), buildPlugins, " (buildPlugin)" ) );
        versionMembers.putAll( collectVersionMembers( project.getReportArtifacts(), reportPlugins, " (reportPlugin)" ) );
        // CHECKSTYLE_ON: LineLength

        if ( versionMembers.size() > 1 )
        {
            StringBuilder builder = new StringBuilder( "Found entries with different versions"
                + System.lineSeparator() );
            for ( Map.Entry<String, List<String>> entry : versionMembers.entrySet() )
            {
                builder.append( "Entries with version " ).append( entry.getKey() ).append( System.lineSeparator() );
                for ( String conflictId : entry.getValue() )
                {
                    builder.append( "- " ).append( conflictId ).append( System.lineSeparator() );
                }
            }
            throw new EnforcerRuleException( builder.toString() );
        }
    }

    private Map<String, List<String>> collectVersionMembers( Set<Artifact> artifacts, Collection<String> patterns,
                                                             String source )
    {
        Map<String, List<String>> versionMembers = new LinkedHashMap<>();

        List<Pattern> regExs = new ArrayList<>();
        for ( String pattern : patterns )
        {
            String regex = pattern.replace( ".", "\\." ).replace( "*", ".*" ).replace( ":", "\\:" ).replace( '?', '.' );

            // pattern is groupId[:artifactId[:type[:classifier]]]
            regExs.add( Pattern.compile( regex + "(\\:.+)?" ) );
        }

        for ( Artifact artifact : artifacts )
        {
            for ( Pattern regEx : regExs )
            {
                if ( regEx.matcher( artifact.getDependencyConflictId() ).matches() )
                {
                    String version = uniqueVersions ? artifact.getVersion() : artifact.getBaseVersion();
                    if ( !versionMembers.containsKey( version ) )
                    {
                        versionMembers.put( version, new ArrayList<>() );
                    }
                    versionMembers.get( version ).add( artifact.getDependencyConflictId() + source );
                }
            }
        }
        return versionMembers;
    }

    private DependencyNode getNode( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        try
        {
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            MavenSession session = (MavenSession) helper.evaluate( "${session}" );
            DependencyCollectorBuilder dependencyCollectorBuilder =
                helper.getComponent( DependencyCollectorBuilder.class );
            ArtifactRepository repository = (ArtifactRepository) helper.evaluate( "${localRepository}" );

            ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );
            buildingRequest.setProject( project );
            buildingRequest.setLocalRepository( repository );

            return dependencyCollectorBuilder.collectDependencyGraph( buildingRequest, null );
        }
        catch ( ExpressionEvaluationException | ComponentLookupException e )
        {
            throw new EnforcerRuleException( "Unable to lookup a component " + e.getLocalizedMessage(), e );
        }
        catch ( DependencyCollectorBuilderException e )
        {
            throw new EnforcerRuleException( "Could not build dependency tree " + e.getLocalizedMessage(), e );
        }
    }
}
