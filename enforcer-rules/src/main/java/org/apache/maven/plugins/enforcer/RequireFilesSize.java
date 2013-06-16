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

import java.io.File;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * Rule to validate the main artifact is within certain size constraints.
 *
 * @author brianf
 * @author Roman Stumm
 */
public class RequireFilesSize
    extends AbstractRequireFiles
{

    /** the max size allowed. */
    long maxsize = 10000;

    /** the min size allowed. */
    long minsize = 0;

    /** The error msg. */
    private String errorMsg;

    /** The log. */
    private Log log;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        this.log = helper.getLog();

        // if the file is already defined, use that. Otherwise get the main artifact.
        if ( files.length == 0 )
        {
            try
            {
                MavenProject project = (MavenProject) helper.evaluate( "${project}" );
                files = new File[1];
                files[0] = project.getArtifact().getFile();

                super.execute( helper );
            }
            catch ( ExpressionEvaluationException e )
            {
                throw new EnforcerRuleException( "Unable to retrieve the project.", e );
            }
        }
        else
        {
            super.execute( helper );
        }

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

    /* (non-Javadoc)
     * @see org.apache.maven.plugins.enforcer.AbstractRequireFiles#checkFile(java.io.File)
     */
    boolean checkFile( File file )
    {
        if ( file == null )
        {
            //if we get here and it's null, treat it as a success.
            return true;
        }

        // check the file now
        if ( file.exists() )
        {
            long length = file.length();
            if ( length < minsize )
            {
                this.errorMsg = ( file + " size (" + length + ") too small. Min. is " + minsize );
                return false;
            }
            else if ( length > maxsize )
            {
                this.errorMsg = ( file + " size (" + length + ") too large. Max. is " + maxsize );
                return false;
            }
            else
            {

                this.log.debug( file
                    + " size ("
                    + length
                    + ") is OK ("
                    + ( minsize == maxsize || minsize == 0 ? ( "max. " + maxsize )
                                    : ( "between " + minsize + " and " + maxsize ) ) + " byte)." );

                return true;
            }
        }
        else
        {
            this.errorMsg = ( file + " does not exist!" );
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.maven.plugins.enforcer.AbstractRequireFiles#getErrorMsg()
     */
    String getErrorMsg()
    {
        return this.errorMsg;
    }
}
