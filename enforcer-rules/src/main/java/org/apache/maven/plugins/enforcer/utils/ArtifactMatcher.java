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

import java.util.Collection;
import java.util.LinkedList;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugins.enforcer.AbstractVersionEnforcer;
import org.apache.maven.plugins.enforcer.BanTransitiveDependencies;

/**
 * This class is used for matching Artifacts against a list of patterns.
 * 
 * @author Jakub Senko
 * @see BanTransitiveDependencies
 */
public final class ArtifactMatcher
{

    public static class Pattern
    {
        private String pattern;

        private String[] parts;

        public Pattern( String pattern )
        {
            if ( pattern == null )
                throw new NullPointerException( "pattern" );

            this.pattern = pattern;

            parts = pattern.split( ":", 6 );

            if ( parts.length == 6 )
                throw new IllegalArgumentException( "Pattern contains too many delimiters." );

            for ( String part : parts )
            {
                if ( "".equals( part ) )
                    throw new IllegalArgumentException( "Pattern or its part is empty." );
            }
        }

        public boolean match( Artifact artifact )
            throws InvalidVersionSpecificationException
        {
            if ( artifact == null )
                throw new NullPointerException( "artifact" );

            switch ( parts.length )
            {
                case 5:
                    String scope = artifact.getScope();
                    if ( scope == null || scope.equals( "" ) )
                    {
                        scope = "compile";
                    }

                    if ( !"*".equals( parts[4] ) && !parts[4].equals( scope ) )
                        return false;

                case 4:
                    String type = artifact.getType();
                    if ( type == null || type.equals( "" ) )
                    {
                        type = "jar";
                    }

                    if ( !"*".equals( parts[3] ) && !parts[3].equals( type ) )
                        return false;

                case 3:
                    if ( !"*".equals( parts[2] ) && !parts[2].equals( artifact.getVersion() ) )
                    {
                        if ( !AbstractVersionEnforcer.containsVersion( VersionRange.createFromVersionSpec( parts[2] ),
                                                                       new DefaultArtifactVersion(
                                                                                                   artifact.getVersion() ) ) )
                        {
                            return false;
                        }
                    }

                case 2:
                    if ( !"*".equals( parts[1] ) && !parts[1].equals( artifact.getArtifactId() ) )
                        return false;

                case 1:
                    if ( !"*".equals( parts[0] ) && !parts[0].equals( artifact.getGroupId() ) )
                        return false;
                    else
                        return true;
                default:
                    throw new AssertionError();
            }
        }

        @Override
        public String toString()
        {
            return pattern;
        }
    }

    private Collection<Pattern> patterns = new LinkedList<Pattern>();

    private Collection<Pattern> ignorePatterns = new LinkedList<Pattern>();

    /**
     * Construct class by providing patterns as strings. Empty strings are ignored.
     * 
     * @throws NullPointerException if any of the arguments is null
     */
    public ArtifactMatcher( final Collection<String> patterns, final Collection<String> ignorePatterns )
    {
        if ( patterns == null )
            throw new NullPointerException( "patterns" );
        if ( ignorePatterns == null )
            throw new NullPointerException( "ignorePatterns" );

        for ( String pattern : patterns )
        {
            if ( pattern != null && !"".equals( pattern ) )
            {
                this.patterns.add( new Pattern( pattern ) );
            }
        }

        for ( String ignorePattern : ignorePatterns )
        {
            if ( ignorePattern != null && !"".equals( ignorePattern ) )
            {
                this.ignorePatterns.add( new Pattern( ignorePattern ) );
            }
        }
    }

    /**
     * Check if artifact matches patterns.
     * 
     * @throws InvalidVersionSpecificationException
     */
    public boolean match( Artifact artifact )
        throws InvalidVersionSpecificationException
    {
        for ( Pattern pattern : patterns )
        {
            if ( pattern.match( artifact ) )
            {
                for ( Pattern ignorePattern : ignorePatterns )
                {
                    if ( ignorePattern.match( artifact ) )
                        return false;
                }

                return true;
            }
        }

        return false;
    }
}
