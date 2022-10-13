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

import java.text.ChoiceFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.ArtifactMatcher;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.eclipse.aether.version.VersionConstraint;

/**
 * This rule bans dependencies having a version which requires resolution (i.e. dynamic versions which might change with
 * each build). Dynamic versions are either
 * <ul>
 * <li>version ranges,</li>
 * <li>the special placeholders {@code LATEST} or {@code RELEASE} or</li>
 * <li>versions ending with {@code -SNAPSHOT}.
 * </ul>
 * 
 * @since 3.2.0
 */
public class BanDynamicVersions
    extends AbstractNonCacheableEnforcerRule
{

    private static final String RELEASE = "RELEASE";

    private static final String LATEST = "LATEST";

    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    /**
     * {@code true} if versions ending with {@code -SNAPSHOT} should be allowed
     */
    private boolean allowSnapshots;

    /**
     * {@code true} if versions using {@code LATEST} should be allowed
     */
    private boolean allowLatest;

    /**
     * {@code true} if versions using {@code RELEASE} should be allowed
     */
    private boolean allowRelease;

    /**
     * {@code true} if version ranges should be allowed
     */
    private boolean allowRanges;

    /**
     * {@code true} if ranges having the same upper and lower bound like {@code [1.0]} should be allowed.
     * Only applicable if {@link #allowRanges} is not set to {@code true}.
     */
    private boolean allowRangesWithIdenticalBounds;

    /**
     * {@code true} if optional dependencies should not be checked
     */
    private boolean excludeOptionals;

    /**
     * the scopes of dependencies which should be excluded from this rule
     */
    private String[] excludedScopes;

    /**
     * Specify the ignored dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId[:version[:type[:scope:[classifier]]]]]</code>. 
     * Any of the sections can be a wildcard by using '*' (e.g. {@code group:*:1.0}).
     * <br>
     * Any of the ignored dependencies may have dynamic versions.
     * 
     * @see {@link #setIgnores(List)}
     */
    private List<String> ignores = null;

    public void setIgnores( List<String> ignores )
    {
        this.ignores = ignores;
    }

    public void setAllowSnapshots( boolean allowSnapshots )
    {
        this.allowSnapshots = allowSnapshots;
    }

    public void setAllowLatest( boolean allowLatest )
    {
        this.allowLatest = allowLatest;
    }

    public void setAllowRelease( boolean allowRelease )
    {
        this.allowRelease = allowRelease;
    }

    public void setAllowRanges( boolean allowRanges )
    {
        this.allowRanges = allowRanges;
    }

    public void setExcludeOptionals( boolean excludeOptionals )
    {
        this.excludeOptionals = excludeOptionals;
    }

    public void setExcludedScopes( String[] excludedScopes )
    {
        this.excludedScopes = excludedScopes;
    }

    private final class BannedDynamicVersionCollector
        implements DependencyVisitor
    {

        private final Log log;

        private final Deque<DependencyNode> nodeStack; // all intermediate nodes (without the root node)

        private boolean isRoot = true;

        private int numViolations;

        private final Predicate<DependencyNode> predicate;

        public int getNumViolations()
        {
            return numViolations;
        }

        BannedDynamicVersionCollector( Log log, Predicate<DependencyNode> predicate )
        {
            this.log = log;
            nodeStack = new ArrayDeque<>();
            this.predicate = predicate;
            this.isRoot = true;
            numViolations = 0;
        }

        private boolean isBannedDynamicVersion( VersionConstraint versionConstraint )
        {
            if ( versionConstraint.getVersion() != null )
            {
                if ( versionConstraint.getVersion().toString().equals( LATEST ) )
                {
                    return !allowLatest;
                }
                else if ( versionConstraint.getVersion().toString().equals( RELEASE ) )
                {
                    return !allowRelease;
                }
                else if ( versionConstraint.getVersion().toString().endsWith( SNAPSHOT_SUFFIX ) )
                {
                    return !allowSnapshots;
                }
            }
            else if ( versionConstraint.getRange() != null )
            {
                if ( allowRangesWithIdenticalBounds 
                     && Objects.equals( versionConstraint.getRange().getLowerBound(), 
                                        versionConstraint.getRange().getUpperBound() ) ) 
                {
                        return false;
                }
                return !allowRanges;
            }
            else
            {
                log.warn( "Unexpected version constraint found: " + versionConstraint );
            }
            return false;

        }

        @Override
        public boolean visitEnter( DependencyNode node )
        {
            if ( isRoot )
            {
                isRoot = false;
            }
            else
            {
                log.debug( "Found node " + node + " with version constraint " + node.getVersionConstraint() );
                if ( predicate.test( node ) && isBannedDynamicVersion( node.getVersionConstraint() ) )
                {
                    MessageBuilder msgBuilder = MessageUtils.buffer();
                    log.warn( msgBuilder.a( "Dependency " )
                              .strong( node.getDependency() )
                              .mojo( dumpIntermediatePath( nodeStack ) )
                              .a( " is referenced with a banned dynamic version " + node.getVersionConstraint() )
                              .toString() );
                    numViolations++;
                    return false;
                }
                nodeStack.addLast( node );
            }
            return true;
        }

        @Override
        public boolean visitLeave( DependencyNode node )
        {
            if ( !nodeStack.isEmpty() )
            {
                nodeStack.removeLast();
            }
            return true;
        }
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        MavenProject project;
        DefaultRepositorySystemSession newRepoSession;
        RepositorySystem repoSystem;
        List<RemoteRepository> remoteRepositories;
        try
        {
            project = (MavenProject) Objects.requireNonNull( helper.evaluate( "${project}" ), "${project} is null" );
            RepositorySystemSession repoSession =
                (RepositorySystemSession) Objects.requireNonNull( helper.evaluate( "${repositorySystemSession}" ),
                                                                  "${repositorySystemSession} is null" );
            // get a new session to be able to tweak the dependency selector
            newRepoSession = new DefaultRepositorySystemSession( repoSession );
            remoteRepositories = (List<RemoteRepository>) helper.evaluate( "${project.remoteProjectRepositories}" );
            repoSystem = helper.getComponent( RepositorySystem.class );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Cannot resolve expression", eee );
        }
        catch ( ComponentLookupException cle )
        {
            throw new EnforcerRuleException( "Unable to retrieve component RepositorySystem", cle );
        }
        Log log = helper.getLog();

        Collection<DependencySelector> depSelectors = new ArrayList<>();
        depSelectors.add( new ScopeDependencySelector( excludedScopes ) );
        if ( excludeOptionals )
        {
            depSelectors.add( new OptionalDependencySelector() );
        }
        newRepoSession.setDependencySelector( new AndDependencySelector( depSelectors ) );

        Dependency rootDependency = RepositoryUtils.toDependency( project.getArtifact(), null );
        try
        {
            // use root dependency with unresolved direct dependencies
            int numViolations = emitDependenciesWithBannedDynamicVersions( rootDependency, repoSystem, newRepoSession,
                                                                           remoteRepositories, log );
            if ( numViolations > 0 )
            {
                ChoiceFormat dependenciesFormat = new ChoiceFormat( "1#dependency|1<dependencies" );
                throw new EnforcerRuleException( "Found " + numViolations + " "
                    + dependenciesFormat.format( numViolations )
                    + " with dynamic versions. Look at the warnings emitted above for the details." );
            }
        }
        catch ( DependencyCollectionException e )
        {
            throw new EnforcerRuleException( "Could not retrieve dependency metadata for project",
                                             e );
        }
    }

    private static String dumpIntermediatePath( Collection<DependencyNode> path )
    {
        if ( path.isEmpty() )
        {
            return "";
        }
        return " via " + path.stream().map( n -> n.getArtifact().toString() ).collect( Collectors.joining( " -> " ) );
    }

    private static final class ExcludeArtifactPatternsPredicate
        implements Predicate<DependencyNode>
    {

        private final ArtifactMatcher artifactMatcher;

        ExcludeArtifactPatternsPredicate( List<String> excludes )
        {
            this.artifactMatcher = new ArtifactMatcher( excludes, Collections.emptyList() );
        }

        @Override
        public boolean test( DependencyNode depNode )
        {
            try
            {
                return artifactMatcher.match( RepositoryUtils.toArtifact( depNode.getArtifact() ) );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new IllegalArgumentException( "Invalid version found for dependency node " + depNode, e );
            }
        }

    }

    protected int emitDependenciesWithBannedDynamicVersions( org.eclipse.aether.graph.Dependency rootDependency,
                                                             RepositorySystem repoSystem,
                                                             RepositorySystemSession repoSession,
                                                             List<RemoteRepository> remoteRepositories, Log log )
        throws DependencyCollectionException
    {
        CollectRequest collectRequest = new CollectRequest( rootDependency, remoteRepositories );
        CollectResult collectResult = repoSystem.collectDependencies( repoSession, collectRequest );
        Predicate<DependencyNode> predicate;
        if ( ignores != null && !ignores.isEmpty() )
        {
            predicate = new ExcludeArtifactPatternsPredicate( ignores );
        }
        else
        {
            predicate = d -> true;
        }
        BannedDynamicVersionCollector bannedDynamicVersionCollector =
            new BannedDynamicVersionCollector( log, predicate );
        DependencyVisitor depVisitor = new TreeDependencyVisitor( bannedDynamicVersionCollector );
        collectResult.getRoot().accept( depVisitor );
        return bannedDynamicVersionCollector.getNumViolations();
    }

}
