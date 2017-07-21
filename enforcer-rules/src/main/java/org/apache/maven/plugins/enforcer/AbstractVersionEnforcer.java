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

import java.util.List;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * Contains the common code to compare a version against a version range.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public abstract class AbstractVersionEnforcer
    extends AbstractStandardEnforcerRule
{

    /**
     * Specify the required version. Some examples are:
     * <ul>
     * <li><code>2.0.4</code> Version 2.0.4 and higher (different from Maven meaning)</li>
     * <li><code>[2.0,2.1)</code> Versions 2.0 (included) to 2.1 (not included)</li>
     * <li><code>[2.0,2.1]</code> Versions 2.0 to 2.1 (both included)</li>
     * <li><code>[2.0.5,)</code> Versions 2.0.5 and higher</li>
     * <li><code>(,2.0.5],[2.1.1,)</code> Versions up to 2.0.5 (included) and 2.1.1 or higher</li>
     * </ul>
     * 
     * @see {@link #setVersion(String)}
     * @see {@link #getVersion()}
     */
    private String version;

    /**
     * Compares the specified version to see if it is allowed by the defined version range.
     *
     * @param log the log
     * @param variableName name of variable to use in messages (Example: "Maven" or "Java" etc).
     * @param requiredVersionRange range of allowed versions.
     * @param actualVersion the version to be checked.
     * @throws EnforcerRuleException the enforcer rule exception
     */
    // CHECKSTYLE_OFF: LineLength
    public void enforceVersion( Log log, String variableName, String requiredVersionRange, ArtifactVersion actualVersion )
        throws EnforcerRuleException
    // CHECKSTYLE_ON: LineLength
    {
        if ( StringUtils.isEmpty( requiredVersionRange ) )
        {
            throw new EnforcerRuleException( variableName + " version can't be empty." );
        }
        else
        {

            VersionRange vr;
            String msg = "Detected " + variableName + " Version: " + actualVersion;

            // short circuit check if the strings are exactly equal
            if ( actualVersion.toString().equals( requiredVersionRange ) )
            {
                log.debug( msg + " is allowed in the range " + requiredVersionRange + "." );
            }
            else
            {
                try
                {
                    vr = VersionRange.createFromVersionSpec( requiredVersionRange );

                    if ( containsVersion( vr, actualVersion ) )
                    {
                        log.debug( msg + " is allowed in the range " + requiredVersionRange + "." );
                    }
                    else
                    {
                        String message = getMessage();

                        if ( StringUtils.isEmpty( message ) )
                        {
                            message = msg + " is not in the allowed range " + vr + ".";
                        }

                        throw new EnforcerRuleException( message );
                    }
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new EnforcerRuleException( "The requested " + variableName + " version "
                        + requiredVersionRange + " is invalid.", e );
                }
            }
        }
    }

    /**
     * Copied from Artifact.VersionRange. This is tweaked to handle singular ranges properly. Currently the default
     * containsVersion method assumes a singular version means allow everything. This method assumes that "2.0.4" ==
     * "[2.0.4,)"
     *
     * @param allowedRange range of allowed versions.
     * @param theVersion the version to be checked.
     * @return true if the version is contained by the range.
     */
    public static boolean containsVersion( VersionRange allowedRange, ArtifactVersion theVersion )
    {
        boolean matched = false;
        ArtifactVersion recommendedVersion = allowedRange.getRecommendedVersion();
        if ( recommendedVersion == null )
        {
            List<Restriction> restrictions = allowedRange.getRestrictions();
            for ( Restriction restriction : restrictions )
            {
                if ( restriction.containsVersion( theVersion ) )
                {
                    matched = true;
                    break;
                }
            }
        }
        else
        {
            // only singular versions ever have a recommendedVersion
            @SuppressWarnings( "unchecked" )
            int compareTo = recommendedVersion.compareTo( theVersion );
            matched = ( compareTo <= 0 );
        }
        return matched;
    }

    @Override
    public String getCacheId()
    {
        if ( StringUtils.isNotEmpty( version ) )
        {
            // return the hashcodes of the parameter that matters
            return "" + version.hashCode();
        }
        else
        {
            return "0";
        }

    }

    @Override
    public boolean isCacheable()
    {
        // the maven version is not going to change between projects in the same build.
        return true;
    }

    @Override
    public boolean isResultValid( EnforcerRule theCachedRule )
    {
        // i will always return the hash of the parameters as my id. If my parameters are the same, this
        // rule must always have the same result.
        return true;
    }

    /**
     * Gets the required version.
     *
     * @return the required version
     */
    public final String getVersion()
    {
        return this.version;
    }

    /**
     * Specify the required version. Some examples are:
     * <ul>
     * <li><code>2.0.4</code> Version 2.0.4 and higher (different from Maven meaning)</li>
     * <li><code>[2.0,2.1)</code> Versions 2.0 (included) to 2.1 (not included)</li>
     * <li><code>[2.0,2.1]</code> Versions 2.0 to 2.1 (both included)</li>
     * <li><code>[2.0.5,)</code> Versions 2.0.5 and higher</li>
     * <li><code>(,2.0.5],[2.1.1,)</code> Versions up to 2.0.5 (included) and 2.1.1 or higher</li>
     * </ul>
     *
     * @param theVersion the required version to set
     */
    public final void setVersion( String theVersion )
    {
        this.version = theVersion;
    }

}
