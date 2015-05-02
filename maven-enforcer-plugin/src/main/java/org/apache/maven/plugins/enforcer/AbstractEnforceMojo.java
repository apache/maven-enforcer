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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public abstract class AbstractEnforceMojo
    extends AbstractMojo
    implements Contextualizable
{
    /**
     * This is a static variable used to persist the cached results across plugin invocations.
     */
    protected static Hashtable<String, EnforcerRule> cache = new Hashtable<String, EnforcerRule>();

    /**
     * Path Translator needed by the ExpressionEvaluator
     */
    @Component( role = PathTranslator.class )
    protected PathTranslator translator;
    
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

    /**
     * Flag to easily skip all checks
     */
    @Parameter( property = "enforcer.skip", defaultValue = "false" )
    protected boolean skip = false;

    /**
     * Use this flag to disable rule result caching. This will cause all rules to execute on each project even if the
     * rule indicates it can safely be cached.
     */
    @Parameter( property = "enforcer.ignoreCache", defaultValue = "false" )
    protected boolean ignoreCache = false;

    // set by the contextualize method. Only way to get the
    // plugin's container in 2.0.x
    protected PlexusContainer container;

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /**
     * @return the fail
     */
    public abstract boolean isFail();

    /**
     * @return the rules
     */
    public abstract EnforcerRule[] getRules();

    /**
     * @param theRules to set.
     */
    public abstract void setRules( EnforcerRule[] theRules );

    /**
     * @return the skip
     */
    public boolean isSkip()
    {
        return this.skip;
    }

    /**
     * @param theSkip the skip to set
     */
    public void setSkip( boolean theSkip )
    {
        this.skip = theSkip;
    }

    /**
     * @return the failFast
     */
    public abstract boolean isFailFast();

    /**
     * @param failFast to set
     */
    public abstract void setFailFast( boolean failFast );

    /**
     * @return the project
     */
    public MavenProject getProject()
    {
        return this.project;
    }

    /**
     * @param theProject the project to set
     */
    public void setProject( MavenProject theProject )
    {
        this.project = theProject;
    }

    /**
     * @return the session
     */
    public MavenSession getSession()
    {
        return this.session;
    }

    /**
     * @param theSession the session to set
     */
    public void setSession( MavenSession theSession )
    {
        this.session = theSession;
    }

    /**
     * @return the translator
     */
    public PathTranslator getTranslator()
    {
        return this.translator;
    }

    /**
     * @param theTranslator the translator to set
     */
    public void setTranslator( PathTranslator theTranslator )
    {
        this.translator = theTranslator;
    }

    /**
     * Returns the error message displayed when failFast is set to false.
     *
     * @param i index
     * @param currentRule name of the current rule.
     * @param e rule exception
     * @return rule message
     */
    protected abstract String createRuleMessage( int i, String currentRule, EnforcerRuleException e );
}
