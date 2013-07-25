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
    @Component(role = PathTranslator.class)
    protected PathTranslator translator;
    /**
     * The MavenSession
     */
    @Component
    protected MavenSession session;
    /**
     * POM
     */
    @Component
    protected MavenProject project;
    /**
     * Flag to easily skip all checks
     */
    @Parameter(property = "enforcer.skip", defaultValue = "false")
    protected boolean skip = false;
    /**
     * Use this flag to disable rule result caching. This will cause
     * all rules to execute on each project even if the rule indicates it can
     * safely be cached.
     */
    @Parameter(property = "enforcer.ignoreCache", defaultValue = "false")
    protected boolean ignoreCache = false;
    // set by the contextualize method. Only way to get the
    // plugin's container in 2.0.x
    protected PlexusContainer container;

    public void contextualize(Context context) throws ContextException {
        container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }

    /**
     * Entry point to the mojo
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException
    {
        Log log = this.getLog();
        EnforcerExpressionEvaluator evaluator = new EnforcerExpressionEvaluator(
                session , translator , project );
        // the entire execution can be easily skipped
        if ( !skip )
        {
            // list to store exceptions
            List<String> list = new ArrayList<String>();
            // make sure the rules exist
            if ( getRules() != null && getRules().length > 0 )
            {
                String currentRule = "Unknown";
                // create my helper
                EnforcerRuleHelper helper = new DefaultEnforcementRuleHelper(
                        session , evaluator , log , container );
                // if we are only warning, then disable
                // failFast
                if ( !isFail() )
                {
                    setFailFast( false );
                }
                // go through each rule
                for ( int i = 0; i < getRules().length; i++ )
                {
                    // prevent against empty rules
                    EnforcerRule rule = getRules()[i];
                    if ( rule != null )
                    {
                        // store the current rule for
                        // logging purposes
                        currentRule = rule.getClass().getName();
                        log.debug( "Executing rule: " + currentRule );
                        try
                        {
                            if ( ignoreCache || shouldExecute( rule ) )
                            {
                                // execute the rule
                                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                                synchronized ( rule )
                                {
                                    rule.execute( helper );
                                }
                            }
                        }
                        catch ( EnforcerRuleException e )
                        {
                            // i can throw an exception
                            // because failfast will be
                            // false if fail is false.
                            if ( isFailFast() )
                            {
                                throw new MojoExecutionException(
                                        currentRule + " failed with message:\n"
                                        + e.getMessage() , e );
                            }
                            else
                            {
                                list.add( createRuleMessage( i , currentRule , e ));
                                log.debug( "Adding failure due to exception" ,
                                        e );
                            }
                        }
                    }
                }
                // if we found anything
                if ( !list.isEmpty() )
                {
                    for ( String failure : list )
                    {
                        log.warn( failure );
                    }
                    if ( isFail() )
                    {
                        throw new MojoExecutionException(
                                "Some Enforcer rules have failed. Look above for specific messages explaining why the rule failed." );
                    }
                }
            }
            else
            {
                throw new MojoExecutionException(
                        "No rules are configured. Use the skip flag if you want to disable execution." );
            }
        }
        else
        {
            log.info( "Skipping Rule Enforcement." );
        }
    }

    /**
     * This method determines if a rule should execute based
     * on the cache
     *
     * @param rule the rule to verify
     * @return {@code true} if rule should be executed, otherwise {@code false}
     */
    protected boolean shouldExecute( EnforcerRule rule )
    {
        if ( rule.isCacheable() )
        {
            Log log = this.getLog();
            log.debug( "Rule " + rule.getClass().getName() + " is cacheable." );
            String key = rule.getClass().getName() + " " + rule.getCacheId();
            if ( EnforceMojo.cache.containsKey( key ) )
            {
                log.debug( "Key " + key + " was found in the cache" );
                if ( rule.isResultValid( (EnforcerRule) cache.get( key ) ) )
                {
                    log.debug(
                            "The cached results are still valid. Skipping the rule: "
                            + rule.getClass().getName() );
                    return false;
                }
            }
            //add it to the cache of executed rules
            EnforceMojo.cache.put( key , rule );
        }
        return true;
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
    public boolean isSkip() {
        return this.skip;
    }

    /**
     * @param theSkip the skip to set
     */
    public void setSkip(boolean theSkip) {
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
    public MavenProject getProject() {
        return this.project;
    }

    /**
     * @param theProject the project to set
     */
    public void setProject(MavenProject theProject) {
        this.project = theProject;
    }

    /**
     * @return the session
     */
    public MavenSession getSession() {
        return this.session;
    }

    /**
     * @param theSession the session to set
     */
    public void setSession(MavenSession theSession) {
        this.session = theSession;
    }

    /**
     * @return the translator
     */
    public PathTranslator getTranslator() {
        return this.translator;
    }

    /**
     * @param theTranslator the translator to set
     */
    public void setTranslator(PathTranslator theTranslator) {
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
    protected abstract String createRuleMessage( int i , String currentRule , EnforcerRuleException e );
}
