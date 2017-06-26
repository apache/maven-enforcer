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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * This goal displays the current platform information.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
@Mojo( name = "display-info", threadSafe = true )
public class DisplayInfoMojo
    extends AbstractMojo
    implements Contextualizable
{

    /**
     * MojoExecution needed by the ExpressionEvaluator
     */
    @Parameter( defaultValue = "${mojoExecution}", readonly = true, required = true )
    protected MojoExecution mojoExecution;

    /**
     * The MavenSession
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    /**
     * POM
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    // set by the contextualize method. Only way to get the
    // plugin's container in 2.0.x
    protected PlexusContainer container;

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /**
     * Entry point to the mojo
     */
    public void execute()
        throws MojoExecutionException
    {
        String mavenVersion = session.getSystemProperties().getProperty( "maven.version" );
        String javaVersion = System.getProperty( "java.version" );
        getLog().info( "Maven Version: " + mavenVersion );
        getLog().info( "JDK Version: " + javaVersion + " normalized as: "
            + RequireJavaVersion.normalizeJDKVersion( javaVersion ) );
        RequireOS os = new RequireOS();
        os.displayOSInfo( getLog(), true );
    }

}
