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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * This rule checks that the current project is not a release.
 */
public class RequireSnapshotVersion
    extends AbstractNonCacheableEnforcerRule
{

    /**
     * Allows this rule to fail when the parent is defined as a release.
     */
    private boolean failWhenParentIsRelease = true;

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {

        MavenProject project = getProject( helper );
        Artifact artifact = project.getArtifact();

        if ( !artifact.isSnapshot() )
        {
            String message = getMessage();
            StringBuilder sb = new StringBuilder();
            if ( message != null )
            {
                sb.append( message ).append( '\n' );
            }
            sb.append( "This project cannot be a release:" ).append( artifact.getId() );
            throw new EnforcerRuleException( sb.toString() );
        }
        if ( failWhenParentIsRelease )
        {
            Artifact parentArtifact = project.getParentArtifact();
            if ( parentArtifact != null && !parentArtifact.isSnapshot() )
            {
                throw new EnforcerRuleException( "Parent cannot be a release: " + parentArtifact.getId() );
            }
        }

    }

    private MavenProject getProject( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        try
        {
            return (MavenProject) helper.evaluate( "${project}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenProject: ", eee );
        }
    }

    public boolean isFailWhenParentIsRelease()
    {
        return failWhenParentIsRelease;
    }

    public void setFailWhenParentIsRelease( boolean failWhenParentIsRelease )
    {
        this.failWhenParentIsRelease = failWhenParentIsRelease;
    }

}
