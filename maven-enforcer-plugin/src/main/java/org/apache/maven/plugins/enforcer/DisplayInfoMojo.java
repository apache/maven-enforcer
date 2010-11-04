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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * This goal displays the current platform information.
 * 
 * @goal display-info
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class DisplayInfoMojo
    extends AbstractMojo
    implements Contextualizable
{

    /**
     * Path Translator needed by the ExpressionEvaluator
     * 
     * @component role="org.apache.maven.project.path.PathTranslator"
     */
    protected PathTranslator translator;

    /**
     * The MavenSession
     * 
     * @parameter default-value="${session}"
     * @readonly
     */
    protected MavenSession session;

    /**
     * POM
     * 
     * @parameter default-value="${project}"
     * @readonly
     * @required
     */
    protected MavenProject project;

    // set by the contextualize method. Only way to get the
    // plugin's container in 2.0.x
    protected PlexusContainer container;

    public void contextualize ( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /**
     * Entry point to the mojo
     */
    public void execute ()
        throws MojoExecutionException
    {
        try
        {
            EnforcerExpressionEvaluator evaluator = new EnforcerExpressionEvaluator( session, translator, project );
            DefaultEnforcementRuleHelper helper = new DefaultEnforcementRuleHelper( session, evaluator, getLog(),
                                                                                    container );
            RuntimeInformation rti = (RuntimeInformation) helper.getComponent( RuntimeInformation.class );
            getLog().info( "Maven Version: " + rti.getApplicationVersion() );
            getLog().info( "JDK Version: " + SystemUtils.JAVA_VERSION + " normalized as: "
                               + RequireJavaVersion.normalizeJDKVersion( SystemUtils.JAVA_VERSION_TRIMMED ) );
            RequireOS os = new RequireOS();
            os.displayOSInfo( getLog(), true );

        }
        catch ( ComponentLookupException e )
        {
            getLog().warn( "Unable to Lookup component: " + e.getLocalizedMessage() );
        }

    }

}
