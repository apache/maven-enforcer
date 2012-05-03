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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.maven.BuildFailureException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugins.enforcer.utils.EnforcerRuleUtils;
import org.apache.maven.plugins.enforcer.utils.PluginWrapper;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * This rule will enforce that all plugins specified in the poms have a version declared.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class RequirePluginVersions
    extends AbstractNonCacheableEnforcerRule
{

    /** Don't allow the LATEST identifier. */
    public boolean banLatest = true;

    /** Don't allow the RELEASE identifier. */
    public boolean banRelease = true;

    /** Don't allow snapshot plugins. */
    public boolean banSnapshots = true;

    /** Don't allow timestamp snapshot plugins. */
    public boolean banTimestamps = true;

    /**
     * The comma separated list of phases that should be used to find lifecycle plugin bindings. The default value is
     * "clean,deploy,site".
     */
    public String phases = "clean,deploy,site";

    /**
     * Additional plugins to enforce have versions. These are plugins that may not be in the poms but are used anyway,
     * like help, eclipse etc. <br>
     * The plugins should be specified in the form: <code>group:artifactId</code>.
     */
    public List additionalPlugins;

    /**
     * Plugins to skip for version enforcement. The plugins should be specified in the form:
     * <code>group:artifactId</code>. NOTE: This is deprecated, use unCheckedPluginList instead.
     * @deprecated
     */
    public List unCheckedPlugins;

    /**
     * Same as unCheckedPlugins but as a comma list to better support properties. Sample form:
     * <code>group:artifactId,group2:artifactId2</code>
     * @since 1.0-beta-1
     */
    public String unCheckedPluginList;

    /** The plugin manager. */
    private PluginManager pluginManager;

    /** The phase to lifecycle map. */
    private Map phaseToLifecycleMap;

    /** The lifecycles. */
    private Collection lifecycles;

    /** The factory. */
    ArtifactFactory factory;

    /** The resolver. */
    ArtifactResolver resolver;

    /** The local. */
    ArtifactRepository local;

    /** The remote repositories. */
    List remoteRepositories;

    /** The log. */
    Log log;

    /** The session. */
    MavenSession session;

    /** The utils. */
    EnforcerRuleUtils utils;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        log = helper.getLog();

        MavenProject project;
        try
        {
            // get the various expressions out of the helper.

            project = (MavenProject) helper.evaluate( "${project}" );
            LifecycleExecutor life;
            life = (LifecycleExecutor) helper.getComponent( LifecycleExecutor.class );
            try
            {
              lifecycles = (Collection) ReflectionUtils.getValueIncludingSuperclasses( "lifecycles", life );
            }
            catch (Exception e)
            {
                //log.info( "The requirePluginVersions rule is currently not compatible with Maven3.");
                Object dlc = ReflectionUtils.getValueIncludingSuperclasses("defaultLifeCycles", life);
                Map mmap = (Map)ReflectionUtils.getValueIncludingSuperclasses("lifecycles", dlc);
                lifecycles = mmap.values();
                /*
                 *
                 * NOTE: If this happens, we're bailing out right away.
                 *
                 *
                 */
                //return;
            }
            session = (MavenSession) helper.evaluate( "${session}" );
            pluginManager = (PluginManager) helper.getComponent( PluginManager.class );
            factory = (ArtifactFactory) helper.getComponent( ArtifactFactory.class );
            resolver = (ArtifactResolver) helper.getComponent( ArtifactResolver.class );
            local = (ArtifactRepository) helper.evaluate( "${localRepository}" );
            remoteRepositories = project.getRemoteArtifactRepositories();

            utils = new EnforcerRuleUtils( helper );

            // get all the plugins that are bound to the specified lifecycles
            Set allPlugins = getBoundPlugins( life, project, phases );

            // insert any additional plugins specified by the user.
            allPlugins = addAdditionalPlugins( allPlugins, additionalPlugins );
            allPlugins.addAll( getProfilePlugins( project ) );


            // pull out any we should skip
            allPlugins = (Set) removeUncheckedPlugins( combineUncheckedPlugins( unCheckedPlugins, unCheckedPluginList ), allPlugins );

            // there's nothing to do here
            if ( allPlugins.isEmpty() )
            {
                log.info( "No plugin bindings found." );
                return;
            }
            else
            {
                log.debug( "All Plugins in use: " + allPlugins );
            }

            // get all the plugins that are mentioned in the pom (and parents)
            List pluginWrappers = getAllPluginEntries( project );

            // now look for the versions that aren't valid and add to a list.
            ArrayList failures = new ArrayList();
            Iterator iter = allPlugins.iterator();
            while ( iter.hasNext() )
            {
                Plugin plugin = (Plugin) iter.next();

                if ( !hasValidVersionSpecified( helper, plugin, pluginWrappers ) )
                {
                    failures.add( plugin );
                }
            }

            // if anything was found, log it then append the optional message.
            if ( !failures.isEmpty() )
            {
                StringBuffer newMsg = new StringBuffer();
                newMsg.append( "Some plugins are missing valid versions:" );
                if ( banLatest || banRelease || banSnapshots || banTimestamps )
                {
                    newMsg.append( "(" );
                    if ( banLatest )
                    {
                        newMsg.append( "LATEST " );
                    }
                    if ( banRelease )
                    {
                        newMsg.append( "RELEASE " );
                    }
                    if ( banSnapshots || banTimestamps )
                    {
                        newMsg.append( "SNAPSHOT " );
                    }
                    newMsg.append( "are not allowed )\n" );
                }
                iter = failures.iterator();
                while ( iter.hasNext() )
                {
                    Plugin plugin = (Plugin) iter.next();

                    newMsg.append( plugin.getGroupId() );
                    newMsg.append( ":" );
                    newMsg.append( plugin.getArtifactId() );

                    try
                    {
                        newMsg.append( ". \tThe version currently in use is " );

                        Plugin currentPlugin = findCurrentPlugin( plugin, project );

                        if ( currentPlugin != null )
                        {
                            newMsg.append( currentPlugin.getVersion() );
                        }
                        else
                        {
                            newMsg.append( "unknown" );
                        }
                    }
                    catch ( Exception e )
                    {
                        // lots can go wrong here. Don't allow any issues trying to
                        // determine the issue stop me
                        log.debug( "Exception while determining plugin Version.", e );
                        newMsg.append( ". Unable to determine the plugin version." );
                    }
                    newMsg.append( "\n" );
                }
                if ( StringUtils.isNotEmpty( message ) )
                {
                    newMsg.append( message );
                }

                throw new EnforcerRuleException( newMsg.toString() );
            }
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to Evaluate an Expression:" + e.getLocalizedMessage() );
        }
        catch ( ComponentLookupException e )
        {
            throw new EnforcerRuleException( "Unable to lookup a component:" + e.getLocalizedMessage() );
        }
        catch ( IllegalAccessException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( LifecycleExecutionException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( PluginNotFoundException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( IOException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( XmlPullParserException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( MojoExecutionException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
    }

    /**
     * Remove the plugins that the user doesn't want to check.
     *
     * @param uncheckedPlugins
     * @param plugins
     * @return
     * @throws MojoExecutionException
     */
    public Collection removeUncheckedPlugins( Collection uncheckedPlugins, Collection plugins )
        throws MojoExecutionException
    {
        if ( uncheckedPlugins != null && !uncheckedPlugins.isEmpty() )
        {
            Iterator iter = uncheckedPlugins.iterator();
            while ( iter.hasNext() )
            {
                Plugin plugin = parsePluginString( (String) iter.next(), "UncheckedPlugins" );
                plugins.remove( plugin );
            }
        }
        return plugins;
    }

    /**
     * Combines the old Collection with the new comma separated list.
     * @param uncheckedPlugins
     * @param uncheckedPluginsList
     * @return
     */
    public Collection combineUncheckedPlugins( Collection uncheckedPlugins, String uncheckedPluginsList )
    {
        //if the comma list is empty, then there's nothing to do here.
        if ( StringUtils.isNotEmpty( uncheckedPluginsList ) )
        {
            //make sure there is a collection to add to.
            if ( uncheckedPlugins == null )
            {
                uncheckedPlugins = new HashSet();
            }
            else if (!uncheckedPlugins.isEmpty() && log != null)
            {
                log.warn( "The parameter 'unCheckedPlugins' is deprecated. Use 'unCheckedPluginList' instead" );
            }

            uncheckedPlugins.addAll( Arrays.asList( uncheckedPluginsList.split( "," ) ) );
        }
        return uncheckedPlugins;
    }

    /**
     * Add the additional plugins if they don't exist yet.
     *
     * @param existing the existing
     * @param additional the additional
     * @return the sets the
     * @throws MojoExecutionException the mojo execution exception
     */
    public Set addAdditionalPlugins( Set existing, List additional )
        throws MojoExecutionException
    {
        if ( additional != null )
        {
            Iterator iter = additional.iterator();
            while ( iter.hasNext() )
            {
                String pluginString = (String) iter.next();
                Plugin plugin = parsePluginString( pluginString, "AdditionalPlugins" );

                if ( existing == null )
                {
                    existing = new HashSet();
                    existing.add( plugin );
                }
                else if ( !existing.contains( plugin ) )
                {
                    existing.add( plugin );
                }
            }
        }
        return existing;
    }

    /**
     * Helper method to parse and inject a Plugin.
     *
     * @param pluginString
     * @return
     * @throws MojoExecutionException
     */
    protected Plugin parsePluginString( String pluginString, String field )
        throws MojoExecutionException
    {
        if ( pluginString != null )
        {
            String[] pluginStrings = pluginString.split( ":" );
            if ( pluginStrings.length == 2 )
            {
                Plugin plugin = new Plugin();
                plugin.setGroupId( StringUtils.strip( pluginStrings[0] ) );
                plugin.setArtifactId( StringUtils.strip( pluginStrings[1] ) );

                return plugin;
            }
            else
            {
                throw new MojoExecutionException( "Invalid " + field + " string: " + pluginString );
            }
        }
        else
        {
            throw new MojoExecutionException( "Invalid " + field + " string: " + pluginString );
        }

    }

    /**
     * Finds the plugins that are listed in active profiles.
     *
     * @param project the project
     * @return the profile plugins
     */
    public Set getProfilePlugins( MavenProject project )
    {
        Set result = new HashSet();
        List profiles = project.getActiveProfiles();
        if ( profiles != null && !profiles.isEmpty() )
        {
            Iterator iter = profiles.iterator();
            while ( iter.hasNext() )
            {
                Profile p = (Profile) iter.next();
                BuildBase b = p.getBuild();
                if ( b != null )
                {
                    List plugins = b.getPlugins();
                    if ( plugins != null )
                    {
                        result.addAll( plugins );
                    }
                }
            }
        }
        return result;
    }

    /**
     * Given a plugin, this will retrieve the matching plugin artifact from the model.
     *
     * @param plugin plugin to lookup
     * @param project project to search
     * @return matching plugin, <code>null</code> if not found.
     */
    protected Plugin findCurrentPlugin( Plugin plugin, MavenProject project )
    {
        Plugin found = null;
        try
        {
            Model model = project.getModel();
            Map plugins = model.getBuild().getPluginsAsMap();
            found = (Plugin) plugins.get( plugin.getKey() );
        }
        catch ( NullPointerException e )
        {
            // nothing to do here
        }

        if ( found == null )
        {
            found = resolvePlugin( plugin, project );
        }

        return found;
    }

    /**
     * Resolve plugin.
     *
     * @param plugin the plugin
     * @param project the project
     * @return the plugin
     */
    protected Plugin resolvePlugin( Plugin plugin, MavenProject project )
    {

        List pluginRepositories = project.getPluginArtifactRepositories();
        Artifact artifact =
            factory.createPluginArtifact( plugin.getGroupId(), plugin.getArtifactId(),
                                          VersionRange.createFromVersion( "LATEST" ) );

        try
        {
            this.resolver.resolve( artifact, pluginRepositories, this.local );
            plugin.setVersion( artifact.getVersion() );
        }
        catch ( ArtifactResolutionException e )
        {
        }
        catch ( ArtifactNotFoundException e )
        {
        }

        return plugin;
    }

    /**
     * Gets the plugins that are bound to the defined phases. This does not find plugins bound in the pom to a phase
     * later than the plugin is executing.
     *
     * @param life the life
     * @param project the project
     * @param thePhases the the phases
     * @return the bound plugins
     * @throws PluginNotFoundException the plugin not found exception
     * @throws LifecycleExecutionException the lifecycle execution exception
     * @throws IllegalAccessException the illegal access exception
     */
    protected Set getBoundPlugins( LifecycleExecutor life, MavenProject project, String thePhases )
        throws PluginNotFoundException, LifecycleExecutionException, IllegalAccessException
    {

        Set allPlugins = new HashSet();

        // lookup the bindings for all the passed in phases
        String[] lifecyclePhases = thePhases.split( "," );
        for ( int i = 0; i < lifecyclePhases.length; i++ )
        {
            String lifecyclePhase = lifecyclePhases[i];
            if ( StringUtils.isNotEmpty( lifecyclePhase ) )
            {
                try
                {
                    Lifecycle lifecycle = getLifecycleForPhase( lifecyclePhase );
                    allPlugins.addAll( getAllPlugins( project, lifecycle ) );
                }
                catch ( BuildFailureException e )
                {
                    // i'm going to swallow this because the
                    // user may have declared a phase that
                    // doesn't exist for every module.
                }
            }
        }
        return allPlugins;
    }

    /*
     * Checks to see if the version is specified for the plugin. Can optionally ban "RELEASE" or "LATEST" even if
     * specified.
     */
    /**
     * Checks for valid version specified.
     *
     * @param helper the helper
     * @param source the source
     * @param pluginWrappers the plugins
     * @return true, if successful
     */
    protected boolean hasValidVersionSpecified( EnforcerRuleHelper helper, Plugin source, List pluginWrappers )
    {
        boolean found = false;
        boolean status = false;
        Iterator iter = pluginWrappers.iterator();
        while ( iter.hasNext() )
        {
            // find the matching plugin entry
            PluginWrapper plugin = (PluginWrapper) iter.next();
            if ( source.getArtifactId().equals( plugin.getArtifactId() )
                && source.getGroupId().equals( plugin.getGroupId() ) )
            {
                found = true;
                // found the entry. now see if the version is specified
                String version = plugin.getVersion();
                try
                {
                    version = (String) helper.evaluate( version );
                }
                catch ( ExpressionEvaluationException e )
                {
                    return false;
                }

                if ( StringUtils.isNotEmpty( version ) && !StringUtils.isWhitespace( version ) )
                {

                    if ( banRelease && version.equals( "RELEASE" ) )
                    {
                        return false;
                    }

                    if ( banLatest && version.equals( "LATEST" ) )
                    {
                        return false;
                    }

                    if ( banSnapshots && isSnapshot( version ) )
                    {
                        return false;
                    }
                    // the version was specified and not
                    // banned. It's ok. Keep looking through the list to make
                    // sure it's not using a banned version somewhere else.

                    status = true;

                    if ( !banRelease && !banLatest && !banSnapshots )
                    {
                        // no need to keep looking
                        break;
                    }
                }
            }
        }
        if ( !found )
        {
            log.debug( "plugin " + source.getGroupId() + ":" + source.getArtifactId() + " not found" );
        }
        return status;
    }

    /**
     * Checks if is snapshot.
     *
     * @param baseVersion the base version
     * @return true, if is snapshot
     */
    protected boolean isSnapshot( String baseVersion )
    {
        if ( banTimestamps )
        {
            return Artifact.VERSION_FILE_PATTERN.matcher( baseVersion ).matches()
                || baseVersion.endsWith( Artifact.SNAPSHOT_VERSION );
        }
        else
        {
            return baseVersion.endsWith( Artifact.SNAPSHOT_VERSION );
        }
    }

    /*
     * Uses borrowed lifecycle code to get a list of all plugins bound to the lifecycle.
     */
    /**
     * Gets the all plugins.
     *
     * @param project the project
     * @param lifecycle the lifecycle
     * @return the all plugins
     * @throws PluginNotFoundException the plugin not found exception
     * @throws LifecycleExecutionException the lifecycle execution exception
     */
    private Set getAllPlugins( MavenProject project, Lifecycle lifecycle )
        throws PluginNotFoundException, LifecycleExecutionException

    {
        HashSet plugins = new HashSet();
        // first, bind those associated with the packaging
        Map mappings = findMappingsForLifecycle( project, lifecycle );

        Iterator iter = mappings.entrySet().iterator();
        while ( iter.hasNext() )
        {
            Entry entry = (Entry) iter.next();
            String value = (String) entry.getValue();
            String tokens[] = value.split( ":" );

            Plugin plugin = new Plugin();
            plugin.setGroupId( tokens[0] );
            plugin.setArtifactId( tokens[1] );
            plugins.add( plugin );
        }

        List mojos = findOptionalMojosForLifecycle( project, lifecycle );
        iter = mojos.iterator();
        while ( iter.hasNext() )
        {
            String value = (String) iter.next();
            String tokens[] = value.split( ":" );

            Plugin plugin = new Plugin();
            plugin.setGroupId( tokens[0] );
            plugin.setArtifactId( tokens[1] );
            plugins.add( plugin );
        }

        for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
        {
            plugins.add( i.next() );
        }

        return plugins;
    }

    /*
     * NOTE: All the code following this point was scooped from the DefaultLifecycleExecutor. There must be a better way
     * but for now it should work.
     */
    /**
     * Gets the phase to lifecycle map.
     *
     * @return the phase to lifecycle map
     * @throws LifecycleExecutionException the lifecycle execution exception
     */
    public Map getPhaseToLifecycleMap()
        throws LifecycleExecutionException
    {
        if ( phaseToLifecycleMap == null )
        {
            phaseToLifecycleMap = new HashMap();

            for ( Iterator i = lifecycles.iterator(); i.hasNext(); )
            {
                Lifecycle lifecycle = (Lifecycle) i.next();

                for ( Iterator p = lifecycle.getPhases().iterator(); p.hasNext(); )
                {
                    String phase = (String) p.next();

                    if ( phaseToLifecycleMap.containsKey( phase ) )
                    {
                        Lifecycle prevLifecycle = (Lifecycle) phaseToLifecycleMap.get( phase );
                        throw new LifecycleExecutionException( "Phase '" + phase
                            + "' is defined in more than one lifecycle: '" + lifecycle.getId() + "' and '"
                            + prevLifecycle.getId() + "'" );
                    }
                    else
                    {
                        phaseToLifecycleMap.put( phase, lifecycle );
                    }
                }
            }
        }
        return phaseToLifecycleMap;
    }

    /**
     * Gets the lifecycle for phase.
     *
     * @param phase the phase
     * @return the lifecycle for phase
     * @throws BuildFailureException the build failure exception
     * @throws LifecycleExecutionException the lifecycle execution exception
     */
    private Lifecycle getLifecycleForPhase( String phase )
        throws BuildFailureException, LifecycleExecutionException
    {
        Lifecycle lifecycle = (Lifecycle) getPhaseToLifecycleMap().get( phase );

        if ( lifecycle == null )
        {
            throw new BuildFailureException( "Unable to find lifecycle for phase '" + phase + "'" );
        }
        return lifecycle;
    }

    /**
     * Find mappings for lifecycle.
     *
     * @param project the project
     * @param lifecycle the lifecycle
     * @return the map
     * @throws LifecycleExecutionException the lifecycle execution exception
     * @throws PluginNotFoundException the plugin not found exception
     */
    private Map findMappingsForLifecycle( MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        String packaging = project.getPackaging();
        Map mappings = null;

        LifecycleMapping m =
            (LifecycleMapping) findExtension( project, LifecycleMapping.ROLE, packaging, session.getSettings(),
                                              session.getLocalRepository() );
        if ( m != null )
        {
            mappings = m.getPhases( lifecycle.getId() );
        }

        Map defaultMappings = lifecycle.getDefaultPhases();

        if ( mappings == null )
        {
            try
            {
                m = (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, packaging );
                mappings = m.getPhases( lifecycle.getId() );
            }
            catch ( ComponentLookupException e )
            {
                if ( defaultMappings == null )
                {
                    throw new LifecycleExecutionException( "Cannot find lifecycle mapping for packaging: \'"
                        + packaging + "\'.", e );
                }
            }
        }

        if ( mappings == null )
        {
            if ( defaultMappings == null )
            {
                throw new LifecycleExecutionException( "Cannot find lifecycle mapping for packaging: \'" + packaging
                    + "\', and there is no default" );
            }
            else
            {
                mappings = defaultMappings;
            }
        }

        return mappings;
    }

    /**
     * Find optional mojos for lifecycle.
     *
     * @param project the project
     * @param lifecycle the lifecycle
     * @return the list
     * @throws LifecycleExecutionException the lifecycle execution exception
     * @throws PluginNotFoundException the plugin not found exception
     */
    private List findOptionalMojosForLifecycle( MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        String packaging = project.getPackaging();
        List optionalMojos = null;

        LifecycleMapping m =
            (LifecycleMapping) findExtension( project, LifecycleMapping.ROLE, packaging, session.getSettings(),
                                              session.getLocalRepository() );

        if ( m != null )
        {
            optionalMojos = m.getOptionalMojos( lifecycle.getId() );
        }

        if ( optionalMojos == null )
        {
            try
            {
                m = (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, packaging );
                optionalMojos = m.getOptionalMojos( lifecycle.getId() );
            }
            catch ( ComponentLookupException e )
            {
                log.debug( "Error looking up lifecycle mapping to retrieve optional mojos. Lifecycle ID: "
                    + lifecycle.getId() + ". Error: " + e.getMessage(), e );
            }
        }

        if ( optionalMojos == null )
        {
            optionalMojos = Collections.EMPTY_LIST;
        }

        return optionalMojos;
    }

    /**
     * Find extension.
     *
     * @param project the project
     * @param role the role
     * @param roleHint the role hint
     * @param settings the settings
     * @param localRepository the local repository
     * @return the object
     * @throws LifecycleExecutionException the lifecycle execution exception
     * @throws PluginNotFoundException the plugin not found exception
     */
    private Object findExtension( MavenProject project, String role, String roleHint, Settings settings,
                                  ArtifactRepository localRepository )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        Object pluginComponent = null;

        for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext() && pluginComponent == null; )
        {
            Plugin plugin = (Plugin) i.next();

            if ( plugin.isExtensions() )
            {
                verifyPlugin( plugin, project, settings, localRepository );

                // TODO: if moved to the plugin manager we
                // already have the descriptor from above
                // and so do can lookup the container
                // directly
                try
                {
                    pluginComponent = pluginManager.getPluginComponent( plugin, role, roleHint );
                }
                catch ( ComponentLookupException e )
                {
                    log.debug( "Unable to find the lifecycle component in the extension", e );
                }
                catch ( PluginManagerException e )
                {
                    throw new LifecycleExecutionException( "Error getting extensions from the plugin '"
                        + plugin.getKey() + "': " + e.getMessage(), e );
                }
            }
        }
        return pluginComponent;
    }

    /**
     * Verify plugin.
     *
     * @param plugin the plugin
     * @param project the project
     * @param settings the settings
     * @param localRepository the local repository
     * @return the plugin descriptor
     * @throws LifecycleExecutionException the lifecycle execution exception
     * @throws PluginNotFoundException the plugin not found exception
     */
    private PluginDescriptor verifyPlugin( Plugin plugin, MavenProject project, Settings settings,
                                           ArtifactRepository localRepository )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        PluginDescriptor pluginDescriptor;
        try
        {
            pluginDescriptor = pluginManager.verifyPlugin( plugin, project, settings, localRepository );
        }
        catch ( PluginManagerException e )
        {
            throw new LifecycleExecutionException( "Internal error in the plugin manager getting plugin '"
                + plugin.getKey() + "': " + e.getMessage(), e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        catch ( InvalidPluginException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        catch ( PluginVersionNotFoundException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        return pluginDescriptor;
    }

    /**
     * Gets all plugin entries in build.plugins, build.pluginManagement.plugins, profile.build.plugins, reporting and
     * profile.reporting in this project and all parents
     *
     * @param project the project
     * @return the all plugin entries wrapped in a PluginWrapper Object
     * @throws ArtifactResolutionException the artifact resolution exception
     * @throws ArtifactNotFoundException the artifact not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws XmlPullParserException the xml pull parser exception
     */
    protected List getAllPluginEntries( MavenProject project )
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        List plugins = new ArrayList();
        // get all the pom models
        
        String pomName = null;
        try
        {
            pomName = project.getFile().getName();
        }
        catch (Exception e)
        {
            pomName = "pom.xml";
        }
        List models =
            utils.getModelsRecursively( project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                        new File( project.getBasedir(), pomName ) );

        // now find all the plugin entries, either in
        // build.plugins or build.pluginManagement.plugins, profiles.plugins and reporting
        Iterator iter = models.iterator();
        while ( iter.hasNext() )
        {
            Model model = (Model) iter.next();
            try
            {
                plugins.addAll( PluginWrapper.addAll( model.getBuild().getPlugins(), model.getId() + ".build.plugins" ) );
            }
            catch ( NullPointerException e )
            {
                // guess there are no plugins here.
            }

            try
            {
                // add the reporting plugins
                plugins.addAll( PluginWrapper.addAll( model.getReporting().getPlugins(), model.getId() + ".reporting" ) );
            }
            catch ( NullPointerException e )
            {
                // guess there are no plugins here.
            }

            try
            {
                plugins.addAll( PluginWrapper.addAll( model.getBuild().getPluginManagement().getPlugins(),
                                                      model.getId() + ".build.pluginManagement.plugins" ) );
            }
            catch ( NullPointerException e )
            {
                // guess there are no plugins here.
            }

            // Add plugins in profiles
            Iterator it = model.getProfiles().iterator();
            while ( it.hasNext() )
            {
                Profile profile = (Profile) it.next();
                try
                {
                    plugins.addAll( PluginWrapper.addAll( profile.getBuild().getPlugins(), model.getId()
                        + ".profiles.profile[" + profile.getId() + "].build.plugins" ) );
                }
                catch ( NullPointerException e )
                {
                    // guess there are no plugins here.
                }

                try
                {
                    // add the reporting plugins
                    plugins.addAll( PluginWrapper.addAll( profile.getReporting().getPlugins(), model.getId()
                        + "profile[" + profile.getId() + "].reporting.plugins" ) );
                }
                catch ( NullPointerException e )
                {
                    // guess there are no plugins here.
                }
                try
                {
                    // add the reporting plugins
                    plugins.addAll( PluginWrapper.addAll( profile.getBuild().getPluginManagement().getPlugins(),
                                                          model.getId() + "profile[" + profile.getId()
                                                              + "].build.pluginManagement.plugins" ) );
                }
                catch ( NullPointerException e )
                {
                    // guess there are no plugins here.
                }
            }
        }

        return plugins;
    }

    /**
     * Checks if is ban latest.
     *
     * @return the banLatest
     */
    protected boolean isBanLatest()
    {
        return this.banLatest;
    }

    /**
     * Sets the ban latest.
     *
     * @param theBanLatest the banLatest to set
     */
    protected void setBanLatest( boolean theBanLatest )
    {
        this.banLatest = theBanLatest;
    }

    /**
     * Checks if is ban release.
     *
     * @return the banRelease
     */
    protected boolean isBanRelease()
    {
        return this.banRelease;
    }

    /**
     * Sets the ban release.
     *
     * @param theBanRelease the banRelease to set
     */
    protected void setBanRelease( boolean theBanRelease )
    {
        this.banRelease = theBanRelease;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    protected String getMessage()
    {
        return this.message;
    }

    /**
     * Sets the message.
     *
     * @param theMessage the message to set
     */
    protected void setMessage( String theMessage )
    {
        this.message = theMessage;
    }

    /**
     * Gets the utils.
     *
     * @return the utils
     */
    protected EnforcerRuleUtils getUtils()
    {
        return this.utils;
    }

    /**
     * Sets the utils.
     *
     * @param theUtils the utils to set
     */
    protected void setUtils( EnforcerRuleUtils theUtils )
    {
        this.utils = theUtils;
    }

    /**
     * Checks if is ban snapshots.
     *
     * @return the banSnapshots
     */
    public boolean isBanSnapshots()
    {
        return this.banSnapshots;
    }

    /**
     * Sets the ban snapshots.
     *
     * @param theBanSnapshots the banSnapshots to set
     */
    public void setBanSnapshots( boolean theBanSnapshots )
    {
        this.banSnapshots = theBanSnapshots;
    }

    /**
     * Checks if is ban timestamps.
     *
     * @return the banTimestamps
     */
    public boolean isBanTimestamps()
    {
        return this.banTimestamps;
    }

    /**
     * Sets the ban timestamps.
     *
     * @param theBanTimestamps the banTimestamps to set
     */
    public void setBanTimestamps( boolean theBanTimestamps )
    {
        this.banTimestamps = theBanTimestamps;
    }

    public List getUnCheckedPlugins()
    {
        return unCheckedPlugins;
    }

    public void setUnCheckedPlugins( List unCheckedPlugins )
    {
        this.unCheckedPlugins = unCheckedPlugins;
    }
}