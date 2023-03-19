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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Since Maven 3 'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique. Early versions of Maven
 * 3 already warn, this rule can force to break a build for this reason.
 *
 * @author Robert Scholte
 * @since 1.3
 */
@Named("banDuplicatePomDependencyVersions")
public final class BanDuplicatePomDependencyVersions extends AbstractStandardEnforcerRule {

    private final MavenProject project;

    @Inject
    public BanDuplicatePomDependencyVersions(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    @Override
    public void execute() throws EnforcerRuleException {

        // re-read model, because M3 uses optimized model
        MavenXpp3Reader modelReader = new MavenXpp3Reader();

        Model model;
        try (FileInputStream pomInputStream = new FileInputStream(project.getFile())) {
            model = modelReader.read(pomInputStream, false);
        } catch (IOException | XmlPullParserException e) {
            throw new EnforcerRuleError("Unable to retrieve the MavenProject: ", e);
        }

        // @todo reuse ModelValidator when possible

        // Object modelValidator = null;
        // try
        // {
        // modelValidator = helper.getComponent( "org.apache.maven.model.validation.ModelValidator" );
        // }
        // catch ( ComponentLookupException e1 )
        // {
        // // noop
        // }

        // if( modelValidator == null )
        // {
        maven2Validation(model);
        // }
        // else
        // {
        // }
    }

    private void maven2Validation(Model model) throws EnforcerRuleException {
        List<Dependency> dependencies = model.getDependencies();
        Map<String, Integer> duplicateDependencies = validateDependencies(dependencies);

        int duplicates = duplicateDependencies.size();

        StringBuilder summary = new StringBuilder();
        messageBuilder(duplicateDependencies, "dependencies.dependency", summary);

        if (model.getDependencyManagement() != null) {
            List<Dependency> managementDependencies =
                    model.getDependencyManagement().getDependencies();
            Map<String, Integer> duplicateManagementDependencies = validateDependencies(managementDependencies);
            duplicates += duplicateManagementDependencies.size();

            messageBuilder(duplicateManagementDependencies, "dependencyManagement.dependencies.dependency", summary);
        }

        List<Profile> profiles = model.getProfiles();
        for (Profile profile : profiles) {
            List<Dependency> profileDependencies = profile.getDependencies();

            Map<String, Integer> duplicateProfileDependencies = validateDependencies(profileDependencies);

            duplicates += duplicateProfileDependencies.size();

            messageBuilder(
                    duplicateProfileDependencies,
                    "profiles.profile[" + profile.getId() + "].dependencies.dependency",
                    summary);

            if (profile.getDependencyManagement() != null) {
                List<Dependency> profileManagementDependencies =
                        profile.getDependencyManagement().getDependencies();

                Map<String, Integer> duplicateProfileManagementDependencies =
                        validateDependencies(profileManagementDependencies);

                duplicates += duplicateProfileManagementDependencies.size();

                messageBuilder(
                        duplicateProfileManagementDependencies,
                        "profiles.profile[" + profile.getId() + "].dependencyManagement.dependencies.dependency",
                        summary);
            }
        }

        if (summary.length() > 0) {
            StringBuilder message = new StringBuilder();
            message.append("Found ").append(duplicates).append(" duplicate dependency ");
            message.append(duplicateDependencies.size() == 1 ? "declaration" : "declarations")
                    .append(" in this project:" + System.lineSeparator());
            message.append(summary);
            throw new EnforcerRuleException(message.toString());
        }
    }

    private void messageBuilder(Map<String, Integer> duplicateDependencies, String prefix, StringBuilder message) {
        if (!duplicateDependencies.isEmpty()) {
            for (Map.Entry<String, Integer> entry : duplicateDependencies.entrySet()) {
                message.append(" - ")
                        .append(prefix)
                        .append('[')
                        .append(entry.getKey())
                        .append("] (")
                        .append(entry.getValue())
                        .append(" times)" + System.lineSeparator());
            }
        }
    }

    private Map<String, Integer> validateDependencies(List<Dependency> dependencies) {
        Map<String, Integer> duplicateDeps = new HashMap<>();
        Set<String> deps = new HashSet<>();
        for (Dependency dependency : dependencies) {
            String key = dependency.getManagementKey();

            if (deps.contains(key)) {
                int times = 1;
                if (duplicateDeps.containsKey(key)) {
                    times = duplicateDeps.get(key);
                }
                duplicateDeps.put(key, times + 1);
            } else {
                deps.add(key);
            }
        }
        return duplicateDeps;
    }
}
