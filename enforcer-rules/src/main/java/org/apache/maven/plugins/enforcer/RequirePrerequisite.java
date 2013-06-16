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

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * 
 * @author Robert Scholte
 * @since 1.3
 */
public class RequirePrerequisite extends AbstractNonCacheableEnforcerRule
{
    /**
     * Can either be version or a range, e.g. {@code 2.2.1} or {@code [2.2.1,)}
     */
    private String mavenVersion;

    /**
     * Set the mavenVersion
     * 
     * Can either be version or a range, e.g. {@code 2.2.1} or {@code [2.2.1,)}
     * 
     * @param mavenVersion the version or {@code null}
     */
    public void setMavenVersion( String mavenVersion )
    {
        this.mavenVersion = mavenVersion;
    }
    
    /**
     * {@inheritDoc}
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        try
        {
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            
            Prerequisites prerequisites = project.getPrerequisites(); 
            
            if( prerequisites == null )
            {
                throw new EnforcerRuleException( "Requires prerequisite not set" );
            }

            if( mavenVersion != null )
            {
                
                VersionRange requiredVersionRange = VersionRange.createFromVersionSpec( mavenVersion );
                
                if( !requiredVersionRange.hasRestrictions() )
                {
                    requiredVersionRange = VersionRange.createFromVersionSpec( "[" + mavenVersion + ",)" );
                }
                
                VersionRange specifiedVersion = VersionRange.createFromVersionSpec( prerequisites.getMaven() );
                
                VersionRange restrictedVersionRange = requiredVersionRange.restrict( specifiedVersion );
                
                if ( restrictedVersionRange.getRecommendedVersion() == null )
                {
                    throw new EnforcerRuleException( "The specified Maven prerequisite( " + specifiedVersion + " ) doesn't match the required version: " + mavenVersion );
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
