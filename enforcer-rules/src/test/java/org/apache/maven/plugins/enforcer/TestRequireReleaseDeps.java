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

import java.io.IOException;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugins.enforcer.utils.EnforcerRuleUtilsHelper;
import org.apache.maven.project.MavenProject;

/**
 * The Class TestNoSnapshots.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class TestRequireReleaseDeps
    extends TestCase
{

    /**
     * Test rule.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void testRule()
        throws IOException
    {

        ArtifactStubFactory factory = new ArtifactStubFactory();
        MockProject project = new MockProject();
        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
        project.setArtifacts( factory.getMixedArtifacts() );
        project.setDependencyArtifacts( factory.getScopedArtifacts() );
        RequireReleaseDeps rule = newRequireReleaseDeps();
        rule.setSearchTransitive( false );

        EnforcerRuleUtilsHelper.execute( rule, helper, false );

        rule.setSearchTransitive( true );

        EnforcerRuleUtilsHelper.execute( rule, helper, true );

        // test onlyWhenRelease in each case

        project.setArtifact( factory.getSnapshotArtifact() );

        EnforcerRuleUtilsHelper.execute( rule, helper, true );

        rule.setOnlyWhenRelease( true );

        EnforcerRuleUtilsHelper.execute( rule, helper, false );

        project.setArtifact( factory.getReleaseArtifact() );

        EnforcerRuleUtilsHelper.execute( rule, helper, true );

        MockProject parent = new MockProject();
        parent.setArtifact( factory.getSnapshotArtifact() );
        project.setParent( parent );
        project.setArtifacts( null );
        project.setDependencyArtifacts( null );
        helper = EnforcerTestUtils.getHelper(project);

        rule.setFailWhenParentIsSnapshot( true );
        EnforcerRuleUtilsHelper.execute( rule, helper, true );

        rule.setFailWhenParentIsSnapshot( false );
        EnforcerRuleUtilsHelper.execute( rule, helper, false );


    }

    private RequireReleaseDeps newRequireReleaseDeps()
    {
        RequireReleaseDeps rule = new RequireReleaseDeps()
        {
            protected Set<Artifact> getDependenciesToCheck( MavenProject project )
            {
                // the integration with dependencyGraphTree is verified with the integration tests
                // for unit-testing 
                return isSearchTransitive() ? project.getArtifacts() : project.getDependencyArtifacts();
            }
        };        
        return rule;
    }

    /**
     * Test id.
     */
    public void testId()
    {
        RequireReleaseDeps rule = newRequireReleaseDeps();
        rule.getCacheId();
    }
}
