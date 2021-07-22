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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
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
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * This rule will enforce that all plugins specified in the poms have a version declared.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class RequirePluginVersions
    extends AbstractNonCacheableEnforcerRule
{

    private EnforcerRuleHelper helper;

    /**
     * Don't allow the LATEST identifier.
     * 
     * @see {@link #setBanLatest(boolean)}
     * @see {@link #isBanLatest()}
     */
    private boolean banLatest = true;

    /**
     * Don't allow the RELEASE identifier.
     * 
     * @see {@link #setBanRelease(boolean)}
     * @see {@link #isBanRelease()}
     */
    private boolean banRelease = true;

    /**
     * Don't allow snapshot plugins.
     * 
     * @see {@link #setBanSnapshots(boolean)}
     * @see {@link #isBanSnapshots()}
     */
    private boolean banSnapshots = true;

    /**
     * Don't allow timestamp snapshot plugins.
     * 
     * @see {@link #setBanTimestamps(boolean)}
     * @see {@link #isBanTimestamps()}
     */
    private boolean banTimestamps = true;

    /**
     * @since 3.0.0
     */
    private boolean banMavenDefaults = true;

    /**
     * The comma separated list of phases that should be used to find lifecycle plugin bindings. The default value is
     * "clean,deploy,site".
     * 
     * @see {@link #setPhases(String)}
     * @see {@link #getPhases()}
     */
    private String phases = "clean,deploy,site";

    /**
     * Additional plugins to enforce have versions. These are plugins that may not be in the poms but are used anyway,
     * like help, eclipse etc. <br>
     * The plugins should be specified in the form: <code>group:artifactId</code>.
     * 
     * @see {@link #setAdditionalPlugins(List)}
     * @see {@link #getAdditionalPlugins()}
     */
    private List<String> additionalPlugins;

    /**
     * Plugins to skip for version enforcement. The plugins should be specified in the form:
     * <code>group:artifactId</code>. NOTE: This is deprecated, use unCheckedPluginList instead.
     * 
     * @see {@link #setUnCheckedPlugins(List)}
     * @see {@link #getUnCheckedPlugins()}
     */
    private List<String> unCheckedPlugins;

    /**
     * Same as unCheckedPlugins but as a comma list to better support properties. Sample form:
     * <code>group:artifactId,group2:artifactId2</code>
     * 
     * @since 1.0-beta-1
     * @see {@link #setUnCheckedPlugins(List)}
     * @see {@link #getUnCheckedPlugins()}
     */
    private String unCheckedPluginList;

    /** The plugin manager. */
    private PluginManager pluginManager;

    /** The phase to lifecycle map. */
    private Map<String, Lifecycle> phaseToLifecycleMap;

    /** The lifecycles. */
    private Collection<Lifecycle> lifecycles;

    /** The factory. */
    private ArtifactFactory factory;

    /** The resolver. */
    private ArtifactResolver resolver;

    /** The local. */
    private ArtifactRepository local;

    /** The log. */
    private Log log;

    /** The session. */
    private MavenSession session;

    /** The utils. */
    private EnforcerRuleUtils utils;

    private RuntimeInformation runtimeInformation;

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        this.log = helper.getLog();
        this.helper = helper;

        MavenProject project;
        try
        {
            // get the various expressions out of the helper.

            project = (MavenProject) helper.evaluate( "${project}" );

            runtimeInformation = helper.getComponent( RuntimeInformation.class );

            DefaultLifecycles defaultLifeCycles = helper.getComponent( DefaultLifecycles.class );
            lifecycles = defaultLifeCycles.getLifeCycles();

            session = (MavenSession) helper.evaluate( "${session}" );
            pluginManager = helper.getComponent( PluginManager.class );
            factory = helper.getComponent( ArtifactFactory.class );
            resolver = helper.getComponent( ArtifactResolver.class );
            local = (ArtifactRepository) helper.evaluate( "${localRepository}" );

            utils = new EnforcerRuleUtils( helper );

            // get all the plugins that are bound to the specified lifecycles
            Set<Plugin> allPlugins = getBoundPlugins( project, phases );

            // insert any additional plugins specified by the user.
            allPlugins = addAdditionalPlugins( allPlugins, additionalPlugins );
            allPlugins.addAll( getProfilePlugins( project ) );

            // pull out any we should skip
            allPlugins =
                removeUncheckedPlugins( combineUncheckedPlugins( unCheckedPlugins, unCheckedPluginList ), allPlugins );

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
            List<PluginWrapper> pluginWrappers = getAllPluginEntries( project );

            for ( PluginWrapper pluginWrapper : pluginWrappers )
            {
                log.debug( "pluginWrappers: " + pluginWrapper.getGroupId() + ":" + pluginWrapper.getArtifactId() + ":"
                    + pluginWrapper.getVersion() + " source: " + pluginWrapper.getSource() );
            }
            // now look for the versions that aren't valid and add to a list.
            List<Plugin> failures = new ArrayList<Plugin>();

            for ( Plugin plugin : allPlugins )
            {
                if ( !hasValidVersionSpecified( helper, plugin, pluginWrappers ) )
                {
                    failures.add( plugin );
                }
            }

            // if anything was found, log it then append the optional message.
            if ( !failures.isEmpty() )
            {
                handleMessagesToTheUser( project, failures );
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
        catch ( Exception e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage(), e );
        }
    }

    private void handleMessagesToTheUser( MavenProject project, List<Plugin> failures )
        throws EnforcerRuleException
    {
        StringBuilder newMsg = new StringBuilder();
        newMsg.append( "Some plugins are missing valid versions or depend on Maven "
            + runtimeInformation.getMavenVersion() + " defaults:" );
        handleBanMessages( newMsg );
        newMsg.append( "\n" );
        for ( Plugin plugin : failures )
        {
            newMsg.append( plugin.getGroupId() );
            newMsg.append( ":" );
            newMsg.append( plugin.getArtifactId() );

            try
            {
                newMsg.append( ". \tThe version currently in use is " );

                Plugin currentPlugin = findCurrentPlugin( plugin, project );

                if ( currentPlugin == null )
                {
                    newMsg.append( "unknown" );
                }
                else
                {
                    newMsg.append( currentPlugin.getVersion() );

                    if ( PluginWrapper.isVersionFromDefaultLifecycleBindings( currentPlugin ).orElse( false ) )
                    {
                        newMsg.append( " via default lifecycle bindings" );
                    }
                    else 
                    {
                        String msg = PluginWrapper.isVersionFromSuperpom( currentPlugin )
                                        .filter( b -> b )
                                        .map( t -> " via super POM" )
                                        // for Maven 3.6.0 or before (MNG-6593 / MNG-6600)
                                        .orElse( " via super POM or default lifecycle bindings" );
                        newMsg.append( msg );
                    }
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
        String message = getMessage();
        if ( StringUtils.isNotEmpty( message ) )
        {
            newMsg.append( message );
        }

        throw new EnforcerRuleException( newMsg.toString() );
    }

    private void handleBanMessages( StringBuilder newMsg )
    {
        if ( banLatest || banRelease || banSnapshots || banTimestamps )
        {
            newMsg.append( " (" );
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
            newMsg.append( "are not allowed)" );
        }
    }

    /**
     * Remove the plugins that the user doesn't want to check.
     *
     * @param uncheckedPlugins
     * @param plugins
     * @throws MojoExecutionException
     * @return The plugins which have been removed.
     */
    public Set<Plugin> removeUncheckedPlugins( Collection<String> uncheckedPlugins, Set<Plugin> plugins )
        throws MojoExecutionException
    {
        if ( uncheckedPlugins != null && !uncheckedPlugins.isEmpty() )
        {
            for ( String pluginKey : uncheckedPlugins )
            {
                Plugin plugin = parsePluginString( pluginKey, "UncheckedPlugins" );
                plugins.remove( plugin );
            }
        }
        return plugins;
    }

    /**
     * Combines the old Collection with the new comma separated list.
     * 
     * @param uncheckedPlugins
     * @param uncheckedPluginsList
     * @return List of unchecked plugins.
     */
    // CHECKSTYLE_OFF: LineLength
    public Collection<String> combineUncheckedPlugins( Collection<String> uncheckedPlugins,
                                                       String uncheckedPluginsList )
    // CHECKSTYLE_ON: LineLength
    {
        // if the comma list is empty, then there's nothing to do here.
        if ( StringUtils.isNotEmpty( uncheckedPluginsList ) )
        {
            // make sure there is a collection to add to.
            if ( uncheckedPlugins == null )
            {
                uncheckedPlugins = new HashSet<String>();
            }
            else if ( !uncheckedPlugins.isEmpty() && log != null )
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
    public Set<Plugin> addAdditionalPlugins( Set<Plugin> existing, List<String> additional )
        throws MojoExecutionException
    {
        if ( additional != null )
        {
            for ( String pluginString : additional )
            {
                Plugin plugin = parsePluginString( pluginString, "AdditionalPlugins" );

                if ( existing == null )
                {
                    existing = new HashSet<>();
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
     * @param field
     * @throws MojoExecutionException
     * @return the plugin
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
    public Set<Plugin> getProfilePlugins( MavenProject project )
    {
        Set<Plugin> result = new HashSet<>();
        List<Profile> profiles = project.getActiveProfiles();
        if ( profiles != null && !profiles.isEmpty() )
        {
            for ( Profile p : profiles )
            {
                BuildBase b = p.getBuild();
                if ( b != null )
                {
                    List<Plugin> plugins = b.getPlugins();
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
            Map<String, Plugin> plugins = model.getBuild().getPluginsAsMap();
            found = plugins.get( plugin.getKey() );
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

        List<ArtifactRepository> pluginRepositories = project.getPluginArtifactRepositories();
        Artifact artifact = factory.createPluginArtifact( plugin.getGroupId(), plugin.getArtifactId(),
                                                          VersionRange.createFromVersion( "LATEST" ) );

        try
        {
            this.resolver.resolve( artifact, pluginRepositories, this.local );
            plugin.setVersion( artifact.getVersion() );
        }
        catch ( ArtifactResolutionException | ArtifactNotFoundException e )
        {
            // What does this mean?
        }

        return plugin;
    }

    /**
     * Gets the plugins that are bound to the defined phases. This does not find plugins bound in the pom to a phase
     * later than the plugin is executing.
     *
     * @param project the project
     * @param thePhases the phases
     * @return the bound plugins
     * @throws PluginNotFoundException the plugin not found exception
     * @throws LifecycleExecutionException the lifecycle execution exception
     * @throws IllegalAccessException the illegal access exception
     */
    protected Set<Plugin> getBoundPlugins( MavenProject project, String thePhases )
        throws PluginNotFoundException, LifecycleExecutionException, IllegalAccessException
    {

        Set<Plugin> allPlugins = new HashSet<>();

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
                    log.debug( "getBoundPlugins(): " + project.getId() + " " + lifecyclePhase + " "
                        + lifecycle.getId() );
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

    /**
     * Checks for valid version specified. Checks to see if the version is specified for the plugin. Can optionally ban
     * "RELEASE" or "LATEST" even if specified.
     *
     * @param helper the helper
     * @param source the source
     * @param pluginWrappers the plugins
     * @return true, if successful
     */
    protected boolean hasValidVersionSpecified( EnforcerRuleHelper helper, Plugin source,
                                                List<PluginWrapper> pluginWrappers )
    {
        boolean found = false;
        boolean status = false;
        for ( PluginWrapper plugin : pluginWrappers )
        {
            // find the matching plugin entry
            if ( isMatchingPlugin( source, plugin ) )
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

                if ( isValidVersion( version ) )
                {
                    helper.getLog().debug( "checking for notEmpty and notIsWhitespace(): " + version );
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
            helper.getLog().debug( "plugin " + source.getGroupId() + ":" + source.getArtifactId() + " not found" );
        }
        return status;
    }

    private boolean isValidVersion( String version )
    {
        return StringUtils.isNotEmpty( version ) && !StringUtils.isWhitespace( version );
    }

    private boolean isMatchingPlugin( Plugin source, PluginWrapper plugin )
    {
        return source.getArtifactId().equals( plugin.getArtifactId() )
            && source.getGroupId().equals( plugin.getGroupId() );
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
    private Set<Plugin> getAllPlugins( MavenProject project, Lifecycle lifecycle )
        throws PluginNotFoundException, LifecycleExecutionException

    {
        log.debug( "RequirePluginVersions.getAllPlugins:" );

        Set<Plugin> plugins = new HashSet<>();
        // first, bind those associated with the packaging
        Map<String, String> mappings = findMappingsForLifecycle( project, lifecycle );

        for ( Map.Entry<String, String> entry : mappings.entrySet() )
        {
            log.debug( "  lifecycleMapping = " + entry.getKey() );
            String pluginsForLifecycle = (String) entry.getValue();
            log.debug( "  plugins = " + pluginsForLifecycle );
            if ( StringUtils.isNotEmpty( pluginsForLifecycle ) )
            {
                String pluginList[] = pluginsForLifecycle.split( "," );
                for ( String plugin : pluginList )
                {
                    plugin = StringUtils.strip( plugin );
                    log.debug( "    plugin = " + plugin );
                    String tokens[] = plugin.split( ":" );
                    log.debug( "    GAV = " + Arrays.asList( tokens ) );

                    Plugin p = new Plugin();
                    p.setGroupId( tokens[0] );
                    p.setArtifactId( tokens[1] );
                    plugins.add( p );
                }
            }
        }

        plugins.addAll( project.getBuildPlugins() );

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
    public Map<String, Lifecycle> getPhaseToLifecycleMap()
        throws LifecycleExecutionException
    {
        if ( phaseToLifecycleMap == null )
        {
            phaseToLifecycleMap = new HashMap<>();

            for ( Lifecycle lifecycle : lifecycles )
            {
                List<String> phases = lifecycle.getPhases();
                for ( String phase : phases )
                {
                    log.debug( "getPhaseToLifecycleMap(): phase: " + phase );
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
        Lifecycle lifecycle = getPhaseToLifecycleMap().get( phase );

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
    private Map<String, String> findMappingsForLifecycle( MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        String packaging = project.getPackaging();
        Map<String, String> mappings = null;

        LifecycleMapping m = (LifecycleMapping) findExtension( project, LifecycleMapping.ROLE, packaging,
                                                               session.getSettings(), session.getLocalRepository() );
        if ( m != null )
        {
            mappings = m.getPhases( lifecycle.getId() );
        }

        Map<String, String> defaultMappings = lifecycle.getDefaultPhases();

        if ( mappings == null )
        {
            try
            {
                m = helper.getComponent( LifecycleMapping.class, packaging );
                mappings = m.getPhases( lifecycle.getId() );
            }
            catch ( ComponentLookupException e )
            {
                if ( defaultMappings == null )
                {
                    throw new LifecycleExecutionException( "Cannot find lifecycle mapping for packaging: \'" + packaging
                        + "\'.", e );
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

        List<Plugin> buildPlugins = project.getBuildPlugins();
        for ( Plugin plugin : buildPlugins )
        {
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

                    if ( pluginComponent != null )
                    {
                        break;
                    }
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
        catch ( PluginVersionResolutionException | InvalidVersionSpecificationException | InvalidPluginException
            | ArtifactNotFoundException | ArtifactResolutionException | PluginVersionNotFoundException e )
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
    protected List<PluginWrapper> getAllPluginEntries( MavenProject project )
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        List<PluginWrapper> plugins = new ArrayList<>();
        // now find all the plugin entries, either in
        // build.plugins or build.pluginManagement.plugins, profiles.plugins and reporting

        getPlugins( plugins, project.getModel() );
        getReportingPlugins( plugins, project.getModel() );
        getPluginManagementPlugins( plugins, project.getModel() );
        addPluginsInProfiles( plugins, project.getModel() );

        return plugins;
    }


    private void addPluginsInProfiles( List<PluginWrapper> plugins, Model model )
    {
        List<Profile> profiles = model.getProfiles();
        for ( Profile profile : profiles )
        {
            getProfilePlugins( plugins, model, profile );
            getProfileReportingPlugins( plugins, model, profile );
            getProfilePluginManagementPlugins( plugins, model, profile );
        }
    }

    private void getProfilePluginManagementPlugins( List<PluginWrapper> plugins, Model model, Profile profile )
    {
        try
        {
            List<Plugin> modelPlugins = profile.getBuild().getPluginManagement().getPlugins();
            plugins.addAll( PluginWrapper.addAll( utils.resolvePlugins( modelPlugins ), banMavenDefaults ) );
        }
        catch ( NullPointerException e )
        {
            // guess there are no plugins here.
        }
    }

    private void getProfileReportingPlugins( List<PluginWrapper> plugins, Model model, Profile profile )
    {
        try
        {
            List<ReportPlugin> modelReportPlugins = profile.getReporting().getPlugins();
            // add the reporting plugins
            plugins.addAll( PluginWrapper.addAll( utils.resolveReportPlugins( modelReportPlugins ),
                                                  banMavenDefaults ) );
        }
        catch ( NullPointerException e )
        {
            // guess there are no plugins here.
        }
    }

    private void getProfilePlugins( List<PluginWrapper> plugins, Model model, Profile profile )
    {
        try
        {
            List<Plugin> modelPlugins = profile.getBuild().getPlugins();
            plugins.addAll( PluginWrapper.addAll( utils.resolvePlugins( modelPlugins ), banMavenDefaults ) );
        }
        catch ( NullPointerException e )
        {
            // guess there are no plugins here.
        }
    }

    private void getPlugins( List<PluginWrapper> plugins, Model model )
    {
        try
        {
            List<Plugin> modelPlugins = model.getBuild().getPlugins();
            plugins.addAll( PluginWrapper.addAll( utils.resolvePlugins( modelPlugins ), banMavenDefaults ) );
        }
        catch ( NullPointerException e )
        {
            // guess there are no plugins here.
        }
    }

    private void getPluginManagementPlugins( List<PluginWrapper> plugins, Model model )
    {
        try
        {
            List<Plugin> modelPlugins = model.getBuild().getPluginManagement().getPlugins();
            plugins.addAll( PluginWrapper.addAll( utils.resolvePlugins( modelPlugins ), banMavenDefaults ) );
        }
        catch ( NullPointerException e )
        {
            // guess there are no plugins here.
        }
    }

    private void getReportingPlugins( List<PluginWrapper> plugins, Model model )
    {
        try
        {
            List<ReportPlugin> modelReportPlugins = model.getReporting().getPlugins();
            // add the reporting plugins
            plugins.addAll( PluginWrapper.addAll( utils.resolveReportPlugins( modelReportPlugins ),
                                                  banMavenDefaults ) );
        }
        catch ( NullPointerException e )
        {
            // guess there are no plugins here.
        }
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

    public List<String> getUnCheckedPlugins()
    {
        return unCheckedPlugins;
    }

    public void setUnCheckedPlugins( List<String> unCheckedPlugins )
    {
        this.unCheckedPlugins = unCheckedPlugins;
    }

    public final void setPhases( String phases )
    {
        this.phases = phases;
    }

    public final String getPhases()
    {
        return phases;
    }

    public final void setAdditionalPlugins( List<String> additionalPlugins )
    {
        this.additionalPlugins = additionalPlugins;
    }

    public final List<String> getAdditionalPlugins()
    {
        return additionalPlugins;
    }
}
