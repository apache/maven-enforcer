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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * This rule will check if a multi module build will follow the best practices.
 * 
 * @author Karl-Heinz Marbaise
 * @since 1.3.2
 */
public class ReactorModuleConvergence
    extends AbstractNonCacheableEnforcerRule
{
    private boolean ignoreModuleDependencies = false;

    private Log logger;

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        logger = helper.getLog();

        MavenSession session;
        try
        {
            session = (MavenSession) helper.evaluate( "${session}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenSession: ", eee );
        }

        @SuppressWarnings( "unchecked" )
        List<MavenProject> sortedProjects = session.getSortedProjects();
        if ( sortedProjects != null && !sortedProjects.isEmpty() )
        {
            checkReactor( sortedProjects );
            checkParentsInReactor( sortedProjects );
            checkMissingParentsInReactor( sortedProjects );
            checkParentsPartOfTheReactor( sortedProjects );
            if ( !isIgnoreModuleDependencies() )
            {
                checkDependenciesWithinReactor( sortedProjects );
            }
        }

    }

    private void checkParentsPartOfTheReactor( List<MavenProject> sortedProjects )
        throws EnforcerRuleException
    {
        List<MavenProject> parentsWhichAreNotPartOfTheReactor =
            existParentsWhichAreNotPartOfTheReactor( sortedProjects );
        if ( !parentsWhichAreNotPartOfTheReactor.isEmpty() )
        {
            StringBuilder sb = new StringBuilder().append( SystemUtils.LINE_SEPARATOR );
            for ( MavenProject mavenProject : parentsWhichAreNotPartOfTheReactor )
            {
                sb.append( " module: " );
                sb.append( mavenProject.getId() );
                sb.append( SystemUtils.LINE_SEPARATOR );
            }
            throw new EnforcerRuleException( "Module parents have been found which could not be found in the reactor."
                + sb.toString() );
        }
    }

    /**
     * Convenience method to create a user readable message.
     * 
     * @param sortedProjects The list of reactor projects.
     * @throws EnforcerRuleException In case of a violation.
     */
    private void checkMissingParentsInReactor( List<MavenProject> sortedProjects )
        throws EnforcerRuleException
    {
        List<MavenProject> modulesWithoutParentsInReactory = existModulesWithoutParentsInReactor( sortedProjects );
        if ( !modulesWithoutParentsInReactory.isEmpty() )
        {
            StringBuilder sb = new StringBuilder().append( SystemUtils.LINE_SEPARATOR );
            for ( MavenProject mavenProject : modulesWithoutParentsInReactory )
            {
                sb.append( " module: " );
                sb.append( mavenProject.getId() );
                sb.append( SystemUtils.LINE_SEPARATOR );
            }
            throw new EnforcerRuleException( "Reactor contains modules without parents." + sb.toString() );
        }
    }

    private void checkDependenciesWithinReactor( List<MavenProject> sortedProjects )
        throws EnforcerRuleException
    {
        //TODO: After we are sure having consistent version we can simply use the first one?
        String reactorVersion = sortedProjects.get( 0 ).getVersion();

        Map<MavenProject, List<Dependency>> areThereDependenciesWhichAreNotPartOfTheReactor =
            areThereDependenciesWhichAreNotPartOfTheReactor( reactorVersion, sortedProjects );
        if ( !areThereDependenciesWhichAreNotPartOfTheReactor.isEmpty() )
        {
            StringBuilder sb = new StringBuilder().append( SystemUtils.LINE_SEPARATOR );
            for ( Entry<MavenProject, List<Dependency>> item : areThereDependenciesWhichAreNotPartOfTheReactor.entrySet() )
            {
                sb.append( " module: " );
                sb.append( item.getKey().getId() );
                sb.append( SystemUtils.LINE_SEPARATOR );
                for ( Dependency dependency : item.getValue() )
                {
                    String id =
                        dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion();
                    sb.append( "    dependency: " );
                    sb.append( id );
                    sb.append( SystemUtils.LINE_SEPARATOR );
                }
            }
            throw new EnforcerRuleException(
                                             "Reactor modules contains dependencies which do not reference the reactor."
                                                 + sb.toString() );
        }
    }

    /**
     * Convenience method to create a user readable message.
     * 
     * @param sortedProjects The list of reactor projects.
     * @throws EnforcerRuleException In case of a violation.
     */
    private void checkParentsInReactor( List<MavenProject> sortedProjects )
        throws EnforcerRuleException
    {
        //TODO: After we are sure having consistent version we can simply use the first one?
        String reactorVersion = sortedProjects.get( 0 ).getVersion();

        List<MavenProject> areParentsFromTheReactor = areParentsFromTheReactor( reactorVersion, sortedProjects );
        if ( !areParentsFromTheReactor.isEmpty() )
        {
            StringBuilder sb = new StringBuilder().append( SystemUtils.LINE_SEPARATOR );
            for ( MavenProject mavenProject : areParentsFromTheReactor )
            {
                sb.append( " --> " );
                sb.append( mavenProject.getId() + " parent:" + mavenProject.getParent().getId() );
                sb.append( SystemUtils.LINE_SEPARATOR );
            }
            throw new EnforcerRuleException( "Reactor modules have parents which contain a wrong version."
                + sb.toString() );
        }
    }

    /**
     * Convenience method to create user readable message.
     * 
     * @param sortedProjects The list of reactor projects.
     * @throws EnforcerRuleException In case of a violation.
     */
    private void checkReactor( List<MavenProject> sortedProjects )
        throws EnforcerRuleException
    {
        List<MavenProject> consistenceCheckResult = isReactorVersionConsistent( sortedProjects );
        if ( !consistenceCheckResult.isEmpty() )
        {
            StringBuilder sb = new StringBuilder().append( SystemUtils.LINE_SEPARATOR );
            for ( MavenProject mavenProject : consistenceCheckResult )
            {
                sb.append( " --> " );
                sb.append( mavenProject.getId() );
                sb.append( SystemUtils.LINE_SEPARATOR );
            }
            throw new EnforcerRuleException( "The reactor contains different versions." + sb.toString() );
        }
    }

    private List<MavenProject> areParentsFromTheReactor( String reactorVersion, List<MavenProject> sortedProjects )
    {
        List<MavenProject> result = new ArrayList<MavenProject>();

        for ( MavenProject mavenProject : sortedProjects )
        {
            logger.debug( "Project: " + mavenProject.getId() );
            if ( hasParent( mavenProject ) )
            {
                if ( !mavenProject.isExecutionRoot() )
                {
                    MavenProject parent = mavenProject.getParent();
                    if ( !reactorVersion.equals( parent.getVersion() ) )
                    {
                        logger.debug( "The project: " + mavenProject.getId()
                            + " has a parent which version does not match the other elements in reactor" );
                        result.add( mavenProject );
                    }
                }
            }
            else
            {
                //This situation is currently ignored, cause it's handled by existModulesWithoutParentsInReactor()
            }
        }

        return result;
    }

    private List<MavenProject> existParentsWhichAreNotPartOfTheReactor( List<MavenProject> sortedProjects )
    {
        List<MavenProject> result = new ArrayList<MavenProject>();

        for ( MavenProject mavenProject : sortedProjects )
        {
            logger.debug( "Project: " + mavenProject.getId() );
            if ( hasParent( mavenProject ) )
            {
                if ( !mavenProject.isExecutionRoot() )
                {
                    MavenProject parent = mavenProject.getParent();
                    if ( !isProjectPartOfTheReactor( parent, sortedProjects ) )
                    {
                        result.add( mavenProject );
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
     * @param project The project which should be checked if it is contained in the sortedProjects.
     * @param sortedProjects The list of existing projects.
     * @return true if the project has been found within the list false otherwise.
     */
    private boolean isProjectPartOfTheReactor( MavenProject project, List<MavenProject> sortedProjects )
    {
        return isGAPartOfTheReactor( project.getGroupId(), project.getArtifactId(), sortedProjects );
    }

    private boolean isDependencyPartOfTheReactor( Dependency dependency, List<MavenProject> sortedProjects )
    {
        return isGAPartOfTheReactor( dependency.getGroupId(), dependency.getArtifactId(), sortedProjects );
    }

    private boolean isGAPartOfTheReactor( String groupId, String artifactId, List<MavenProject> sortedProjects )
    {
        boolean result = false;
        for ( MavenProject mavenProject : sortedProjects )
        {
            String parentId = groupId + ":" + artifactId;
            String projectId = mavenProject.getGroupId() + ":" + mavenProject.getArtifactId();
            if ( parentId.equals( projectId ) )
            {
                result = true;
            }
        }
        return result;
    }

    /**
     * Assume we have a module which is which is a child of a multi module build but this child does not have a parent.
     * This method will exactly search for such cases.
     * 
     * @param projectList The sorted list of the reactor modules.
     * @return The resulting list will contain the modules in the reactor which do not have a parent. The list will
     *         never null. If the list is empty no violation have happened.
     */
    private List<MavenProject> existModulesWithoutParentsInReactor( List<MavenProject> sortedProjects )
    {
        List<MavenProject> result = new ArrayList<MavenProject>();

        for ( MavenProject mavenProject : sortedProjects )
        {
            logger.debug( "Project: " + mavenProject.getId() );
            if ( !hasParent( mavenProject ) )
            {
                if ( mavenProject.isExecutionRoot() )
                {
                    logger.debug( "The root does not need having a parent." );
                }
                else
                {
                    logger.debug( "The module: " + mavenProject.getId() + " has no parent." );
                    result.add( mavenProject );
                }
            }
        }

        return result;
    }

    private void addDep( Map<MavenProject, List<Dependency>> result, MavenProject project, Dependency dependency )
    {
        if ( result.containsKey( project ) )
        {
            List<Dependency> list = result.get( project );
            if ( list == null )
            {
                list = new ArrayList<Dependency>();
            }
            list.add( dependency );
            result.put( project, list );
        }
        else
        {
            List<Dependency> list = new ArrayList<Dependency>();
            list.add( dependency );
            result.put( project, list );
        }
    }

    private Map<MavenProject, List<Dependency>> areThereDependenciesWhichAreNotPartOfTheReactor( String reactorVersion,
                                                                                                 List<MavenProject> sortedProjects )
    {
        Map<MavenProject, List<Dependency>> result = new HashMap<MavenProject, List<Dependency>>();
        for ( MavenProject mavenProject : sortedProjects )
        {
            logger.debug( "Project: " + mavenProject.getId() );

            @SuppressWarnings( "unchecked" )
            List<Dependency> dependencies = mavenProject.getDependencies();
            if ( hasDependencies( dependencies ) )
            {
                for ( Dependency dependency : dependencies )
                {
                    if ( isDependencyPartOfTheReactor( dependency, sortedProjects ) )
                    {
                        if ( !dependency.getVersion().equals( reactorVersion ) )
                        {
                            addDep( result, mavenProject, dependency );
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * This method will check the following situation within a multi-module build.
     * 
     * <pre>
     *  &lt;parent&gt;
     *    &lt;groupId&gt;...&lt;/groupId&gt;
     *    &lt;artifactId&gt;...&lt;/artifactId&gt;
     *    &lt;version&gt;1.0-SNAPSHOT&lt;/version&gt;
     *  &lt;/parent&gt;
     *  
     *  &lt;version&gt;1.1-SNAPSHOT&lt;/version&gt;
     * </pre>
     * 
     * @param projectList The sorted list of the reactor modules.
     * @return The resulting list will contain the modules in the reactor which do the thing in the example above. The
     *         list will never null. If the list is empty no violation have happened.
     */
    private List<MavenProject> isReactorVersionConsistent( List<MavenProject> projectList )
    {
        List<MavenProject> result = new ArrayList<MavenProject>();

        if ( projectList != null && !projectList.isEmpty() )
        {
            //TODO: Check if this the right choice ?
            String version = projectList.get( 0 ).getVersion();
            logger.debug( "First version:" + version );
            for ( MavenProject mavenProject : projectList )
            {
                logger.debug( " -> checking " + mavenProject.getId() );
                if ( !version.equals( mavenProject.getVersion() ) )
                {
                    result.add( mavenProject );
                }
            }
        }
        return result;
    }

    private boolean hasDependencies( List<Dependency> dependencies )
    {
        return dependencies != null && !dependencies.isEmpty();
    }

    private boolean hasParent( MavenProject mavenProject )
    {
        return mavenProject.getParent() != null;
    }

    public boolean isIgnoreModuleDependencies()
    {
        return ignoreModuleDependencies;
    }

    public void setIgnoreModuleDependencies( boolean ignoreModuleDependencies )
    {
        this.ignoreModuleDependencies = ignoreModuleDependencies;
    }

}