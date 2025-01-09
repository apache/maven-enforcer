/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.enforcer.rules;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.BuildFailureException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.EnforcerRuleUtils;
import org.apache.maven.enforcer.rules.utils.ExpressionEvaluator;
import org.apache.maven.enforcer.rules.utils.PluginWrapper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginConfiguration;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import static java.util.Optional.ofNullable;

/**
 * This rule will enforce that all plugins specified in the poms have a version declared.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@Named("requirePluginVersions")
public final class RequirePluginVersions extends AbstractStandardEnforcerRule {

    /**
     * Don't allow the LATEST identifier.
     */
    private boolean banLatest = true;

    /**
     * Don't allow the RELEASE identifier.
     */
    private boolean banRelease = true;

    /**
     * Don't allow snapshot plugins.
     */
    private boolean banSnapshots = true;

    /**
     * Don't allow timestamp snapshot plugins.
     */
    private boolean banTimestamps = true;

    /**
     * @since 3.0.0
     */
    private boolean banMavenDefaults = true;

    /**
     * The comma separated list of phases that should be used to find lifecycle plugin bindings. The default value is
     * "clean,deploy,site".
     */
    private String phases = "clean,deploy,site";

    /**
     * Additional plugins to enforce have versions. These are plugins that may not be in the poms but are used anyway,
     * like help, eclipse etc. <br>
     * The plugins should be specified in the form: <code>group:artifactId</code>.
     */
    private List<String> additionalPlugins;

    /**
     * Plugins to skip for version enforcement. The plugins should be specified in the form:
     * <code>group:artifactId</code>. NOTE: This is deprecated, use unCheckedPluginList instead.
     */
    private List<String> unCheckedPlugins;

    /**
     * Same as unCheckedPlugins but as a comma list to better support properties. Sample form:
     * <code>group:artifactId,group2:artifactId2</code>
     *
     * @since 1.0-beta-1
     */
    private String unCheckedPluginList;

    /** The phase to lifecycle map. */
    private Map<String, Lifecycle> phaseToLifecycleMap;

    /** The lifecycles. */
    private Collection<Lifecycle> lifecycles;

    /** The plugin manager. */
    private final PluginManager pluginManager;

    /** The factory. */
    private final ArtifactFactory factory;

    private final RepositorySystem repositorySystem;

    /** The session. */
    private final MavenSession session;

    /** The utils. */
    private final EnforcerRuleUtils utils;

    private final RuntimeInformation runtimeInformation;

    private final DefaultLifecycles defaultLifeCycles;

    private final MavenProject project;

    private final ExpressionEvaluator evaluator;

    private final PlexusContainer container;

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Inject
    public RequirePluginVersions(
            PluginManager pluginManager,
            ArtifactFactory factory,
            RepositorySystem repositorySystem,
            MavenSession session,
            EnforcerRuleUtils utils,
            RuntimeInformation runtimeInformation,
            DefaultLifecycles defaultLifeCycles,
            MavenProject project,
            ExpressionEvaluator evaluator,
            PlexusContainer container) {
        this.pluginManager = Objects.requireNonNull(pluginManager);
        this.factory = Objects.requireNonNull(factory);
        this.repositorySystem = Objects.requireNonNull(repositorySystem);
        this.session = Objects.requireNonNull(session);
        this.utils = Objects.requireNonNull(utils);
        this.runtimeInformation = Objects.requireNonNull(runtimeInformation);
        this.defaultLifeCycles = Objects.requireNonNull(defaultLifeCycles);
        this.project = Objects.requireNonNull(project);
        this.evaluator = Objects.requireNonNull(evaluator);
        this.container = Objects.requireNonNull(container);
    }

    @Override
    public void execute() throws EnforcerRuleException {

        try {
            // get the various expressions out of the helper.

            lifecycles = defaultLifeCycles.getLifeCycles();

            // get all the plugins that are bound to the specified lifecycles
            Set<Plugin> allPlugins = getBoundPlugins(project, phases);

            // insert any additional plugins specified by the user.
            allPlugins = addAdditionalPlugins(allPlugins, additionalPlugins);
            allPlugins.addAll(getProfilePlugins(project));

            // pull out any we should skip
            allPlugins =
                    removeUncheckedPlugins(combineUncheckedPlugins(unCheckedPlugins, unCheckedPluginList), allPlugins);

            // there's nothing to do here
            if (allPlugins.isEmpty()) {
                getLog().info("No plugin bindings found.");
                return;
            } else {
                getLog().debug("All Plugins in use: " + allPlugins);
            }

            // get all the plugins that are mentioned in the pom (and parents)
            List<PluginWrapper> pluginWrappers = getAllPluginEntries(project);

            for (PluginWrapper pluginWrapper : pluginWrappers) {
                getLog().debug("pluginWrappers: " + pluginWrapper.getGroupId() + ":" + pluginWrapper.getArtifactId()
                        + ":" + pluginWrapper.getVersion() + " source: " + pluginWrapper.getSource());
            }
            // now look for the versions that aren't valid and add to a list.
            List<Plugin> failures = new ArrayList<>();

            for (Plugin plugin : allPlugins) {
                if (!hasValidVersionSpecified(plugin, pluginWrappers)) {
                    failures.add(plugin);
                }
            }

            // if anything was found, log it then append the optional message.
            if (!failures.isEmpty()) {
                handleMessagesToTheUser(project, failures);
            }
        } catch (PluginNotFoundException | LifecycleExecutionException e) {
            throw new EnforcerRuleException(e.getLocalizedMessage(), e);
        }
    }

    private void handleMessagesToTheUser(MavenProject project, List<Plugin> failures) throws EnforcerRuleException {
        StringBuilder newMsg = new StringBuilder();
        newMsg.append("Some plugins are missing valid versions or depend on Maven ");
        newMsg.append(runtimeInformation.getMavenVersion());
        newMsg.append(" defaults");
        handleBanMessages(newMsg);
        newMsg.append(System.lineSeparator());
        for (Plugin plugin : failures) {
            newMsg.append("   ");
            newMsg.append(plugin.getGroupId());
            newMsg.append(":");
            newMsg.append(plugin.getArtifactId());

            try {
                newMsg.append(". \tThe version currently in use is ");

                Plugin currentPlugin = findCurrentPlugin(plugin, project);

                if (currentPlugin == null) {
                    newMsg.append("unknown");
                } else {
                    newMsg.append(currentPlugin.getVersion());

                    if (PluginWrapper.isVersionFromDefaultLifecycleBindings(currentPlugin)
                            .orElse(false)) {
                        newMsg.append(" via default lifecycle bindings");
                    } else {
                        String msg = PluginWrapper.isVersionFromSuperpom(currentPlugin)
                                .filter(b -> b)
                                .map(t -> " via super POM")
                                // for Maven 3.6.0 or before (MNG-6593 / MNG-6600)
                                .orElse(" via super POM or default lifecycle bindings");
                        newMsg.append(msg);
                    }
                }
            } catch (Exception e) {
                // lots can go wrong here. Don't allow any issues trying to
                // determine the issue stop me
                getLog().debug("Exception while determining plugin Version " + e.getMessage());
                newMsg.append(". Unable to determine the plugin version.");
            }
            newMsg.append(System.lineSeparator());
        }
        String message = getMessage();
        if (message != null && !message.isEmpty()) {
            newMsg.append(message);
        }

        throw new EnforcerRuleException(newMsg.toString());
    }

    private void handleBanMessages(StringBuilder newMsg) {
        if (banLatest || banRelease || banSnapshots || banTimestamps) {
            List<String> banList = new ArrayList<>();
            if (banLatest) {
                banList.add("LATEST");
            }
            if (banRelease) {
                banList.add("RELEASE");
            }
            if (banSnapshots) {
                banList.add("SNAPSHOT");
                if (banTimestamps) {
                    banList.add("TIMESTAMP SNAPSHOT");
                }
            }
            if (!banList.isEmpty()) {
                newMsg.append(" (");
                newMsg.append(String.join(", ", banList));
                newMsg.append(" as plugin version are not allowed)");
            }
        }
    }

    /**
     * Remove the plugins that the user doesn't want to check.
     *
     * @param uncheckedPlugins
     * @param plugins
     * @return The plugins which have been removed.
     */
    Set<Plugin> removeUncheckedPlugins(Collection<String> uncheckedPlugins, Set<Plugin> plugins)
            throws EnforcerRuleError {
        if (uncheckedPlugins != null && !uncheckedPlugins.isEmpty()) {
            for (String pluginKey : uncheckedPlugins) {
                Plugin plugin = parsePluginString(pluginKey, "UncheckedPlugins");
                plugins.remove(plugin);
            }
        }
        return plugins;
    }

    /**
     * Combines the old Collection with the new comma separated list.
     *
     * @param uncheckedPlugins     a new collections
     * @param uncheckedPluginsList a list to merge
     * @return List of unchecked plugins.
     */
    public Collection<String> combineUncheckedPlugins(
            Collection<String> uncheckedPlugins, String uncheckedPluginsList) {
        // if the comma list is empty, then there's nothing to do here.
        if (uncheckedPluginsList != null && !uncheckedPluginsList.isEmpty()) {
            // make sure there is a collection to add to.
            if (uncheckedPlugins == null) {
                uncheckedPlugins = new HashSet<>();
            } else if (!uncheckedPlugins.isEmpty()) {
                getLog().warn("The parameter 'unCheckedPlugins' is deprecated. Use 'unCheckedPluginList' instead");
            }

            uncheckedPlugins.addAll(Arrays.asList(uncheckedPluginsList.split(",")));
        }
        return uncheckedPlugins;
    }

    /**
     * Add the additional plugins if they don't exist yet.
     *
     * @param existing   the existing
     * @param additional the additional
     * @return the sets the
     * @throws EnforcerRuleError the enforcer error
     */
    public Set<Plugin> addAdditionalPlugins(Set<Plugin> existing, List<String> additional) throws EnforcerRuleError {
        if (additional != null) {
            for (String pluginString : additional) {
                Plugin plugin = parsePluginString(pluginString, "AdditionalPlugins");

                if (existing == null) {
                    existing = new HashSet<>();
                    existing.add(plugin);
                } else if (!existing.contains(plugin)) {
                    existing.add(plugin);
                }
            }
        }
        return existing;
    }

    /**
     * Helper method to parse and inject a Plugin.
     *
     * @param pluginString a plugin description to parse
     * @param field        a source of pluginString
     * @return the prepared plugin
     */
    private Plugin parsePluginString(String pluginString, String field) throws EnforcerRuleError {
        if (pluginString != null) {
            String[] pluginStrings = pluginString.split(":");
            if (pluginStrings.length == 2) {
                Plugin plugin = new Plugin();
                plugin.setGroupId(StringUtils.strip(pluginStrings[0]));
                plugin.setArtifactId(StringUtils.strip(pluginStrings[1]));

                return plugin;
            } else {
                throw new EnforcerRuleError("Invalid " + field + " string: " + pluginString);
            }
        } else {
            throw new EnforcerRuleError("Invalid " + field + " string: " + pluginString);
        }
    }

    /**
     * Finds the plugins that are listed in active profiles.
     *
     * @param project the project
     * @return the profile plugins
     */
    public Set<Plugin> getProfilePlugins(MavenProject project) {
        Set<Plugin> result = new HashSet<>();
        List<Profile> profiles = project.getActiveProfiles();
        if (profiles != null && !profiles.isEmpty()) {
            for (Profile p : profiles) {
                BuildBase b = p.getBuild();
                if (b != null) {
                    List<Plugin> plugins = b.getPlugins();
                    if (plugins != null) {
                        result.addAll(plugins);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Given a plugin, this will retrieve the matching plugin artifact from the model.
     *
     * @param plugin  plugin to lookup
     * @param project project to search
     * @return matching plugin, <code>null</code> if not found.
     */
    private Plugin findCurrentPlugin(Plugin plugin, MavenProject project) throws EnforcerRuleException {
        Plugin found = null;
        try {
            Model model = project.getModel();
            Map<String, Plugin> plugins = model.getBuild().getPluginsAsMap();
            found = plugins.get(plugin.getKey());
        } catch (NullPointerException e) {
            // nothing to do here
        }

        if (found == null) {
            Artifact artifact = factory.createPluginArtifact(
                    plugin.getGroupId(), plugin.getArtifactId(), VersionRange.createFromVersion("LATEST"));

            try {
                repositorySystem.resolveArtifact(
                        session.getRepositorySession(),
                        new ArtifactRequest(
                                RepositoryUtils.toArtifact(artifact),
                                session.getCurrentProject().getRemotePluginRepositories(),
                                "resolvePlugin"));
            } catch (ArtifactResolutionException e) {
                throw new EnforcerRuleException("Unable to resolve the plugin " + artifact.getArtifactId(), e);
            }
            plugin.setVersion(artifact.getVersion());

            found = plugin;
        }

        return found;
    }

    /**
     * Gets the plugins that are bound to the defined phases. This does not find plugins bound in the pom to a phase
     * later than the plugin is executing.
     *
     * @param project   the project
     * @param thePhases the phases
     * @return the bound plugins
     * @throws PluginNotFoundException     the plugin not found exception
     * @throws LifecycleExecutionException the lifecycle execution exception
     */
    private Set<Plugin> getBoundPlugins(MavenProject project, String thePhases)
            throws PluginNotFoundException, LifecycleExecutionException {

        Set<Plugin> allPlugins = new HashSet<>();

        // lookup the bindings for all the passed in phases
        String[] lifecyclePhases = thePhases.split(",");
        for (int i = 0; i < lifecyclePhases.length; i++) {
            String lifecyclePhase = lifecyclePhases[i];
            if (lifecyclePhase != null && !lifecyclePhase.isEmpty()) {
                try {
                    Lifecycle lifecycle = getLifecycleForPhase(lifecyclePhase);
                    getLog().debug("getBoundPlugins(): " + project.getId() + " " + lifecyclePhase + " "
                            + lifecycle.getId());
                    allPlugins.addAll(getAllPlugins(project, lifecycle));
                } catch (BuildFailureException e) {
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
     * @param source         the source
     * @param pluginWrappers the plugins
     * @return true, if successful
     */
    public boolean hasValidVersionSpecified(Plugin source, List<PluginWrapper> pluginWrappers) {
        boolean found = false;
        boolean status = false;
        for (PluginWrapper plugin : pluginWrappers) {
            // find the matching plugin entry
            if (isMatchingPlugin(source, plugin)) {
                found = true;
                // found the entry. now see if the version is specified
                String version = plugin.getVersion();
                try {
                    version = (String) evaluator.evaluate(version);
                } catch (ExpressionEvaluationException e) {
                    return false;
                }

                if (isValidVersion(version)) {
                    getLog().debug("checking for notEmpty and notIsWhitespace(): " + version);
                    if (banRelease && version.equals("RELEASE")) {
                        return false;
                    }

                    if (banLatest && version.equals("LATEST")) {
                        return false;
                    }

                    if (banSnapshots && isSnapshot(version)) {
                        return false;
                    }
                    // the version was specified and not
                    // banned. It's ok. Keep looking through the list to make
                    // sure it's not using a banned version somewhere else.

                    status = true;

                    if (!banRelease && !banLatest && !banSnapshots) {
                        // no need to keep looking
                        break;
                    }
                }
            }
        }
        if (!found) {
            getLog().debug("plugin " + source.getGroupId() + ":" + source.getArtifactId() + " not found");
        }
        return status;
    }

    private boolean isValidVersion(String version) {
        return (version != null && !version.isEmpty()) && !StringUtils.isWhitespace(version);
    }

    private boolean isMatchingPlugin(Plugin source, PluginWrapper plugin) {
        return source.getArtifactId().equals(plugin.getArtifactId())
                && source.getGroupId().equals(plugin.getGroupId());
    }

    /**
     * Checks if is snapshot.
     *
     * @param baseVersion the base version
     * @return true, if is snapshot
     */
    private boolean isSnapshot(String baseVersion) {
        if (banTimestamps) {
            return Artifact.VERSION_FILE_PATTERN.matcher(baseVersion).matches()
                    || baseVersion.endsWith(Artifact.SNAPSHOT_VERSION);
        } else {
            return baseVersion.endsWith(Artifact.SNAPSHOT_VERSION);
        }
    }

    /*
     * Uses borrowed lifecycle code to get a list of all plugins bound to the lifecycle.
     */

    /**
     * Gets the all plugins.
     *
     * @param project   the project
     * @param lifecycle the lifecycle
     * @return the all plugins
     * @throws PluginNotFoundException     the plugin not found exception
     * @throws LifecycleExecutionException the lifecycle execution exception
     */
    private Set<Plugin> getAllPlugins(MavenProject project, Lifecycle lifecycle)
            throws PluginNotFoundException, LifecycleExecutionException {

        getLog().debug("RequirePluginVersions.getAllPlugins:");

        Set<Plugin> plugins = new HashSet<>();
        // first, bind those associated with the packaging
        Map<String, String> mappings = findMappingsForLifecycle(project, lifecycle);

        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            getLog().debug("  lifecycleMapping = " + entry.getKey());
            String pluginsForLifecycle = (String) entry.getValue();
            getLog().debug("  plugins = " + pluginsForLifecycle);
            if (pluginsForLifecycle != null && !pluginsForLifecycle.isEmpty()) {
                String pluginList[] = pluginsForLifecycle.split(",");
                for (String plugin : pluginList) {
                    plugin = StringUtils.strip(plugin);
                    getLog().debug("    plugin = " + plugin);
                    String tokens[] = plugin.split(":");
                    getLog().debug("    GAV = " + Arrays.asList(tokens));

                    Plugin p = new Plugin();
                    p.setGroupId(tokens[0]);
                    p.setArtifactId(tokens[1]);
                    plugins.add(p);
                }
            }
        }

        plugins.addAll(project.getBuildPlugins());

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
    public Map<String, Lifecycle> getPhaseToLifecycleMap() throws LifecycleExecutionException {
        if (phaseToLifecycleMap == null) {
            phaseToLifecycleMap = new HashMap<>();

            for (Lifecycle lifecycle : lifecycles) {
                List<String> phases = lifecycle.getPhases();
                for (String phase : phases) {
                    getLog().debug("getPhaseToLifecycleMap(): phase: " + phase);
                    if (phaseToLifecycleMap.containsKey(phase)) {
                        Lifecycle prevLifecycle = (Lifecycle) phaseToLifecycleMap.get(phase);
                        throw new LifecycleExecutionException("Phase '" + phase
                                + "' is defined in more than one lifecycle: '" + lifecycle.getId() + "' and '"
                                + prevLifecycle.getId() + "'");
                    } else {
                        phaseToLifecycleMap.put(phase, lifecycle);
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
     * @throws BuildFailureException       the build failure exception
     * @throws LifecycleExecutionException the lifecycle execution exception
     */
    private Lifecycle getLifecycleForPhase(String phase) throws BuildFailureException, LifecycleExecutionException {
        Lifecycle lifecycle = getPhaseToLifecycleMap().get(phase);

        if (lifecycle == null) {
            throw new BuildFailureException("Unable to find lifecycle for phase '" + phase + "'");
        }
        return lifecycle;
    }

    /**
     * Find mappings for lifecycle.
     *
     * @param project   the project
     * @param lifecycle the lifecycle
     * @return the map
     * @throws LifecycleExecutionException the lifecycle execution exception
     * @throws PluginNotFoundException     the plugin not found exception
     */
    private Map<String, String> findMappingsForLifecycle(MavenProject project, Lifecycle lifecycle)
            throws LifecycleExecutionException, PluginNotFoundException {
        String packaging = project.getPackaging();
        Map<String, String> mappings = null;

        LifecycleMapping m = (LifecycleMapping) findExtension(
                project, LifecycleMapping.ROLE, packaging, session.getSettings(), session.getLocalRepository());
        if (m != null) {
            mappings = m.getPhases(lifecycle.getId());
        }

        Map<String, String> defaultMappings = lifecycle.getDefaultPhases();

        if (mappings == null) {
            try {
                m = container.lookup(LifecycleMapping.class, packaging);
                mappings = m.getPhases(lifecycle.getId());
            } catch (ComponentLookupException e) {
                if (defaultMappings == null) {
                    throw new LifecycleExecutionException(
                            "Cannot find lifecycle mapping for packaging: \'" + packaging + "\'.", e);
                }
            }
        }

        if (mappings == null) {
            if (defaultMappings == null) {
                throw new LifecycleExecutionException(
                        "Cannot find lifecycle mapping for packaging: \'" + packaging + "\', and there is no default");
            } else {
                mappings = defaultMappings;
            }
        }

        return mappings;
    }

    /**
     * Find extension.
     *
     * @param project         the project
     * @param role            the role
     * @param roleHint        the role hint
     * @param settings        the settings
     * @param localRepository the local repository
     * @return the object
     * @throws LifecycleExecutionException the lifecycle execution exception
     * @throws PluginNotFoundException     the plugin not found exception
     */
    private Object findExtension(
            MavenProject project, String role, String roleHint, Settings settings, ArtifactRepository localRepository)
            throws LifecycleExecutionException, PluginNotFoundException {
        Object pluginComponent = null;

        List<Plugin> buildPlugins = project.getBuildPlugins();
        for (Plugin plugin : buildPlugins) {
            if (plugin.isExtensions()) {
                verifyPlugin(plugin, project, settings, localRepository);

                // TODO: if moved to the plugin manager we
                // already have the descriptor from above
                // and so do can lookup the container
                // directly
                try {
                    pluginComponent = pluginManager.getPluginComponent(plugin, role, roleHint);

                    if (pluginComponent != null) {
                        break;
                    }
                } catch (ComponentLookupException e) {
                    getLog().debug("Unable to find the lifecycle component in the extension " + e.getMessage());
                } catch (PluginManagerException e) {
                    throw new LifecycleExecutionException(
                            "Error getting extensions from the plugin '" + plugin.getKey() + "': " + e.getMessage(), e);
                }
            }
        }
        return pluginComponent;
    }

    /**
     * Verify plugin.
     *
     * @param plugin          the plugin
     * @param project         the project
     * @param settings        the settings
     * @param localRepository the local repository
     * @return the plugin descriptor
     * @throws LifecycleExecutionException the lifecycle execution exception
     * @throws PluginNotFoundException     the plugin not found exception
     */
    private PluginDescriptor verifyPlugin(
            Plugin plugin, MavenProject project, Settings settings, ArtifactRepository localRepository)
            throws LifecycleExecutionException, PluginNotFoundException {
        PluginDescriptor pluginDescriptor;
        try {
            pluginDescriptor = pluginManager.verifyPlugin(plugin, project, settings, localRepository);
        } catch (PluginManagerException e) {
            throw new LifecycleExecutionException(
                    "Internal error in the plugin manager getting plugin '" + plugin.getKey() + "': " + e.getMessage(),
                    e);
        } catch (PluginVersionResolutionException
                | InvalidVersionSpecificationException
                | InvalidPluginException
                | PluginVersionNotFoundException
                | org.apache.maven.artifact.resolver.ArtifactResolutionException
                | ArtifactNotFoundException e) {
            throw new LifecycleExecutionException(e.getMessage(), e);
        }
        return pluginDescriptor;
    }

    /**
     * Gets all plugin entries in build.plugins, build.pluginManagement.plugins, profile.build.plugins, reporting and
     * profile.reporting in this project and all parents
     *
     * @param project the project
     * @return the all plugin entries wrapped in a PluginWrapper Object
     */
    private List<PluginWrapper> getAllPluginEntries(MavenProject project) {
        List<PluginWrapper> plugins = new ArrayList<>();
        // now find all the plugin entries, either in
        // build.plugins or build.pluginManagement.plugins, profiles.plugins and reporting

        getPlugins(plugins, project.getModel());
        getReportingPlugins(plugins, project.getModel());
        getPluginManagementPlugins(plugins, project.getModel());
        addPluginsInProfiles(plugins, project.getModel());

        return plugins;
    }

    private void addPluginsInProfiles(List<PluginWrapper> plugins, Model model) {
        List<Profile> profiles = ofNullable(model).map(Model::getProfiles).orElseGet(Collections::emptyList);
        for (Profile profile : profiles) {
            getProfilePlugins(plugins, profile);
            getProfileReportingPlugins(plugins, profile);
            getProfilePluginManagementPlugins(plugins, profile);
        }
    }

    private void getProfilePluginManagementPlugins(List<PluginWrapper> plugins, Profile profile) {
        List<Plugin> modelPlugins = ofNullable(profile)
                .map(Profile::getBuild)
                .map(PluginConfiguration::getPluginManagement)
                .map(PluginContainer::getPlugins)
                .orElseGet(Collections::emptyList);
        plugins.addAll(PluginWrapper.addAll(utils.resolvePlugins(modelPlugins), banMavenDefaults));
    }

    private void getProfileReportingPlugins(List<PluginWrapper> plugins, Profile profile) {
        List<ReportPlugin> modelReportPlugins = ofNullable(profile)
                .map(ModelBase::getReporting)
                .map(Reporting::getPlugins)
                .orElseGet(Collections::emptyList);
        // add the reporting plugins
        plugins.addAll(PluginWrapper.addAll(utils.resolveReportPlugins(modelReportPlugins), banMavenDefaults));
    }

    private void getProfilePlugins(List<PluginWrapper> plugins, Profile profile) {
        List<Plugin> modelPlugins = ofNullable(profile)
                .map(Profile::getBuild)
                .map(PluginContainer::getPlugins)
                .orElseGet(Collections::emptyList);
        plugins.addAll(PluginWrapper.addAll(utils.resolvePlugins(modelPlugins), banMavenDefaults));
    }

    private void getPlugins(List<PluginWrapper> plugins, Model model) {
        List<Plugin> modelPlugins = ofNullable(model)
                .map(Model::getBuild)
                .map(PluginContainer::getPlugins)
                .orElseGet(Collections::emptyList);
        plugins.addAll(PluginWrapper.addAll(utils.resolvePlugins(modelPlugins), banMavenDefaults));
    }

    private void getPluginManagementPlugins(List<PluginWrapper> plugins, Model model) {
        List<Plugin> modelPlugins = ofNullable(model)
                .map(Model::getBuild)
                .map(PluginConfiguration::getPluginManagement)
                .map(PluginContainer::getPlugins)
                .orElseGet(Collections::emptyList);
        plugins.addAll(PluginWrapper.addAll(utils.resolvePlugins(modelPlugins), banMavenDefaults));
    }

    private void getReportingPlugins(List<PluginWrapper> plugins, Model model) {
        List<ReportPlugin> modelReportPlugins = ofNullable(model)
                .map(ModelBase::getReporting)
                .map(Reporting::getPlugins)
                .orElseGet(Collections::emptyList);
        // add the reporting plugins
        plugins.addAll(PluginWrapper.addAll(utils.resolveReportPlugins(modelReportPlugins), banMavenDefaults));
    }

    /**
     * Sets the ban latest.
     *
     * @param theBanLatest the banLatest to set
     */
    public void setBanLatest(boolean theBanLatest) {
        this.banLatest = theBanLatest;
    }

    /**
     * Sets the ban release.
     *
     * @param theBanRelease the banRelease to set
     */
    public void setBanRelease(boolean theBanRelease) {
        this.banRelease = theBanRelease;
    }

    /**
     * Checks if is ban snapshots.
     *
     * @return the banSnapshots
     */
    public boolean isBanSnapshots() {
        return this.banSnapshots;
    }

    /**
     * Sets the ban snapshots.
     *
     * @param theBanSnapshots the banSnapshots to set
     */
    public void setBanSnapshots(boolean theBanSnapshots) {
        this.banSnapshots = theBanSnapshots;
    }

    /**
     * Sets the ban timestamps.
     *
     * @param theBanTimestamps the banTimestamps to set
     */
    public void setBanTimestamps(boolean theBanTimestamps) {
        this.banTimestamps = theBanTimestamps;
    }

    @Override
    public String toString() {
        return String.format(
                "RequirePluginVersions[message=%s, banLatest=%b, banRelease=%b, banSnapshots=%b, banTimestamps=%b, phases=%s, additionalPlugins=%s, unCheckedPluginList=%s, unCheckedPlugins=%s]",
                getMessage(),
                banLatest,
                banRelease,
                banSnapshots,
                banTimestamps,
                phases,
                additionalPlugins,
                unCheckedPluginList,
                unCheckedPlugins);
    }
}
