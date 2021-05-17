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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRule2;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;

/**
 * This goal executes the defined enforcer-rules once per module.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
// CHECKSTYLE_OFF: LineLength
@Mojo( name = "enforce", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyCollection = ResolutionScope.TEST, threadSafe = true )
//CHECKSTYLE_ON: LineLength
public class EnforceMojo
    extends AbstractMojo
    implements Contextualizable
{
    /**
     * This is a static variable used to persist the cached results across plugin invocations.
     */
    protected static Hashtable<String, EnforcerRule> cache = new Hashtable<>();

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
     * Flag to fail the build if a version check fails.
     */
    @Parameter( property = "enforcer.fail", defaultValue = "true" )
    private boolean fail = true;

    /**
     * Fail on the first rule that doesn't pass
     */
    @Parameter( property = "enforcer.failFast", defaultValue = "false" )
    private boolean failFast = false;

    /**
     * Array of objects that implement the EnforcerRule interface to execute.
     */
    @Parameter( required = false )
    private EnforcerRule[] rules;

    /**
     * Array of Strings that matches the EnforcerRules to execute.
     */
    @Parameter( required = false, property = "rules" )
    private String[] commandLineRules;

    /**
     * File with rules configurations to execute.
     */
    @Parameter ( required = false, property = "rulesFile" )
    private String rulesFilename;

    /**
     * Use this flag to disable rule result caching. This will cause all rules to execute on each project even if the
     * rule indicates it can safely be cached.
     */
    @Parameter( property = "enforcer.ignoreCache", defaultValue = "false" )
    protected boolean ignoreCache = false;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    private BuildPluginManager pluginManager;

    // set by the contextualize method. Only way to get the
    // plugin's container in 2.0.x
    protected PlexusContainer container;

    @Override
    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    private boolean havingRules()
    {
        return rules != null && rules.length > 0;
    }

    /**
     * Entry point to the mojo
     * 
     * @throws MojoExecutionException
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        Log log = this.getLog();

        EnforcerExpressionEvaluator evaluator =
            new EnforcerExpressionEvaluator( session, mojoExecution );
        if ( commandLineRules != null && commandLineRules.length > 0 )
        {
            this.rules = createRulesFromCommandLineOptions();
        }
        
        if ( rulesFilename != null && rulesFilename.length() > 0 )
        {
            Xpp3Dom configuration = null;
            try
            {
                configuration = Xpp3DomBuilder.build( new FileReader( rulesFilename ) );
            }
            catch ( org.codehaus.plexus.util.xml.pull.XmlPullParserException | IOException e )
            {
                String apath = rulesFilename;
                try
                {
                    apath = new File( rulesFilename ).getCanonicalPath();
                }
                catch ( IOException e1 )
                {
                }
                throw new MojoExecutionException( "Unable to locate rules configuration xml file " + apath, e );
            }
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
            
            configuration = Xpp3DomUtils.mergeXpp3Dom( configuration,
                toXpp3Dom( mojoDescriptor.getMojoConfiguration() ) );
            Xpp3Dom[] cs = configuration.getChildren();
            for ( int i = 0; i < cs.length; i++ )
            {
                Xpp3Dom c = cs[i];
                if ( "rulesFilename".equals( c.getName() ) )
                {
                    configuration.removeChild( i ); // remove to avoid re-doing this if block
                }
            }
            MojoExecution mojoExecution = new MojoExecution( mojoDescriptor, configuration );
            
            try
            {
                pluginManager.executeMojo( session, mojoExecution );
            }
            catch ( PluginConfigurationException
                    | PluginManagerException | MojoFailureException e )
            {
                // problem trying to run the new configuration
                throw new MojoExecutionException( "Unable to attempt new execution with new configuration", e );
            }
        }
        
        if ( isSkip() )
        {
            log.info( "Skipping Rule Enforcement." );
            return;
        }

        if ( !havingRules() )
        {
            // CHECKSTYLE_OFF: LineLength
            throw new MojoExecutionException( "No rules are configured. Use the skip flag if you want to disable execution." );
            // CHECKSTYLE_ON: LineLength
        }

        // list to store exceptions
        List<String> list = new ArrayList<>();

        String currentRule = "Unknown";

        // create my helper
        EnforcerRuleHelper helper = new DefaultEnforcementRuleHelper( session, evaluator, log, container );

        // if we are only warning, then disable
        // failFast
        if ( !fail )
        {
            failFast = false;
        }

        boolean hasErrors = false;

        // go through each rule
        for ( int i = 0; i < rules.length; i++ )
        {

            // prevent against empty rules
            EnforcerRule rule = rules[i];
            final EnforcerLevel level = getLevel( rule );
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
                        // noinspection
                        // SynchronizationOnLocalVariableOrMethodParameter
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
                    if ( failFast && level == EnforcerLevel.ERROR )
                    {
                        throw new MojoExecutionException( currentRule + " failed with message:"
                            + System.lineSeparator() + e.getMessage(), e );
                    }
                    else
                    {
                        // log a warning in case the exception message is missing
                        // so that the user can figure out what is going on
                        final String exceptionMessage = e.getMessage();
                        if ( exceptionMessage != null )
                        {
                            log.debug( "Adding " + level + " message due to exception", e );
                        }
                        else
                        {
                            log.warn( "Rule " + i + ": " + currentRule + " failed without a message", e );
                        }
                        // add the 'failed/warned' message including exceptionMessage
                        // which might be null in rare cases
                        if ( level == EnforcerLevel.ERROR )
                        {
                            hasErrors = true;
                            list.add( "Rule " + i + ": " + currentRule + " failed with message:"
                                 + System.lineSeparator() + exceptionMessage );
                        }
                        else
                        {
                            list.add( "Rule " + i + ": " + currentRule + " warned with message:"
                                 + System.lineSeparator() + exceptionMessage );
                        }
                    }
                }
            }
        }

        // if we found anything
        // CHECKSTYLE_OFF: LineLength
        if ( !list.isEmpty() )
        {
            for ( String failure : list )
            {
                log.warn( failure );
            }
            if ( fail && hasErrors )
            {
                throw new MojoExecutionException( "Some Enforcer rules have failed. Look above for specific messages explaining why the rule failed." );
            }
        }
        // CHECKSTYLE_ON: LineLength
    }

    private EnforcerRule[] createRulesFromCommandLineOptions() throws MojoExecutionException 
    {
        EnforcerRule[] rules = new EnforcerRule[commandLineRules.length];
        for ( int i = 0; i < commandLineRules.length; i++ ) 
        {
            String rule = commandLineRules[i];
            if ( !rule.contains( "." ) )
            {
                rule = getClass().getPackage().getName() 
                    + "." + Character.toUpperCase( rule.charAt( 0 ) ) + rule.substring( 1 ); 
            }
            
            try 
            {
                rules[i] = ( EnforcerRule ) Class.forName( rule ).newInstance();
            }
            catch ( Exception e ) 
            {
                throw new MojoExecutionException( "Failed to create enforcer rules from command line argument", e );
            }
        }
        return rules;
    }

    /**
     * This method determines if a rule should execute based on the cache
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
                if ( rule.isResultValid( cache.get( key ) ) )
                {
                    log.debug( "The cached results are still valid. Skipping the rule: " + rule.getClass().getName() );
                    return false;
                }
            }

            // add it to the cache of executed rules
            EnforceMojo.cache.put( key, rule );
        }
        return true;
    }

    /**
     * @return the fail
     */
    public boolean isFail()
    {
        return this.fail;
    }

    /**
     * @param theFail the fail to set
     */
    public void setFail( boolean theFail )
    {
        this.fail = theFail;
    }

    /**
     * @return the rules
     */
    public EnforcerRule[] getRules()
    {
        return this.rules;
    }

    /**
     * @param theRules the rules to set
     */
    public void setRules( EnforcerRule[] theRules )
    {
        this.rules = theRules;
    }

    /**
     * @param theFailFast the failFast to set
     */
    public void setFailFast( boolean theFailFast )
    {
        this.failFast = theFailFast;
    }

    public boolean isFailFast()
    {
        return failFast;
    }

    protected String createRuleMessage( int i, String currentRule, EnforcerRuleException e )
    {
        return "Rule " + i + ": " + currentRule + " failed with message:" + System.lineSeparator() + e.getMessage();
    }

    /**
     * Returns the level of the rule, defaults to {@link EnforcerLevel#ERROR} for backwards compatibility.
     *
     * @param rule might be of type {@link EnforcerRule2}.
     * @return level of the rule.
     */
    private EnforcerLevel getLevel( EnforcerRule rule )
    {
        if ( rule instanceof EnforcerRule2 )
        {
            return ( (EnforcerRule2) rule ).getLevel();
        }
        else
        {
            return EnforcerLevel.ERROR;
        }
    }

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
     * Converts PlexusConfiguration to a Xpp3Dom.
     *
     * @param config the PlexusConfiguration. Must not be {@code null}.
     * @return the Xpp3Dom representation of the PlexusConfiguration
     */
    public static Xpp3Dom toXpp3Dom( PlexusConfiguration config )
    {
        Xpp3Dom result = new Xpp3Dom( config.getName() );
        result.setValue( config.getValue( null ) );
        for ( String name : config.getAttributeNames() )
        {
            result.setAttribute( name, config.getAttribute( name ) );
        }
        for ( PlexusConfiguration child : config.getChildren() )
        {
            result.addChild( toXpp3Dom( child ) );
        }
        return result;
    }
}
