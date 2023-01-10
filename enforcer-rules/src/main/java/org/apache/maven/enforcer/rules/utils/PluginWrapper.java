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
package org.apache.maven.enforcer.rules.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;

/**
 * @author Brian Fox
 *
 */
public class PluginWrapper {
    private final String groupId;

    private final String artifactId;

    private final String version;

    private final InputLocationTracker locationTracker;

    public static List<PluginWrapper> addAll(List<? extends InputLocationTracker> plugins, boolean banMavenDefaults) {
        if (plugins.isEmpty()) {
            return Collections.emptyList();
        }

        List<PluginWrapper> results = new ArrayList<>(plugins.size());
        for (InputLocationTracker o : plugins) {
            // null or true means it is most assumed a Maven default
            if (banMavenDefaults
                    && (isVersionFromDefaultLifecycleBindings(o).orElse(true)
                            || isVersionFromSuperpom(o).orElse(true))) {
                continue;
            }

            if (o instanceof Plugin) {
                results.add(new PluginWrapper((Plugin) o));
            } else {
                if (o instanceof ReportPlugin) {
                    results.add(new PluginWrapper((ReportPlugin) o));
                }
            }
        }
        return results;
    }

    /**
     * Whether the version is coming from the default lifecycle bindings.
     * Cannot be determined before Maven 3.6.1
     *
     * @param o either Plugin or ReportPlugin
     * @return null if untraceable, otherwise its matching value
     * @see <a href="https://issues.apache.org/jira/browse/MNG-6600">MNG-6600</a>
     */
    public static Optional<Boolean> isVersionFromDefaultLifecycleBindings(InputLocationTracker o) {
        InputLocation versionLocation = o.getLocation("version");
        if (versionLocation == null) {
            return Optional.empty();
        }

        String modelId = versionLocation.getSource().getModelId();
        return Optional.of(
                modelId.startsWith("org.apache.maven:maven-core:") && modelId.endsWith(":default-lifecycle-bindings"));
    }

    /**
     * Whether the version is coming from the super POM.
     * Cannot be determined before Maven 3.6.1
     *
     * @param o either Plugin or ReportPlugin
     * @return null if untraceable, otherwise its matching value
     * @see <a href="https://issues.apache.org/jira/browse/MNG-6593">MNG-6593</a>
     */
    public static Optional<Boolean> isVersionFromSuperpom(InputLocationTracker o) {
        InputLocation versionLocation = o.getLocation("version");
        if (versionLocation == null) {
            return Optional.empty();
        }

        String modelId = versionLocation.getSource().getModelId();
        return Optional.of(
                modelId.startsWith("org.apache.maven:maven-model-builder:") && modelId.endsWith(":super-pom"));
    }

    private PluginWrapper(Plugin plugin) {
        this.groupId = plugin.getGroupId();
        this.artifactId = plugin.getArtifactId();
        this.version = plugin.getVersion();
        this.locationTracker = plugin;
    }

    private PluginWrapper(ReportPlugin plugin) {
        this.groupId = plugin.getGroupId();
        this.artifactId = plugin.getArtifactId();
        this.version = plugin.getVersion();
        this.locationTracker = plugin;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getSource() {
        InputLocation inputLocation = locationTracker.getLocation("version");

        if (inputLocation == null) {
            // most likely super-pom or default-lifecycle-bindings in Maven 3.6.0 or before (MNG-6593 / MNG-6600)
            return "unknown";
        } else {
            return inputLocation.getSource().getLocation();
        }
    }
}
