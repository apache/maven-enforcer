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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.DependencyVersionMap;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.i18n.I18N;

/**
 * @author <a href="mailto:rex@e-hoffman.org">Rex Hoffman</a>
 */
public class DependencyConvergence
    implements EnforcerRule
{

    private static Log log;

    private static I18N i18n;
    
    private boolean uniqueVersions; 

    public void setUniqueVersions( boolean uniqueVersions )
    {
        this.uniqueVersions = uniqueVersions;
    }
    
    /**
     * Uses the {@link EnforcerRuleHelper} to populate the values of the
     * {@link DependencyTreeBuilder#buildDependencyTree(MavenProject, ArtifactRepository, ArtifactFactory, ArtifactMetadataSource, ArtifactFilter, ArtifactCollector)}
     * factory method. <br/>
     * This method simply exists to hide all the ugly lookup that the {@link EnforcerRuleHelper} has to do.
     * 
     * @param helper
     * @return a Dependency Node which is the root of the project's dependency tree
     * @throws EnforcerRuleException
     */
    private DependencyNode getNode( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        try
        {
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            DependencyTreeBuilder dependencyTreeBuilder =
                (DependencyTreeBuilder) helper.getComponent( DependencyTreeBuilder.class );
            ArtifactRepository repository = (ArtifactRepository) helper.evaluate( "${localRepository}" );
            ArtifactFactory factory = (ArtifactFactory) helper.getComponent( ArtifactFactory.class );
            ArtifactMetadataSource metadataSource =
                (ArtifactMetadataSource) helper.getComponent( ArtifactMetadataSource.class );
            ArtifactCollector collector = (ArtifactCollector) helper.getComponent( ArtifactCollector.class );
            ArtifactFilter filter = null; // we need to evaluate all scopes
            DependencyNode node =
                dependencyTreeBuilder.buildDependencyTree( project, repository, factory, metadataSource, filter,
                                                           collector );
            return node;
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to lookup an expression " + e.getLocalizedMessage(), e );
        }
        catch ( ComponentLookupException e )
        {
            throw new EnforcerRuleException( "Unable to lookup a component " + e.getLocalizedMessage(), e );
        }
        catch ( DependencyTreeBuilderException e )
        {
            throw new EnforcerRuleException( "Could not build dependency tree " + e.getLocalizedMessage(), e );
        }
    }

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        if ( log == null )
        {
            log = helper.getLog();
        }
        try
        {
            if ( i18n == null )
            {
                i18n = (I18N) helper.getComponent( I18N.class );
            }
            DependencyNode node = getNode( helper );
            DependencyVersionMap visitor = new DependencyVersionMap( log );
            visitor.setUniqueVersions( uniqueVersions );
            node.accept( visitor );
            List<CharSequence> errorMsgs = new ArrayList<CharSequence>();
            errorMsgs.addAll( getConvergenceErrorMsgs( visitor.getConflictedVersionNumbers() ) );
            for ( CharSequence errorMsg : errorMsgs )
            {
                log.error( errorMsg );
            }
            if ( errorMsgs.size() > 0 )
            {
                throw new EnforcerRuleException( "Failed while enforcing releasability the error(s) are " + errorMsgs );
            }
        }
        catch ( ComponentLookupException e )
        {
            throw new EnforcerRuleException( "Unable to lookup a component " + e.getLocalizedMessage(), e );
        }
        catch ( Exception e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage(), e );
        }
    }

    private String getFullArtifactName( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    private StringBuilder buildTreeString( DependencyNode node )
    {
        List<String> loc = new ArrayList<String>();
        DependencyNode currentNode = node;
        while ( currentNode != null )
        {
            loc.add( getFullArtifactName( currentNode.getArtifact() ) );
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
            builder.append( "+-" + loc.get( i ) );
            builder.append( "\n" );
        }
        return builder;
    }

    private List<String> getConvergenceErrorMsgs( List<List<DependencyNode>> errors )
    {
        List<String> errorMsgs = new ArrayList<String>();
        for ( List<DependencyNode> nodeList : errors )
        {
            errorMsgs.add( buildConvergenceErrorMsg( nodeList ) );
        }
        return errorMsgs;
    }

    private String buildConvergenceErrorMsg( List<DependencyNode> nodeList )
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "\nDependency convergence error for " + getFullArtifactName( nodeList.get( 0 ).getArtifact() )
            + " paths to dependency are:\n" );
        if ( nodeList.size() > 0 )
        {
            builder.append( buildTreeString( nodeList.get( 0 ) ) );
        }
        for ( DependencyNode node : nodeList.subList( 1, nodeList.size() ) )
        {
            builder.append( "and\n" );
            builder.append( buildTreeString( node ) );
        }
        return builder.toString();
    }

    /**
     * If your rule is cacheable, you must return a unique id when parameters or conditions change that would cause the
     * result to be different. Multiple cached results are stored based on their id. The easiest way to do this is to
     * return a hash computed from the values of your parameters. If your rule is not cacheable, then the result here is
     * not important, you may return anything.
     */
    public String getCacheId()
    {
        return "";
    }

    /**
     * This tells the system if the results are cacheable at all. Keep in mind that during forked builds and other
     * things, a given rule may be executed more than once for the same project. This means that even things that change
     * from project to project may still be cacheable in certain instances.
     */
    public boolean isCacheable()
    {
        return false;
    }

    /**
     * If the rule is cacheable and the same id is found in the cache, the stored results are passed to this method to
     * allow double checking of the results. Most of the time this can be done by generating unique ids, but sometimes
     * the results of objects returned by the helper need to be queried. You may for example, store certain objects in
     * your rule and then query them later.
     */
    public boolean isResultValid( EnforcerRule arg0 )
    {
        return false;
    }
}