package org.apache.maven.plugins.enforcer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugins.enforcer.utils.TestEnforcerRuleUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTree;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.junit.Before;
import org.junit.Test;

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

/**
 * Tests for {@link RequireUpperBoundDeps}.
 * @author Oleg Nenashev
 */
public class TestRequireUpperBoundDeps {
    
    private MavenProject project;

    private EnforcerRuleHelper helper;

    private ArtifactStubFactory factory;

    private RequireUpperBoundDeps rule;
    private MockDependencyTreeBuilder dependencyTree;
    
    Artifact library1_10, library1_20, library2_10;
    
    @Before
    public void before() throws Exception
    {
        PlexusContainer c = new DefaultPlexusContainer();
        dependencyTree = new MockDependencyTreeBuilder();
        factory = new ArtifactStubFactory();
        
        project = new MockProject();
        project.setArtifact(factory.createArtifact("my", "project", "1.0"));
        library1_10 = factory.createArtifact("my", "library1", "1.0");
        library1_20 = factory.createArtifact("my", "library1", "2.0");
        library2_10 = factory.createArtifact("my", "library2", "1.0");
        
        helper = EnforcerTestUtils.getHelper(project, false, c);
        helper.getContainer().addComponent(dependencyTree, DependencyTreeBuilder.class.getName());
        
        rule = new RequireUpperBoundDeps();  
    }
    
    @Test
    public void testShouldPassForSameDependencies() throws Exception {
        dependencyTree.addDependency(library1_10);
        DependencyNode n = new DependencyNode(library2_10);
        n.addChild(new DependencyNode(library1_10));
        dependencyTree.addDependency(n);
                
        TestEnforcerRuleUtils.execute(rule, helper, false);
    }
    
    @Test
    public void testShouldFailForNewerDependencies() throws Exception {
        dependencyTree.addDependency(library1_10);
        DependencyNode n = new DependencyNode(library2_10);
        n.addChild(new DependencyNode(library1_20));
        dependencyTree.addDependency(n);
                
        TestEnforcerRuleUtils.execute(rule, helper, true);
    }
    
    @Test
    public void testShouldPassForOlderDependencies() throws Exception {
        dependencyTree.addDependency(library1_20);
        DependencyNode n = new DependencyNode(library2_10);
        n.addChild(new DependencyNode(library1_10));
        dependencyTree.addDependency(n);
                
        TestEnforcerRuleUtils.execute(rule, helper, false);
    }
    
    // MENFORCER-273
    @Test
    public void testShouldPassForOlderDependencyIfExcluded() throws Exception {
        dependencyTree.addDependency(library1_10);
        DependencyNode n = new DependencyNode(library2_10);
        n.addChild(new DependencyNode(library1_20));
        dependencyTree.addDependency(n);
                
        rule.setExcludes(Arrays.asList("my:library1"));
        TestEnforcerRuleUtils.execute(rule, helper, false);
    }
    
    // MENFORCER-276
    @Test
    public void testShouldPassIfTestArtifactsAreIgnored() throws Exception {
        dependencyTree.addDependency(library1_10);
        
        library2_10.setScope("test");
        DependencyNode n = new DependencyNode(library2_10);
        n.addChild(new DependencyNode(library1_20));
        dependencyTree.addDependency(n);
                
        rule.setIgnoreDependencyScopes(Arrays.asList("test"));
        TestEnforcerRuleUtils.execute(rule, helper, false);
    }
    
    // MENFORCER-276
    @Test
    public void testShouldFailIfWrongScopeIsIgnored() throws Exception {
        testShouldPassIfTestArtifactsAreIgnored();   
        rule.setIgnoreDependencyScopes(Arrays.asList("provided"));
        TestEnforcerRuleUtils.execute(rule, helper, true);
    }
    
    // TODO: make it a generic class
    private static final class MockDependencyTreeBuilder implements DependencyTreeBuilder {

        List<DependencyNode> dependencies = new ArrayList<DependencyNode>();    
       
        public void addDependency(DependencyNode node) {
            dependencies.add(node);
        }
        
        public void addDependency(Artifact artifact) {
            dependencies.add(new DependencyNode(artifact));
        }
        
        @Override
        public DependencyNode buildDependencyTree(MavenProject project, ArtifactRepository repository, 
                ArtifactFactory factory, ArtifactMetadataSource metadataSource, ArtifactFilter filter, 
                ArtifactCollector collector) throws DependencyTreeBuilderException {
            DependencyNode root = new DependencyNode(project.getArtifact());
            for (DependencyNode child : dependencies) {
                root.addChild(child);
            }
            return root;
        }
        
        @Override
        public DependencyTree buildDependencyTree(MavenProject project, ArtifactRepository repository, 
                ArtifactFactory factory, ArtifactMetadataSource metadataSource, 
                ArtifactCollector collector) throws DependencyTreeBuilderException {
            throw new UnsupportedOperationException("Not supported yet.");
        }


        @Override
        public DependencyNode buildDependencyTree(MavenProject project) throws DependencyTreeBuilderException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public DependencyNode buildDependencyTree(MavenProject arg0, ArtifactRepository arg1, ArtifactFilter arg2) throws DependencyTreeBuilderException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
}
