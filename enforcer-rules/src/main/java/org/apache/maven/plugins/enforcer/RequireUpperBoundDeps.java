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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Rule to enforce that the resolved dependency is also the most recent one of all transitive dependencies.
 *
 * @author Geoffrey De Smet
 * @since 1.1
 */
public class RequireUpperBoundDeps
    extends AbstractNonCacheableEnforcerRule
{
    private static Log log;

    /**
     * @since 1.3
     */
    private boolean uniqueVersions;

    /**
     * Dependencies to ignore.
     *
     * @since TBD
     */
    private List<String> excludes = null;

    /**
     * Dependencies to include.
     *
     * @since 3.0.0
     */
    private List<String> includes = null;

    /**
     * Set to {@code true} if timestamped snapshots should be used.
     *
     * @param uniqueVersions
     * @since 1.3
     */
    public void setUniqueVersions( boolean uniqueVersions )
    {
        this.uniqueVersions = uniqueVersions;
    }

    /**
     * Sets dependencies to exclude.
     * @param excludes a list of {@code groupId:artifactId} names
     */
    public void setExcludes( List<String> excludes )
    {
        this.excludes = excludes;
    }

    /**
     * Sets dependencies to include.
     *
     * @param includes a list of {@code groupId:artifactId} names
     */
    public void setIncludes( List<String> includes )
    {
        this.includes = includes;
    }

    // CHECKSTYLE_OFF: LineLength
    /**
     * Uses the {@link EnforcerRuleHelper} to populate the values of the
     * {@link DependencyTreeBuilder#buildDependencyTree(MavenProject, ArtifactRepository, ArtifactFactory, ArtifactMetadataSource, ArtifactFilter, ArtifactCollector)}
     * factory method. <br/>
     * This method simply exists to hide all the ugly lookup that the {@link EnforcerRuleHelper} has to do.
     *
     * @param helper
     * @return a Dependency Node which is the root of the project's dependency tree
     * @throws EnforcerRuleException when the build should fail
     */
    // CHECKSTYLE_ON: LineLength
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
            ArtifactFilter filter = ( Artifact a ) -> ( "compile".equalsIgnoreCase( a.getScope () )
                    || "runtime".equalsIgnoreCase( a.getScope () ) )
                    && !a.isOptional();
            
            return dependencyCollectorBuilder.collectDependencyGraph( buildingRequest, filter );
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to lookup an expression " + e.getLocalizedMessage(), e );
        }
        catch ( ComponentLookupException e )
        {
            throw new EnforcerRuleException( "Unable to lookup a component " + e.getLocalizedMessage(), e );
        }
        catch ( DependencyCollectorBuilderException e )
        {
            throw new EnforcerRuleException( "Could not build dependency tree " + e.getLocalizedMessage(), e );
        }
    }

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        if ( log == null )
        {
            log = helper.getLog();
        }
        DependencyNode node = getNode( helper );
        RequireUpperBoundDepsVisitor visitor = new RequireUpperBoundDepsVisitor();
        visitor.setUniqueVersions( uniqueVersions );
        visitor.setIncludes( includes );
        node.accept( visitor );
        List<String> errorMessages = buildErrorMessages( visitor.getConflicts() );
        if ( errorMessages.size() > 0 )
        {
            throw new EnforcerRuleException( "Failed while enforcing RequireUpperBoundDeps. The error(s) are "
                + errorMessages );
        }
    }

    private List<String> buildErrorMessages( List<List<DependencyNode>> conflicts )
    {
        List<String> errorMessages = new ArrayList<>( conflicts.size() );
        for ( List<DependencyNode> conflict : conflicts )
        {
            Artifact artifact = conflict.get( 0 ).getArtifact();
            String groupArt = artifact.getGroupId() + ":" + artifact.getArtifactId();
            if ( excludes != null && excludes.contains( groupArt ) )
            {
                log.info( "Ignoring requireUpperBoundDeps in " + groupArt );
            }
            else
            {
                errorMessages.add( buildErrorMessage( conflict ) );
            }
        }
        return errorMessages;
    }

    private String buildErrorMessage( List<DependencyNode> conflict )
    {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append(
                System.lineSeparator() + "Require upper bound dependencies error for " + getFullArtifactName(
                        conflict.get( 0 ), false ) + " paths to dependency are:" + System.lineSeparator() );
        if ( conflict.size() > 0 )
        {
            errorMessage.append( buildTreeString( conflict.get( 0 ) ) );
        }
        for ( DependencyNode node : conflict.subList( 1, conflict.size() ) )
        {
            errorMessage.append( "and" + System.lineSeparator() );
            errorMessage.append( buildTreeString( node ) );
        }
        return errorMessage.toString();
    }

    private StringBuilder buildTreeString( DependencyNode node )
    {
        List<String> loc = new ArrayList<>();
        DependencyNode currentNode = node;
        while ( currentNode != null )
        {
            StringBuilder line = new StringBuilder( getFullArtifactName( currentNode, false ) );

            if ( currentNode.getPremanagedVersion() != null )
            {
                line.append( " (managed) <-- " );
                line.append( getFullArtifactName( currentNode, true ) );
            }

            loc.add( line.toString() );
            currentNode = currentNode.getParent();
        }
        Collections.reverse( loc );
        StringBuilder builder = new StringBuilder();
        for ( int i = 0; i < loc.size(); i++ )
        {
            for ( int j = 0; j < i; j++ )
            {
                builder.append( "  " );
            }
            builder.append( "+-" ).append( loc.get( i ) );
            builder.append( System.lineSeparator() );
        }
        return builder;
    }

    private String getFullArtifactName( DependencyNode node, boolean usePremanaged )
    {
        Artifact artifact = node.getArtifact();

        String version = node.getPremanagedVersion();
        if ( !usePremanaged || version == null )
        {
            version = uniqueVersions ? artifact.getVersion() : artifact.getBaseVersion();
        }
        String result = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + version;

        String classifier = artifact.getClassifier();
        if ( classifier != null && !classifier.isEmpty() )
        {
            result += ":" + classifier;
        }
  
        String scope = artifact.getScope();      
        if ( "compile".equals( scope ) )
        {
            result = MessageUtils.buffer().strong( result ).toString();
        }
        else if ( scope != null )
        {
            result += " [" + scope + ']';
        }
        
        return result;
    }

    private static class RequireUpperBoundDepsVisitor
        implements DependencyNodeVisitor
    {

        private boolean uniqueVersions;

        private List<String> includes = null;

        public void setUniqueVersions( boolean uniqueVersions )
        {
            this.uniqueVersions = uniqueVersions;
        }

        public void setIncludes( List<String> includes )
        {
            this.includes = includes;
        }

        private Map<String, List<DependencyNodeHopCountPair>> keyToPairsMap = new LinkedHashMap<>();

        public boolean visit( DependencyNode node )
        {
            DependencyNodeHopCountPair pair = new DependencyNodeHopCountPair( node );
            String key = pair.constructKey();

            if ( includes != null && !includes.isEmpty() && !includes.contains( key ) )
            {
                return true;
            }

            List<DependencyNodeHopCountPair> pairs = keyToPairsMap.get( key );
            if ( pairs == null )
            {
                pairs = new ArrayList<>();
                keyToPairsMap.put( key, pairs );
            }
            pairs.add( pair );
            Collections.sort( pairs );
            return true;
        }

        public boolean endVisit( DependencyNode node )
        {
            return true;
        }

        public List<List<DependencyNode>> getConflicts()
        {
            List<List<DependencyNode>> output = new ArrayList<>();
            for ( List<DependencyNodeHopCountPair> pairs : keyToPairsMap.values() )
            {
                if ( containsConflicts( pairs ) )
                {
                    List<DependencyNode> outputSubList = new ArrayList<>( pairs.size() );
                    for ( DependencyNodeHopCountPair pair : pairs )
                    {
                        outputSubList.add( pair.getNode() );
                    }
                    output.add( outputSubList );
                }
            }
            return output;
        }

        private boolean containsConflicts( List<DependencyNodeHopCountPair> pairs )
        {
            DependencyNodeHopCountPair resolvedPair = pairs.get( 0 );

            // search for artifact with lowest hopCount
            for ( DependencyNodeHopCountPair hopPair : pairs.subList( 1, pairs.size() ) )
            {
                if ( hopPair.getHopCount() < resolvedPair.getHopCount() )
                {
                    resolvedPair = hopPair;
                }
            }

            ArtifactVersion resolvedVersion = resolvedPair.extractArtifactVersion( uniqueVersions, false );

            for ( DependencyNodeHopCountPair pair : pairs )
            {
                ArtifactVersion version = pair.extractArtifactVersion( uniqueVersions, true );
                if ( resolvedVersion.compareTo( version ) < 0 )
                {
                    return true;
                }
            }
            return false;
        }

    }

    private static class DependencyNodeHopCountPair
        implements Comparable<DependencyNodeHopCountPair>
    {

        private DependencyNode node;

        private int hopCount;

        private DependencyNodeHopCountPair( DependencyNode node )
        {
            this.node = node;
            countHops();
        }

        private void countHops()
        {
            hopCount = 0;
            DependencyNode parent = node.getParent();
            while ( parent != null )
            {
                hopCount++;
                parent = parent.getParent();
            }
        }

        private String constructKey()
        {
            Artifact artifact = node.getArtifact();
            return artifact.getGroupId() + ":" + artifact.getArtifactId();
        }

        public DependencyNode getNode()
        {
            return node;
        }

        private ArtifactVersion extractArtifactVersion( boolean uniqueVersions, boolean usePremanagedVersion )
        {
            if ( usePremanagedVersion && node.getPremanagedVersion() != null )
            {
                return new DefaultArtifactVersion( node.getPremanagedVersion() );
            }

            Artifact artifact = node.getArtifact();
            String version = uniqueVersions ? artifact.getVersion() : artifact.getBaseVersion();
            if ( version != null )
            {
                return new DefaultArtifactVersion( version );
            }
            try
            {
                return artifact.getSelectedVersion();
            }
            catch ( OverConstrainedVersionException e )
            {
                throw new RuntimeException( "Version ranges problem with " + node.getArtifact(), e );
            }
        }

        public int getHopCount()
        {
            return hopCount;
        }

        public int compareTo( DependencyNodeHopCountPair other )
        {
            return Integer.valueOf( hopCount ).compareTo( Integer.valueOf( other.getHopCount() ) );
        }
    }

}
