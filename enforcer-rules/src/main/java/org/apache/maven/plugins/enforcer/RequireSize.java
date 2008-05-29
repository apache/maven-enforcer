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
package org.apache.maven.plugins.enforcer;

import java.io.File;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

// TODO: Auto-generated Javadoc
/**
 * Rule to validate the main artifact is within certain size constraints.
 * 
 * @author brianf
 * @author Roman Stumm
 */
public class RequireSize
    extends AbstractStandardEnforcerRule
{

    /** the max size allowed. */
    long maxsize = 10000;

    /** the max size allowed. */
    long minsize = 0;

    /** the artifact file to check. */
    File artifact;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        // if the artifact is already defined, use that. Otherwise get the main artifact.
        if ( artifact == null )
        {
            try
            {
                MavenProject project = (MavenProject) helper.evaluate( "${project}" );

                artifact = project.getArtifact().getFile();
            }
            catch ( ExpressionEvaluationException e )
            {
                throw new EnforcerRuleException( "Unable to retrieve the project.", e );
            }
        }

        // check the file now
        if ( artifact.exists() )
        {
            long length = artifact.length();
            if ( length < minsize )
            {
                throw new EnforcerRuleException( artifact + " size (" + length + ") too small. Min. is " + minsize );
            }
            else if ( length > maxsize )
            {
                throw new EnforcerRuleException( artifact + " size (" + length + ") too large. Max. is " + maxsize );
            }
            else
            {

                helper.getLog().debug(
                                       artifact +
                                           " size (" +
                                           length +
                                           ") is OK (" +
                                           ( minsize == maxsize || minsize == 0 ? ( "max. " + maxsize ) : ( "between " +
                                               minsize + " and " + maxsize ) ) + " byte)." );

            }
        }
        else
        {
            throw new EnforcerRuleException( artifact + " does not exist!" );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#getCacheId()
     */
    public String getCacheId()
    {
        return "0";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isCacheable()
     */
    public boolean isCacheable()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isResultValid(org.apache.maven.enforcer.rule.api.EnforcerRule)
     */
    public boolean isResultValid( EnforcerRule cachedRule )
    {
        return false;
    }
}
