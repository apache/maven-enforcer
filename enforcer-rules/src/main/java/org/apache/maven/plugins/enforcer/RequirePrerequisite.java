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

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * @author Robert Scholte
 * @since 1.3
 */
public class RequirePrerequisite
    extends AbstractNonCacheableEnforcerRule
{
    /**
     * Only the projects with one of these packagings will be enforced to have the correct prerequisite.
     * 
     * @since 1.4
     */
    private List<String> packagings;

    /**
     * Can either be version or a range, e.g. {@code 2.2.1} or {@code [2.2.1,)}
     */
    private String mavenVersion;

    /**
     * Set the mavenVersion Can either be version or a range, e.g. {@code 2.2.1} or {@code [2.2.1,)}
     * 
     * @param mavenVersion the version or {@code null}
     */
    public void setMavenVersion( String mavenVersion )
    {
        this.mavenVersion = mavenVersion;
    }

    /**
     * Only the projects with one of these packagings will be enforced to have the correct prerequisite.
     * 
     * @since 1.4
     * @param packagings the list of packagings
     */
    public void setPackagings( List<String> packagings )
    {
        this.packagings = packagings;
    }

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        try
        {
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );

            if ( "pom".equals( project.getPackaging() ) )
            {
                helper.getLog().debug( "Packaging is pom, skipping requirePrerequisite rule" );
                return;
            }

            if ( packagings != null && !packagings.contains( project.getPackaging() ) )
            {
                // CHECKSTYLE_OFF: LineLength
                helper.getLog().debug( "Packaging is " + project.getPackaging() + ", skipping requirePrerequisite rule" );
                return;
                // CHECKSTYLE_ON: LineLength
            }

            Prerequisites prerequisites = project.getPrerequisites();

            if ( prerequisites == null )
            {
                throw new EnforcerRuleException( "Requires prerequisite not set" );
            }

            if ( mavenVersion != null )
            {

                VersionRange requiredVersionRange = VersionRange.createFromVersionSpec( mavenVersion );

                if ( !requiredVersionRange.hasRestrictions() )
                {
                    requiredVersionRange = VersionRange.createFromVersionSpec( "[" + mavenVersion + ",)" );
                }

                VersionRange specifiedVersion = VersionRange.createFromVersionSpec( prerequisites.getMaven() );

                VersionRange restrictedVersionRange = requiredVersionRange.restrict( specifiedVersion );

                if ( restrictedVersionRange.getRecommendedVersion() == null )
                {
                    throw new EnforcerRuleException( "The specified Maven prerequisite( " + specifiedVersion
                        + " ) doesn't match the required version: " + mavenVersion );
                }
            }
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new EnforcerRuleException( e.getMessage(), e );
        }
    }

}
