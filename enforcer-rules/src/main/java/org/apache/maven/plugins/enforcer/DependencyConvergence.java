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
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.DependencyGraphLookup;
import org.apache.maven.plugins.enforcer.utils.DependencyVersionMap;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author <a href="mailto:rex@e-hoffman.org">Rex Hoffman</a>
 */
public class DependencyConvergence
    implements EnforcerRule
{

    private static Log log;

    private boolean uniqueVersions;

    private transient DependencyGraphLookup graphLookup;

    public void setUniqueVersions( boolean uniqueVersions )
    {
        this.uniqueVersions = uniqueVersions;
    }

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        if ( log == null )
        {
            log = helper.getLog();
        }
        try
        {
            graphLookup = helper.getComponent( DependencyGraphLookup.class );
        }
        catch ( ComponentLookupException e )
        {
            throw new EnforcerRuleException( "Unable to lookup DependencyGraphLookup: ", e );
        }
        try
        {
            DependencyNode node = graphLookup.getDependencyGraph( helper );
            DependencyVersionMap visitor = new DependencyVersionMap( log );
            visitor.setUniqueVersions( uniqueVersions );
            node.accept( visitor );
            List<CharSequence> errorMsgs = new ArrayList<CharSequence>();
            errorMsgs.addAll( getConvergenceErrorMsgs( visitor.getConflictedVersionNumbers() ) );
            for ( CharSequence errorMsg : errorMsgs )
            {
                log.warn( errorMsg );
            }
            if ( errorMsgs.size() > 0 )
            {
                throw new EnforcerRuleException( "Failed while enforcing releasability. "
                    + "See above detailed error message." );
            }
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
            builder.append( System.lineSeparator() );
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
        builder.append( System.lineSeparator() + "Dependency convergence error for "
            + getFullArtifactName( nodeList.get( 0 ).getArtifact() )
            + " paths to dependency are:" + System.lineSeparator() );
        if ( nodeList.size() > 0 )
        {
            builder.append( buildTreeString( nodeList.get( 0 ) ) );
        }
        for ( DependencyNode node : nodeList.subList( 1, nodeList.size() ) )
        {
            builder.append( "and" + System.lineSeparator() );
            builder.append( buildTreeString( node ) );
        }
        return builder.toString();
    }

    @Override
    public String getCacheId()
    {
        return "";
    }

    @Override
    public boolean isCacheable()
    {
        return false;
    }

    @Override
    public boolean isResultValid( EnforcerRule rule )
    {
        return false;
    }
}
