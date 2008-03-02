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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that lists of dependencies are not included.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class BannedDependencies
    extends AbstractBanDependencies
{

    /**
     * Specify the banned dependencies. This can be a list of artifacts in the format groupId[:artifactId][:version] Any
     * of the sections can be a wildcard by using '*' (ie group:*:1.0) <br>
     * The rule will fail if any dependencies match any exclude, unless it also matches an include rule.
     * 
     * @parameter
     * @required
     */
    public List excludes = null;

    /**
     * Specify the allowed dependencies. This can be a list of artifacts in the format groupId[:artifactId][:version]
     * Any of the sections can be a wildcard by using '*' (ie group:*:1.0) <br>
     * Includes override the exclude rules. It is meant to allow wide exclusion rules with wildcards and still allow a
     * smaller set of includes. <br>
     * For example, to ban all xerces except xerces-api -> exclude "xerces", include "xerces:xerces-api"
     * 
     * @parameter default-value="*"
     * @required
     */
    public List includes = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.plugin.enforcer.AbstractBanDependencies#checkDependencies(java.util.Set)
     */
    protected Set checkDependencies( Set theDependencies, Log log )
        throws EnforcerRuleException
    {
        Set excluded = checkDependencies( theDependencies, excludes );

        // anything specifically included should be removed
        // from the ban list.
        if ( excluded != null )
        {
            Set included = checkDependencies( theDependencies, includes );
            if ( included != null )
            {
                excluded.removeAll( included );
            }
        }
        return excluded;

    }

    /**
     * Checks the set of dependencies against the list of patterns
     * 
     * @param thePatterns
     * @param dependencies
     * @return
     * @throws EnforcerRuleException
     */
    private Set checkDependencies( Set dependencies, List thePatterns )
        throws EnforcerRuleException
    {
        Set foundMatches = null;

        if ( thePatterns != null && thePatterns.size() > 0 )
        {

            Iterator iter = thePatterns.iterator();
            while ( iter.hasNext() )
            {
                String pattern = (String) iter.next();

                String[] subStrings = pattern.split( ":" );
                subStrings = StringUtils.stripAll( subStrings );

                Iterator DependencyIter = dependencies.iterator();
                while ( DependencyIter.hasNext() )
                {
                    Artifact artifact = (Artifact) DependencyIter.next();

                    if ( compareDependency( subStrings, artifact ) )
                    {
                        // only create if needed
                        if ( foundMatches == null )
                        {
                            foundMatches = new HashSet();
                        }
                        foundMatches.add( artifact );
                    }
                }
            }
        }
        return foundMatches;
    }

    /**
     * Compares the parsed array of substrings against the artifact
     * 
     * @param pattern
     * @param artifact
     * @return
     * @throws EnforcerRuleException
     */
    protected boolean compareDependency( String[] pattern, Artifact artifact )
        throws EnforcerRuleException
    {

        boolean result = false;
        if ( pattern.length > 0 )
        {
            result = pattern[0].equals( "*" ) || artifact.getGroupId().equals( pattern[0] );
        }

        if ( result && pattern.length > 1 )
        {
            result = pattern[1].equals( "*" ) || artifact.getArtifactId().equals( pattern[1] );
        }

        if ( result && pattern.length > 2 )
        {
            // short circuit if the versions are exactly the
            // same
            if ( pattern[2].equals( "*" ) || artifact.getVersion().equals( pattern[2] ) )
            {
                result = true;
            }
            else
            {
                try
                {
                    result =
                        AbstractVersionEnforcer.containsVersion( VersionRange.createFromVersionSpec( pattern[2] ),
                                                                 new DefaultArtifactVersion( artifact.getVersion() ) );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new EnforcerRuleException( "Invalid Version Range: ", e );
                }
            }
        }

        return result;

    }

    /**
     * @return the excludes
     */
    public List getExcludes()
    {
        return this.excludes;
    }

    /**
     * @param theExcludes the excludes to set
     */
    public void setExcludes( List theExcludes )
    {
        this.excludes = theExcludes;
    }

    /**
     * @return the includes
     */
    public List getIncludes()
    {
        return this.includes;
    }

    /**
     * @param theIncludes the includes to set
     */
    public void setIncludes( List theIncludes )
    {
        this.includes = theIncludes;
    }

}
