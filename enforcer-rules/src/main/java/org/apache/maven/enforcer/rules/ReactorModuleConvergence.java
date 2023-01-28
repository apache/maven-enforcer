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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule will check if a multi module build will follow the best practices.
 *
 * @author Karl-Heinz Marbaise
 * @since 1.4
 */
@Named("reactorModuleConvergence")
public final class ReactorModuleConvergence extends AbstractStandardEnforcerRule {
    private static final String MODULE_TEXT = " module: ";

    private boolean ignoreModuleDependencies = false;

    private final MavenSession session;

    @Inject
    public ReactorModuleConvergence(MavenSession session) {
        this.session = Objects.requireNonNull(session);
    }

    @Override
    public void execute() throws EnforcerRuleException {

        List<MavenProject> sortedProjects = session.getProjectDependencyGraph().getSortedProjects();
        if (sortedProjects != null && !sortedProjects.isEmpty()) {
            checkReactor(sortedProjects);
            checkParentsInReactor(sortedProjects);
            checkMissingParentsInReactor(sortedProjects);
            checkParentsPartOfTheReactor(sortedProjects);
            if (!isIgnoreModuleDependencies()) {
                checkDependenciesWithinReactor(sortedProjects);
            }
        }
    }

    private void checkParentsPartOfTheReactor(List<MavenProject> sortedProjects) throws EnforcerRuleException {
        List<MavenProject> parentsWhichAreNotPartOfTheReactor = existParentsWhichAreNotPartOfTheReactor(sortedProjects);
        if (!parentsWhichAreNotPartOfTheReactor.isEmpty()) {
            StringBuilder sb = new StringBuilder().append(System.lineSeparator());
            addMessageIfExist(sb);
            for (MavenProject mavenProject : parentsWhichAreNotPartOfTheReactor) {
                sb.append(MODULE_TEXT);
                sb.append(mavenProject.getId());
                sb.append(System.lineSeparator());
            }
            throw new EnforcerRuleException(
                    "Module parents have been found which could not be found in the reactor." + sb);
        }
    }

    /**
     * Convenience method to create a user readable message.
     *
     * @param sortedProjects The list of reactor projects.
     * @throws EnforcerRuleException In case of a violation.
     */
    private void checkMissingParentsInReactor(List<MavenProject> sortedProjects) throws EnforcerRuleException {
        List<MavenProject> modulesWithoutParentsInReactor = existModulesWithoutParentsInReactor(sortedProjects);
        if (!modulesWithoutParentsInReactor.isEmpty()) {
            StringBuilder sb = new StringBuilder().append(System.lineSeparator());
            addMessageIfExist(sb);
            for (MavenProject mavenProject : modulesWithoutParentsInReactor) {
                sb.append(MODULE_TEXT);
                sb.append(mavenProject.getId());
                sb.append(System.lineSeparator());
            }
            throw new EnforcerRuleException("Reactor contains modules without parents." + sb);
        }
    }

    private void checkDependenciesWithinReactor(List<MavenProject> sortedProjects) throws EnforcerRuleException {
        // After we are sure having consistent version we can simply use the first one?
        String reactorVersion = sortedProjects.get(0).getVersion();

        Map<MavenProject, List<Dependency>> areThereDependenciesWhichAreNotPartOfTheReactor =
                areThereDependenciesWhichAreNotPartOfTheReactor(reactorVersion, sortedProjects);
        if (!areThereDependenciesWhichAreNotPartOfTheReactor.isEmpty()) {
            StringBuilder sb = new StringBuilder().append(System.lineSeparator());
            addMessageIfExist(sb);
            // CHECKSTYLE_OFF: LineLength
            for (Entry<MavenProject, List<Dependency>> item :
                    areThereDependenciesWhichAreNotPartOfTheReactor.entrySet()) {
                sb.append(MODULE_TEXT);
                sb.append(item.getKey().getId());
                sb.append(System.lineSeparator());
                for (Dependency dependency : item.getValue()) {
                    String id =
                            dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion();
                    sb.append("    dependency: ");
                    sb.append(id);
                    sb.append(System.lineSeparator());
                }
            }
            throw new EnforcerRuleException(
                    "Reactor modules contains dependencies which do not reference the reactor." + sb);
            // CHECKSTYLE_ON: LineLength
        }
    }

    /**
     * Convenience method to create a user readable message.
     *
     * @param sortedProjects The list of reactor projects.
     * @throws EnforcerRuleException In case of a violation.
     */
    private void checkParentsInReactor(List<MavenProject> sortedProjects) throws EnforcerRuleException {
        // After we are sure having consistent version we can simply use the first one?
        String reactorVersion = sortedProjects.get(0).getVersion();

        List<MavenProject> areParentsFromTheReactor = areParentsFromTheReactor(reactorVersion, sortedProjects);
        if (!areParentsFromTheReactor.isEmpty()) {
            StringBuilder sb = new StringBuilder().append(System.lineSeparator());
            addMessageIfExist(sb);
            for (MavenProject mavenProject : areParentsFromTheReactor) {
                sb.append(" --> ");
                sb.append(mavenProject.getId());
                sb.append(" parent:");
                sb.append(mavenProject.getParent().getId());
                sb.append(System.lineSeparator());
            }
            throw new EnforcerRuleException("Reactor modules have parents which contain a wrong version." + sb);
        }
    }

    /**
     * Convenience method to create user readable message.
     *
     * @param sortedProjects The list of reactor projects.
     * @throws EnforcerRuleException In case of a violation.
     */
    private void checkReactor(List<MavenProject> sortedProjects) throws EnforcerRuleException {
        List<MavenProject> consistenceCheckResult = isReactorVersionConsistent(sortedProjects);
        if (!consistenceCheckResult.isEmpty()) {
            StringBuilder sb = new StringBuilder().append(System.lineSeparator());
            addMessageIfExist(sb);
            for (MavenProject mavenProject : consistenceCheckResult) {
                sb.append(" --> ");
                sb.append(mavenProject.getId());
                sb.append(System.lineSeparator());
            }
            throw new EnforcerRuleException("The reactor contains different versions." + sb);
        }
    }

    private List<MavenProject> areParentsFromTheReactor(String reactorVersion, List<MavenProject> sortedProjects) {
        List<MavenProject> result = new ArrayList<>();

        for (MavenProject mavenProject : sortedProjects) {
            getLog().debug("Project: " + mavenProject.getId());
            if (hasParent(mavenProject)) {
                if (!mavenProject.isExecutionRoot()) {
                    MavenProject parent = mavenProject.getParent();
                    if (!reactorVersion.equals(parent.getVersion())) {
                        getLog().debug("The project: " + mavenProject.getId()
                                + " has a parent which version does not match the other elements in reactor");
                        result.add(mavenProject);
                    }
                }
            } else {
                // This situation is currently ignored, cause it's handled by existModulesWithoutParentsInReactor()
            }
        }

        return result;
    }

    private List<MavenProject> existParentsWhichAreNotPartOfTheReactor(List<MavenProject> sortedProjects) {
        List<MavenProject> result = new ArrayList<>();

        for (MavenProject mavenProject : sortedProjects) {
            getLog().debug("Project: " + mavenProject.getId());
            if (hasParent(mavenProject)) {
                if (!mavenProject.isExecutionRoot()) {
                    MavenProject parent = mavenProject.getParent();
                    if (!isProjectPartOfTheReactor(parent, sortedProjects)) {
                        result.add(mavenProject);
                    }
                }
            }
        }

        return result;
    }

    /**
     * This will check of the groupId/artifactId can be found in any reactor project. The version will be ignored cause
     * versions are checked before.
     *
     * @param project        The project which should be checked if it is contained in the sortedProjects.
     * @param sortedProjects The list of existing projects.
     * @return true if the project has been found within the list false otherwise.
     */
    private boolean isProjectPartOfTheReactor(MavenProject project, List<MavenProject> sortedProjects) {
        return isGAPartOfTheReactor(project.getGroupId(), project.getArtifactId(), sortedProjects);
    }

    private boolean isDependencyPartOfTheReactor(Dependency dependency, List<MavenProject> sortedProjects) {
        return isGAPartOfTheReactor(dependency.getGroupId(), dependency.getArtifactId(), sortedProjects);
    }

    /**
     * This will check if the given <code>groupId/artifactId</code> is part of the current reactor.
     *
     * @param groupId        The groupId
     * @param artifactId     The artifactId
     * @param sortedProjects The list of projects within the reactor.
     * @return true if the groupId/artifactId is part of the reactor false otherwise.
     */
    private boolean isGAPartOfTheReactor(String groupId, String artifactId, List<MavenProject> sortedProjects) {
        boolean result = false;
        for (MavenProject mavenProject : sortedProjects) {
            String parentId = groupId + ":" + artifactId;
            String projectId = mavenProject.getGroupId() + ":" + mavenProject.getArtifactId();
            if (parentId.equals(projectId)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Assume we have a module which is a child of a multi module build but this child does not have a parent. This
     * method will exactly search for such cases.
     *
     * @param sortedProjects The sorted list of the reactor modules.
     * @return The resulting list will contain the modules in the reactor which do not have a parent. The list will
     *         never null. If the list is empty no violation have happened.
     */
    private List<MavenProject> existModulesWithoutParentsInReactor(List<MavenProject> sortedProjects) {
        List<MavenProject> result = new ArrayList<>();

        for (MavenProject mavenProject : sortedProjects) {
            getLog().debug("Project: " + mavenProject.getId());
            if (!hasParent(mavenProject)) {
                // TODO: Should add an option to force having a parent?
                if (mavenProject.isExecutionRoot()) {
                    getLog().debug("The root does not need having a parent.");
                } else {
                    getLog().debug("The module: " + mavenProject.getId() + " has no parent.");
                    result.add(mavenProject);
                }
            }
        }

        return result;
    }

    /**
     * Convenience method to handle adding a dependency to the Map of List.
     *
     * @param result     The result List which should be handled.
     * @param project    The MavenProject which will be added.
     * @param dependency The dependency which will be added.
     */
    private void addDep(Map<MavenProject, List<Dependency>> result, MavenProject project, Dependency dependency) {
        if (result.containsKey(project)) {
            List<Dependency> list = result.get(project);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(dependency);
            result.put(project, list);
        } else {
            List<Dependency> list = new ArrayList<>();
            list.add(dependency);
            result.put(project, list);
        }
    }

    /**
     * Go through the list of modules in the builds and check if we have dependencies. If yes we will check every
     * dependency based on groupId/artifactId if it belongs to the multi module build. In such a case it will be checked
     * if the version does fit the version in the rest of build.
     *
     * @param reactorVersion The version of the reactor.
     * @param sortedProjects The list of existing projects within this build.
     * @return List of violations. Never null. If the list is empty than no violation has happened.
     */
    // CHECKSTYLE_OFF: LineLength
    private Map<MavenProject, List<Dependency>> areThereDependenciesWhichAreNotPartOfTheReactor(
            String reactorVersion, List<MavenProject> sortedProjects)
                // CHECKSTYLE_ON: LineLength
            {
        Map<MavenProject, List<Dependency>> result = new HashMap<>();
        for (MavenProject mavenProject : sortedProjects) {
            getLog().debug("Project: " + mavenProject.getId());

            List<Dependency> dependencies = mavenProject.getDependencies();
            if (hasDependencies(dependencies)) {
                for (Dependency dependency : dependencies) {
                    getLog().debug(" -> Dep:" + dependency.getGroupId() + ":" + dependency.getArtifactId() + ":"
                            + dependency.getVersion());
                    if (isDependencyPartOfTheReactor(dependency, sortedProjects)) {
                        if (!dependency.getVersion().equals(reactorVersion)) {
                            addDep(result, mavenProject, dependency);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * This method will check the following situation within a multi-module build.
     * <pre>
     *  &lt;parent&gt;
     *    &lt;groupId&gt;...&lt;/groupId&gt;
     *    &lt;artifactId&gt;...&lt;/artifactId&gt;
     *    &lt;version&gt;1.0-SNAPSHOT&lt;/version&gt;
     *  &lt;/parent&gt;
     *  &lt;version&gt;1.1-SNAPSHOT&lt;/version&gt;
     * </pre>
     *
     * @param projectList The sorted list of the reactor modules.
     * @return The resulting list will contain the modules in the reactor which do the thing in the example above. The
     *         list will never null. If the list is empty no violation have happened.
     */
    private List<MavenProject> isReactorVersionConsistent(List<MavenProject> projectList) {
        List<MavenProject> result = new ArrayList<>();

        if (projectList != null && !projectList.isEmpty()) {
            String version = projectList.get(0).getVersion();
            getLog().debug("First version:" + version);
            for (MavenProject mavenProject : projectList) {
                getLog().debug(" -> checking " + mavenProject.getId());
                if (!version.equals(mavenProject.getVersion())) {
                    result.add(mavenProject);
                }
            }
        }
        return result;
    }

    private boolean hasDependencies(List<Dependency> dependencies) {
        return dependencies != null && !dependencies.isEmpty();
    }

    private boolean hasParent(MavenProject mavenProject) {
        return mavenProject.getParent() != null;
    }

    public boolean isIgnoreModuleDependencies() {
        return ignoreModuleDependencies;
    }

    /**
     * This will add the given user message to the output.
     *
     * @param sb The already initialized exception message part.
     */
    private void addMessageIfExist(StringBuilder sb) {
        if (!StringUtils.isEmpty(getMessage())) {
            sb.append(getMessage());
            sb.append(System.lineSeparator());
        }
    }

    @Override
    public String getCacheId() {
        return String.valueOf(toString().hashCode());
    }

    @Override
    public String toString() {
        return String.format(
                "ReactorModuleConvergence[message=%s, ignoreModuleDependencies=%b]",
                getMessage(), ignoreModuleDependencies);
    }
}
