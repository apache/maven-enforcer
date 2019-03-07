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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;

/**
 * Just a mock object hard coded to return version 2.0.5
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class MockRuntimeInformation
    implements RuntimeInformation
{
    @Override
    public String getMavenVersion() {
        return "2.0.5";
    }

    @Override
    public boolean isMavenVersion(String versionRange) {
        VersionScheme versionScheme = new GenericVersionScheme();

        Validate.notBlank( versionRange, "versionRange can neither be null, empty nor blank" );

        VersionConstraint constraint;
        try
        {
            constraint = versionScheme.parseVersionConstraint( versionRange );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new IllegalArgumentException( e.getMessage(), e );
        }

        Version current;
        try
        {
            String mavenVersion = getMavenVersion();
            Validate.validState( StringUtils.isNotEmpty( mavenVersion ), "Could not determine current Maven version" );

            current = versionScheme.parseVersion( mavenVersion );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new IllegalStateException( "Could not parse current Maven version: " + e.getMessage(), e );
        }

        if ( constraint.getRange() == null )
        {
            return constraint.getVersion().compareTo( current ) <= 0;
        }
        return constraint.containsVersion( current );
    }
}
