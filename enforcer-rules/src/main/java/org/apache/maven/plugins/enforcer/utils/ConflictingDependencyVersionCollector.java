package org.apache.maven.plugins.enforcer.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

public class ConflictingDependencyVersionCollector implements DependencyVisitor {

    private final Map<Key, List<DependencyNode>> keysToNodes;

    ConflictingDependencyVersionCollector() {
        keysToNodes = new HashMap<>();
    }
    private static final class Key
    {
        private final String artifactId;
        private final String groupId;

        public Key(Artifact artifact) {
            this(artifact.getArtifactId(), artifact.getGroupId());
        }
        public Key(String artifactId, String groupId) {
            super();
            this.artifactId = artifactId;
            this.groupId = groupId;
        }
    }
    
    @Override
    public boolean visitEnter( DependencyNode node )
    {
        Key key = new Key(node.getArtifact());
        keysToNodes.merge(key, new LinkedList<DependencyNode>(node), nodes -> nodes.add( node ));
        return false;
    }

    @Override
    public boolean visitLeave( DependencyNode node )
    {
        // TODO Auto-generated method stub
        return false;
    }

}
