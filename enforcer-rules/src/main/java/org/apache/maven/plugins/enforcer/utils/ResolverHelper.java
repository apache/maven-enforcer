package org.apache.maven.plugins.enforcer.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;

public class ResolverHelper {
    
    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repoSession;
    private final List<RemoteRepository> remoteRepositories;
    private final List<RemoteRepository> remotePluginRepositories;
    
    public ResolverHelper( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        try
        {
            repoSession =
                (RepositorySystemSession) Objects.requireNonNull( helper.evaluate( "${repositorySystemSession}" ),
                                                                  "${repositorySystemSession} is null" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve repositorySystemSession", eee );
        }
        remoteRepositories = getRemoteRepositories( helper );
        remotePluginRepositories = getRemotePluginRepositories( helper );
        try 
        {
            repoSystem = helper.getComponent( RepositorySystem.class );
        }
        catch ( ComponentLookupException cle )
        {
            throw new EnforcerRuleException( "Unable to lookup component RepositorySystem", cle );
        }
    }

    /**
     * The project's remote repositories to use for the resolution of dependencies.
     * 
     * @throws EnforcerRuleException 
     */
    private List<RemoteRepository> getRemoteRepositories( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        try
        {
            return (List<RemoteRepository>) Objects.requireNonNull( helper.evaluate( "${project.remoteProjectRepositories}" ),
                    "${project.remoteProjectRepositories} is null");
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve project's remote repositories", eee );
        }
    }

    /**
     * The project's remote repositories to use for the resolution of plugins.
     * @throws EnforcerRuleException 
     */
    private List<RemoteRepository> getRemotePluginRepositories( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        try
        {
            return (List<RemoteRepository>) Objects.requireNonNull( helper.evaluate( "${project.remotePluginRepositories}" ),
                    "${project.remotePluginRepositories} is null");
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve project's remote plugin repositories", eee );
        }
    }

    public Map<Artifact, DependencyNode> getDependencies( MavenProject project, boolean searchTransitive ) throws DependencyCollectionException
    {
        Map<Artifact, DependencyNode> dependencies = null;
        DependencyNode node = collectDependencies(project);
        if ( searchTransitive )
        {
            // TODO:!
            dependencies = null; // ArtifactUtils.getAllDescendants( node );
        }
        else if ( node.getChildren() != null )
        {
            dependencies = new HashMap<>();
            for ( DependencyNode depNode : node.getChildren() )
            {
                dependencies.putIfAbsent( RepositoryUtils.toArtifact( depNode.getArtifact() ), depNode );
            }
        }
        return dependencies;
    }

    public DependencyNode collectDependencies(MavenProject project) throws DependencyCollectionException {
        List<org.eclipse.aether.graph.Dependency> resolverDeps = ArtifactUtils.toDependencies( project.getDependencies(), repoSession.getArtifactTypeRegistry() );
        List<org.eclipse.aether.graph.Dependency> resolvedManagedDeps = ArtifactUtils.toDependencies( project.getDependencyManagement().getDependencies(), repoSession.getArtifactTypeRegistry() );
        CollectRequest collectRequest = new CollectRequest( resolverDeps, resolvedManagedDeps, remoteRepositories );
        CollectResult collectResult = repoSystem.collectDependencies( repoSession, collectRequest );
        DependencyNode node = collectResult.getRoot();
        return node;
    }
}
