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
 * This rule checks that the current project is not a snapshot.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class RequireReleaseVersion
    extends AbstractNonCacheableEnforcerRule
{

    /**
     * Allows this rule to fail when the parent is defined as a snapshot.
     *
     * @parameter
     * 
     * @see {@link #setFailWhenParentIsSnapshot(boolean)}
     * @see {@link #isFailWhenParentIsSnapshot()}
     */
    private boolean failWhenParentIsSnapshot = true;

    @Override
    public void execute( EnforcerRuleHelper theHelper )
        throws EnforcerRuleException
    {

        MavenProject project = getProject( theHelper );

        if ( project.getArtifact().isSnapshot() )
        {
            String message = getMessage();
            StringBuilder buf = new StringBuilder();
            if ( message != null )
            {
                buf.append( message ).append( System.lineSeparator() );
            }
            buf.append( "This project cannot be a snapshot:" ).append( project.getArtifact().getId() );
            throw new EnforcerRuleException( buf.toString() );
        }
        if ( failWhenParentIsSnapshot )
        {
            Artifact parentArtifact = project.getParentArtifact();
            if ( parentArtifact != null && parentArtifact.isSnapshot() )
            {
                throw new EnforcerRuleException( "Parent Cannot be a snapshot: " + parentArtifact.getId() );
            }
        }

    }

    /**
     * @param helper
     * @return The evaluated {@link MavenProject}.
     * @throws EnforcerRuleException
     */
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

    public final boolean isFailWhenParentIsSnapshot()
    {
        return failWhenParentIsSnapshot;
    }

    public final void setFailWhenParentIsSnapshot( boolean failWhenParentIsSnapshot )
    {
        this.failWhenParentIsSnapshot = failWhenParentIsSnapshot;
    }
}
