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
package org.apache.maven.extensions.enforcer;

import javax.inject.Named;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Extends every MavenProject with the maven-enforcer-plugin, adding executions as defined in
 * <code>.mvn/enforcer-extension.xml</code>
 *
 * @since 3.0.0
 */
@Named("enforcer")
public class EnforceExtension extends AbstractMavenLifecycleParticipant {
    private static final String ENFORCER_EXTENSION_XML = ".mvn/enforcer-extension.xml";

    private static final String POM_PROPERTIES =
            "/META-INF/maven/org.apache.maven.extensions/maven-enforcer-extension/pom.properties";

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        Xpp3Dom configuration;
        Path config = Paths.get(session.getExecutionRootDirectory(), ENFORCER_EXTENSION_XML);
        if (Files.isRegularFile(config)) {
            try (Reader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
                configuration = Xpp3DomBuilder.build(reader);
            } catch (XmlPullParserException | IOException e) {
                throw new MavenExecutionException("Failed to read " + ENFORCER_EXTENSION_XML, e);
            }
        } else {
            return;
        }

        List<PluginExecution> executions = null;
        Xpp3Dom executionsDom = configuration.getChild("executions");
        if (executionsDom != null) {
            executions = new ArrayList<>();
            for (Xpp3Dom executionDom : executionsDom.getChildren("execution")) {
                executions.add(getPluginExecution(executionDom));
            }
        }

        if (executions == null) {
            return;
        }

        for (MavenProject project : session.getProjects()) {
            Plugin enforcerPlugin = null;
            for (Plugin plugin : project.getBuildPlugins()) {
                if ("maven-enforcer-plugin".equals(plugin.getArtifactId())
                        && "org.apache.maven.plugins".equals(plugin.getGroupId())) {
                    enforcerPlugin = plugin;
                }
            }

            if (enforcerPlugin == null) {
                enforcerPlugin = new Plugin();
                enforcerPlugin.setGroupId("org.apache.maven.plugins");
                enforcerPlugin.setArtifactId("maven-enforcer-plugin");

                try (InputStream is = EnforceExtension.class.getResourceAsStream(POM_PROPERTIES)) {
                    Properties properties = new Properties();
                    properties.load(is);
                    enforcerPlugin.setVersion(properties.getProperty("version"));
                } catch (IOException e) {
                    // noop
                }

                if (project.getBuildPlugins().isEmpty()) {
                    Build build = project.getBuild();
                    if (build == null) {
                        build = new Build();
                        project.setBuild(build);
                    }
                    build.setPlugins(Collections.singletonList(enforcerPlugin));
                } else {
                    List<Plugin> buildPlugins = new ArrayList<>(project.getBuildPlugins());
                    buildPlugins.add(enforcerPlugin);
                    project.getBuild().setPlugins(buildPlugins);
                }
            }

            for (PluginExecution pe : executions) {
                enforcerPlugin.addExecution(pe);
            }
        }
    }

    private static PluginExecution getPluginExecution(Xpp3Dom execution) {
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setId(get(execution, "id", "default-extension"));
        pluginExecution.addGoal("enforce");
        pluginExecution.setPhase(get(execution, "phase", "validate"));
        // here we must use Mavens internal configuration implementation
        pluginExecution.setConfiguration(execution.getChild("configuration"));
        return pluginExecution;
    }

    private static String get(Xpp3Dom elm, String name, String defaultValue) {
        if (elm == null || elm.getChild(name) == null) {
            return defaultValue;
        } else {
            return elm.getChild(name).getValue();
        }
    }
}
