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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugins.enforcer.utils.ArtifactMatcher.Pattern;
import org.apache.maven.shared.dependency.graph.DependencyNode;

/**
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
public final class ArtifactUtils
{
    private ArtifactUtils()
    {
    }

    public static Set<Artifact> getAllDescendants( DependencyNode node )
    {
        Set<Artifact> children = null;
        if ( node.getChildren() != null )
        {
            children = new HashSet<>();
            for ( DependencyNode depNode : node.getChildren() )
            {
                children.add( depNode.getArtifact() );
                Set<Artifact> subNodes = getAllDescendants( depNode );
                if ( subNodes != null )
                {
                    children.addAll( subNodes );
                }
            }
        }
        return children;
    }

    /**
     * Checks the set of dependencies against the list of patterns.
     * 
     * @param thePatterns the patterns
     * @param dependencies the dependencies
     * @return a set containing artifacts matching one of the patterns or <code>null</code>
     * @throws EnforcerRuleException the enforcer rule exception
     */
    public static Set<Artifact> checkDependencies( Set<Artifact> dependencies, List<String> thePatterns )
        throws EnforcerRuleException
    {
        Set<Artifact> foundMatches = null;
    
        if ( thePatterns != null && thePatterns.size() > 0 )
        {
    
            for ( String pattern : thePatterns )
            {
                String[] subStrings = pattern.split( ":" );
                subStrings = StringUtils.stripAll( subStrings );
                String resultPattern = StringUtils.join( subStrings, ":" );
    
                for ( Artifact artifact : dependencies )
                {
                    if ( compareDependency( resultPattern, artifact ) )
                    {
                        // only create if needed
                        if ( foundMatches == null )
                        {
                            foundMatches = new HashSet<Artifact>();
                        }
                        foundMatches.add( artifact );
                    }
                }
            }
        }
        return foundMatches;
    }

    /**
     * Compares the given pattern against the given artifact. The pattern should follow the format
     * <code>groupId:artifactId:version:type:scope:classifier</code>.
     * 
     * @param pattern The pattern to compare the artifact with.
     * @param artifact the artifact
     * @return <code>true</code> if the artifact matches one of the patterns
     * @throws EnforcerRuleException the enforcer rule exception
     */
    private static boolean compareDependency( String pattern, Artifact artifact )
        throws EnforcerRuleException
    {
    
        ArtifactMatcher.Pattern am = new Pattern( pattern );
        boolean result;
        try
        {
            result = am.match( artifact );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new EnforcerRuleException( "Invalid Version Range: ", e );
        }
    
        return result;
    }

}
