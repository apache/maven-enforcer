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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that the Java version is allowed.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class RequireJavaVersion
    extends AbstractVersionEnforcer
{
    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        String javaVersion = SystemUtils.JAVA_VERSION;
        Log log = helper.getLog();

        log.debug( "Detected Java String: '" + javaVersion + "'" );
        javaVersion = normalizeJDKVersion( javaVersion );
        log.debug( "Normalized Java String: '" + javaVersion + "'" );

        ArtifactVersion detectedJdkVersion = new DefaultArtifactVersion( javaVersion );

        log.debug( "Parsed Version: Major: " + detectedJdkVersion.getMajorVersion() + " Minor: "
            + detectedJdkVersion.getMinorVersion() + " Incremental: " + detectedJdkVersion.getIncrementalVersion()
            + " Build: " + detectedJdkVersion.getBuildNumber() + " Qualifier: " + detectedJdkVersion.getQualifier() );

        enforceVersion( helper.getLog(), "JDK", getVersion(), detectedJdkVersion );
    }

    /**
     * Converts a jdk string from 1.5.0-11b12 to a single 3 digit version like 1.5.0-11
     *
     * @param theJdkVersion to be converted.
     * @return the converted string.
     */
    public static String normalizeJDKVersion( String theJdkVersion )
    {

        theJdkVersion = theJdkVersion.replaceAll( "_|-", "." );
        String tokenArray[] = StringUtils.split( theJdkVersion, "." );
        List<String> tokens = Arrays.asList( tokenArray );
        StringBuilder buffer = new StringBuilder( theJdkVersion.length() );

        Iterator<String> iter = tokens.iterator();
        for ( int i = 0; i < tokens.size() && i < 4; i++ )
        {
            String section = iter.next();
            section = section.replaceAll( "[^0-9]", "" );

            if ( StringUtils.isNotEmpty( section ) )
            {
                buffer.append( Integer.parseInt( section ) );

                if ( i != 2 )
                {
                    buffer.append( '.' );
                }
                else
                {
                    buffer.append( '-' );
                }
            }
        }

        String version = buffer.toString();
        version = StringUtils.stripEnd( version, "-" );
        return StringUtils.stripEnd( version, "." );
    }
}
