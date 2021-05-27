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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRule2;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.OrDependencyFilter;
import org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;

/**
 * Checks that the runtime classpath (e.g. used by Maven Plugins via the 
 * <a href="https://maven.apache.org/guides/mini/guide-maven-classloading.html#3-plugin-classloaders">Plugin
 * Classloader</a>) contains all transitive provided dependencies.
 * 
 * As those are not transitively inherited they need to be declared explicitly in the pom.xml of the using Maven
 * project.
 * 
 * This check is useful to make sure that a Maven Plugin has access to all necessary classes at run time.
 * @author Konrad Windszus
 */
public class RequireTransitiveProvidedDependenciesInRuntimeClasspath 
    extends AbstractNonCacheableEnforcerRule implements EnforcerRule2
{
    /**
     * Specify the banned dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard 
     * by using '*' (ie group:*:1.0) <br>
     * The rule will fail if any dependency matches any exclude, unless it also matches 
     * an include rule.
     * 
     * @see {@link #setExcludes(List)}
     */
    private List<String> excludes = null;
    
    @Override
    public void execute( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        MavenProject project;
        DefaultRepositorySystemSession newRepoSession;
        RepositorySystem repoSystem;
        List<RemoteRepository> remoteRepositories;
        try
        {
            project = (MavenProject) helper.evaluate( "${project}" );
            // get a new session to be able to tweak the dependency selector
            newRepoSession = new DefaultRepositorySystemSession( 
                    (RepositorySystemSession) helper.evaluate( "${repositorySystemSession}" ) );
            remoteRepositories =  (List<RemoteRepository>) helper.evaluate( "${project.remoteProjectRepositories}" );
            repoSystem = helper.getComponent( RepositorySystem.class );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve Maven project or repository system sesssion", eee );
        } 
        catch ( ComponentLookupException cle ) 
        {
            throw new EnforcerRuleException( "Unable to retrieve component RepositorySystem", cle );
        }
        Log log = helper.getLog();
        // make sure to transitively also collect dependencies of provided scope
        newRepoSession.setDependencySelector( new AndDependencySelector( 
                new OptionalDependencySelector(),
                new ScopeDependencySelector( "test" ),
                new ExclusionDependencySelector( Collections.singleton( new Exclusion( "*", "*", "*", "pom" ) ) ) 
                ) );
        // use the ones for https://maven.apache.org/guides/mini/guide-maven-classloading.html#3-plugin-classloaders
        List<Artifact> runtimeArtifacts = project.getRuntimeArtifacts();
        
        try 
        {
            DependencyNode dependencyRootNode = resolveTransitiveDependencies(
                    RepositoryUtils.toDependency( project.getArtifact(), null ),
                    repoSystem, newRepoSession, remoteRepositories );
            if ( log.isDebugEnabled() ) 
            {
                StringWriter writer = new StringWriter();
                DependencyVisitor depVisitor = new TreeDependencyVisitor( 
                        new DependencyVisitorPrinter( new PrintWriter ( writer ) ) );
                dependencyRootNode.accept( depVisitor );
                log.debug( "Dependency Graph " + writer.toString() );
            }
            checkDependencyChildren( dependencyRootNode, runtimeArtifacts, "", log );
        }
        catch ( DependencyResolutionException e )
        {
            // draw graph
            StringWriter writer = new StringWriter();
            DependencyVisitor depVisitor = new TreeDependencyVisitor( 
                    new DependencyVisitorPrinter( new PrintWriter ( writer ) ) );
            e.getResult().getRoot().accept( depVisitor );
            throw new EnforcerRuleException( "Could not retrieve dependency metadata for project  : "
                    + e.getMessage() + ". Partial dependency tree: " + writer.toString(), e );
        }
        
    }

    private final class DependencyVisitorPrinter implements DependencyVisitor 
    {
        private final PrintWriter printWriter;
        private String indent = "";

        DependencyVisitorPrinter( PrintWriter printWriter ) 
        {
            this.printWriter = printWriter;
        }
        @Override
        public boolean visitEnter( DependencyNode dependencyNode )
        {
            printWriter.println( indent + dependencyNode.getArtifact() );
            indent += "    ";
            return true;
        }

        @Override
        public boolean visitLeave( DependencyNode dependencyNode )
        {
            indent = indent.substring( 0, indent.length() - 4 );
            return true;
        }
    }
 
    protected void checkDependencyChildren( DependencyNode dependencyNode, List<Artifact> runtimeArtifacts, 
            String dependencyGraph, Log log ) throws EnforcerRuleException
    {
        // should children be evaluated in that case?
        for ( DependencyNode child : dependencyNode.getChildren() )
        {
            Artifact dependency = RepositoryUtils.toArtifact( child.getArtifact() );
            if ( ArtifactUtils.checkDependencies( Collections.singleton( dependency ), excludes ) == null )
            {
                if ( !isArtifactContainedInList( dependency, runtimeArtifacts ) )
                {
                    throw new EnforcerRuleException( "Transitive provided dependency " + dependency
                             + dependencyGraph + " not found as direct dependency!" );
                }
                StringBuilder newDependencyGraph = new StringBuilder( dependencyGraph );
                if ( newDependencyGraph.length() > 0 )
                {
                    newDependencyGraph.append( " -> " );
                }
                else 
                {
                    newDependencyGraph.append( " via " );
                }
                newDependencyGraph.append( dependency );
                checkDependencyChildren( child, runtimeArtifacts, newDependencyGraph.toString(), log );
            } 
            else
            {
                log.debug( "Skip excluded dependency and all its children " + dependency );
            }
        }
    }

    protected static boolean isArtifactContainedInList( Artifact artifact, 
            List<org.apache.maven.artifact.Artifact> artifacts )
    {
        for ( org.apache.maven.artifact.Artifact artifactInList : artifacts ) 
        {
            if ( artifact.getArtifactId().equals( artifactInList.getArtifactId() )
                 && artifact.getGroupId().equals( artifactInList.getGroupId() )
                 && Objects.toString( artifactInList.getClassifier(), "" )
                     .equals( Objects.toString( artifact.getClassifier(), "" ) )
                 && artifact.getType().equals( artifactInList.getType() ) )
            {
                return true;
            }
        }
        return false;
    }

    protected static DependencyNode resolveTransitiveDependencies(
            org.eclipse.aether.graph.Dependency dependency, 
            RepositorySystem repoSystem, RepositorySystemSession repoSession,
            List<RemoteRepository> remoteRepositories ) 
                    throws DependencyResolutionException 
    {
        CollectRequest collectRequest = new CollectRequest( dependency, remoteRepositories );
        // dependency itself (independent of scope) + transitive provided ones
        DependencyFilter depFilter = new OrDependencyFilter( 
                new PatternInclusionsDependencyFilter( 
                        dependency.getArtifact().getGroupId() + ":" 
                        + dependency.getArtifact().getArtifactId() ),
                new ScopeDependencyFilter( Collections.singleton( "provided" ), 
                        Collections.<String>emptySet() ) );
        DependencyRequest req = new DependencyRequest( collectRequest, depFilter );
        DependencyResult resolutionResult = repoSystem.resolveDependencies( repoSession, req );
        return resolutionResult.getRoot();
    }

    /**
     * Specify the banned dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard 
     * by using '*' (ie group:*:1.0) <br>
     * The rule will fail if any dependency matches any exclude, unless it also matches an 
     * include rule.
     * 
     * @param theExcludes the excludes to set
     */
    public void setExcludes( List<String> theExcludes )
    {
        this.excludes = theExcludes;
    }
}
