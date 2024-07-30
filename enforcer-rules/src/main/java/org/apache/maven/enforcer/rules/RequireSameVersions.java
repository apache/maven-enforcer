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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * @author Robert Scholte
 * @since 1.3
 */
@Named("requireSameVersions")
public final class RequireSameVersions extends AbstractStandardEnforcerRule {
    private boolean uniqueVersions;

    private Set<String> dependencies = new HashSet<>();

    private Set<String> plugins = new HashSet<>();

    private Set<String> buildPlugins = new HashSet<>();

    private Set<String> reportPlugins = new HashSet<>();

    private boolean sameModuleVersions;

    private final MavenProject project;

    private final MavenSession session;

    @Inject
    public RequireSameVersions(MavenProject project, MavenSession session) {
        this.project = Objects.requireNonNull(project);
        this.session = Objects.requireNonNull(session);
    }

    @Override
    public void execute() throws EnforcerRuleException {

        // consider including profile based artifacts
        Map<String, List<String>> versionMembers = new LinkedHashMap<>();

        Set<String> buildPluginSet = new HashSet<>(buildPlugins);
        buildPluginSet.addAll(plugins);
        Set<String> reportPluginSet = new HashSet<>(reportPlugins);
        reportPluginSet.addAll(plugins);

        // CHECKSTYLE_OFF: LineLength
        versionMembers.putAll(collectVersionMembers(project.getArtifacts(), dependencies, " (dependency)"));
        versionMembers.putAll(collectVersionMembers(project.getPluginArtifacts(), buildPlugins, " (buildPlugin)"));
        versionMembers.putAll(collectVersionMembers(project.getReportArtifacts(), reportPlugins, " (reportPlugin)"));
        // CHECKSTYLE_ON: LineLength

        if (versionMembers.size() > 1) {
            StringBuilder builder = new StringBuilder("Found entries with different versions" + System.lineSeparator());
            for (Map.Entry<String, List<String>> entry : versionMembers.entrySet()) {
                builder.append("Entries with version ").append(entry.getKey()).append(System.lineSeparator());
                for (String conflictId : entry.getValue()) {
                    builder.append("- ").append(conflictId).append(System.lineSeparator());
                }
            }
            throw new EnforcerRuleException(builder.toString());
        }

        if (sameModuleVersions) {
            MavenProject topLevelProject = session.getTopLevelProject();
            if (!Objects.equals(topLevelProject.getVersion(), project.getVersion())) {
                throw new EnforcerRuleException("Top level project has version " + topLevelProject.getVersion()
                        + " but current module has different version " + project.getVersion());
            }
        }
    }

    private Map<String, List<String>> collectVersionMembers(
            Set<Artifact> artifacts, Collection<String> patterns, String source) {
        Map<String, List<String>> versionMembers = new LinkedHashMap<>();

        List<Pattern> regExs = new ArrayList<>();
        for (String pattern : patterns) {
            String regex = pattern.replace(".", "\\.")
                    .replace("*", ".*")
                    .replace(":", "\\:")
                    .replace('?', '.');

            // pattern is groupId[:artifactId[:type[:classifier]]]
            regExs.add(Pattern.compile(regex + "(\\:.+)?"));
        }

        for (Artifact artifact : artifacts) {
            for (Pattern regEx : regExs) {
                if (regEx.matcher(artifact.getDependencyConflictId()).matches()) {
                    String version = uniqueVersions ? artifact.getVersion() : artifact.getBaseVersion();
                    if (!versionMembers.containsKey(version)) {
                        versionMembers.put(version, new ArrayList<>());
                    }
                    versionMembers.get(version).add(artifact.getDependencyConflictId() + source);
                }
            }
        }
        return versionMembers;
    }

    @Override
    public String toString() {
        return String.format(
                "RequireSameVersions[dependencies=%s, buildPlugins=%s, reportPlugins=%s, plugins=%s, uniqueVersions=%b, sameModuleVersions=%b]",
                dependencies, buildPlugins, reportPlugins, plugins, uniqueVersions, sameModuleVersions);
    }
}
