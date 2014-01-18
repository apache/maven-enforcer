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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.fail;

// TODO: Auto-generated Javadoc
/**
 * The Class TestBannedDependencies.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@RunWith( Enclosed.class )
public class TestBannedDependencies
{
    public static class ExcludesDoNotUseTransitiveDependencies
    {

        private List<String> excludes;

        private BannedDependencies rule;

        private EnforcerRuleHelper helper;

        @Before
        public void beforeMethod()
            throws IOException
        {
            ArtifactStubFactory factory = new ArtifactStubFactory();

            MockProject project = new MockProject();
            project.setArtifacts( factory.getMixedArtifacts() );
            project.setDependencyArtifacts( factory.getScopedArtifacts() );

            helper = EnforcerTestUtils.getHelper( project );
            rule = newBannedDependenciesRule();

            excludes = new ArrayList<String>();
            rule.setExcludes( excludes );
            rule.setMessage( null );

            rule.setSearchTransitive( false );
        }

        private void addExcludeAndRunRule( String toAdd )
            throws EnforcerRuleException
        {
            excludes.add( toAdd );
            rule.execute( helper );
        }

        @Test
        public void testGroupIdArtifactIdVersion()
            throws Exception
        {
            addExcludeAndRunRule( "testGroupId:release:1.0" );
        }

        @Test
        public void testGroupIdArtifactId()
            throws Exception
        {
            addExcludeAndRunRule( "testGroupId:release" );
        }

        @Test
        public void testGroupId()
            throws Exception
        {
            addExcludeAndRunRule( "testGroupId" );
        }

    }

    public static class ExcludesUsingTransitiveDependencies
    {

        private List<String> excludes;

        private BannedDependencies rule;

        private EnforcerRuleHelper helper;

        @Before
        public void beforeMethod()
            throws IOException
        {
            ArtifactStubFactory factory = new ArtifactStubFactory();

            MockProject project = new MockProject();
            project.setArtifacts( factory.getMixedArtifacts() );
            project.setDependencyArtifacts( factory.getScopedArtifacts() );

            helper = EnforcerTestUtils.getHelper( project );
            rule = newBannedDependenciesRule();

            excludes = new ArrayList<String>();
            rule.setExcludes( excludes );
            rule.setMessage( null );
            rule.setSearchTransitive( true );
        }

        private void addExcludeAndRunRule( String toAdd )
            throws EnforcerRuleException
        {
            excludes.add( toAdd );
            rule.execute( helper );
        }

        @Test( expected = EnforcerRuleException.class )
        public void testGroupIdArtifactIdVersion()
            throws Exception
        {
            addExcludeAndRunRule( "testGroupId:release:1.0" );
        }

        @Test( expected = EnforcerRuleException.class )
        public void testGroupIdArtifactId()
            throws Exception
        {
            addExcludeAndRunRule( "testGroupId:release" );
        }

        @Test( expected = EnforcerRuleException.class )
        public void testGroupId()
            throws Exception
        {
            addExcludeAndRunRule( "testGroupId" );
        }

        @Test( expected = EnforcerRuleException.class )
        public void testSpaceTrimmingGroupIdArtifactIdVersion()
            throws Exception
        {
            addExcludeAndRunRule( "  testGroupId  :  release   :   1.0    " );
        }

        @Test( expected = EnforcerRuleException.class )
        public void groupIdArtifactIdVersionType()
            throws Exception
        {
            addExcludeAndRunRule( "g:a:1.0:war" );
        }

        @Test( expected = EnforcerRuleException.class )
        public void groupIdArtifactIdVersionTypeScope()
            throws Exception
        {
            addExcludeAndRunRule( "g:a:1.0:war:compile" );
        }

        // @Test(expected = EnforcerRuleException.class)
        // public void groupIdArtifactIdVersionTypeScopeClassifier() throws Exception {
        // addExcludeAndRunRule("g:compile:1.0:jar:compile:one");
        // }
        //
    }

    public static class WildcardExcludesUsingTransitiveDependencies
    {

        private List<String> excludes;

        private BannedDependencies rule;

        private EnforcerRuleHelper helper;

        @Before
        public void beforeMethod()
            throws IOException
        {
            ArtifactStubFactory factory = new ArtifactStubFactory();

            MockProject project = new MockProject();
            project.setArtifacts( factory.getMixedArtifacts() );
            project.setDependencyArtifacts( factory.getScopedArtifacts() );

            helper = EnforcerTestUtils.getHelper( project );
            rule = newBannedDependenciesRule();

            rule.setMessage( null );

            excludes = new ArrayList<String>();
            rule.setExcludes( excludes );
            rule.setSearchTransitive( true );
        }

        private void addExcludeAndRunRule( String toAdd )
            throws EnforcerRuleException
        {
            excludes.add( toAdd );
            rule.execute( helper );
        }

        @Test
        public void testWildcardForGroupIdArtifactIdVersion()
            throws Exception
        {
            addExcludeAndRunRule( "*:release:1.2" );
        }

        @Test( expected = EnforcerRuleException.class )
        public void testWildCardForGroupIdArtifactId()
            throws Exception
        {
            addExcludeAndRunRule( "*:release" );
        }

        @Test( expected = EnforcerRuleException.class )
        public void testWildcardForGroupIdWildcardForArtifactIdVersion()
            throws Exception
        {
            addExcludeAndRunRule( "*:*:1.0" );
        }

        @Test( expected = EnforcerRuleException.class )
        public void testWildcardForGroupIdArtifactIdWildcardForVersion()
            throws Exception
        {
            addExcludeAndRunRule( "*:release:*" );
        }

    }

    public static class PartialWildcardExcludesUsingTransitiveDependencies
    {

        private List<String> excludes;

        private BannedDependencies rule;

        private EnforcerRuleHelper helper;

        @Before
        public void beforeMethod()
            throws IOException
        {
            ArtifactStubFactory factory = new ArtifactStubFactory();

            MockProject project = new MockProject();
            project.setArtifacts( factory.getMixedArtifacts() );
            project.setDependencyArtifacts( factory.getScopedArtifacts() );

            helper = EnforcerTestUtils.getHelper( project );
            rule = newBannedDependenciesRule();

            rule.setMessage( null );

            excludes = new ArrayList<String>();
            rule.setExcludes( excludes );
            rule.setSearchTransitive( true );
        }

        private void addExcludeAndRunRule( String toAdd )
            throws EnforcerRuleException
        {
            excludes.add( toAdd );
            rule.execute( helper );
        }

        @Test( expected = EnforcerRuleException.class )
        public void groupIdArtifactIdWithWildcard()
            throws EnforcerRuleException
        {
            addExcludeAndRunRule( "testGroupId:re*" );
        }

        @Test( expected = EnforcerRuleException.class )
        public void groupIdArtifactIdVersionTypeWildcardScope()
            throws EnforcerRuleException
        {
            addExcludeAndRunRule( "g:a:1.0:war:co*" );
        }

        @Test( expected = EnforcerRuleException.class )
        public void groupIdArtifactIdVersionWildcardTypeScope()
            throws EnforcerRuleException
        {
            addExcludeAndRunRule( "g:a:1.0:w*:compile" );
        }
    }

    public static class IllegalFormatsTests
    {
        private List<String> excludes;

        private BannedDependencies rule;

        private EnforcerRuleHelper helper;

        @Before
        public void beforeMethod()
            throws IOException
        {
            ArtifactStubFactory factory = new ArtifactStubFactory();

            MockProject project = new MockProject();
            project.setArtifacts( factory.getMixedArtifacts() );
            project.setDependencyArtifacts( factory.getScopedArtifacts() );

            helper = EnforcerTestUtils.getHelper( project );
            rule = newBannedDependenciesRule();

            rule.setMessage( null );

            excludes = new ArrayList<String>();
            rule.setExcludes( excludes );
            rule.setSearchTransitive( true );
        }

        private void addExcludeAndRunRule( String toAdd )
            throws EnforcerRuleException
        {
            excludes.add( toAdd );
            rule.execute( helper );
        }

        @Test( expected = IllegalArgumentException.class )
        public void onlyThreeColonsWithoutAnythingElse()
            throws EnforcerRuleException
        {
            addExcludeAndRunRule( ":::" );
        }

        @Test( expected = IllegalArgumentException.class )
        public void onlySevenColonsWithoutAnythingElse()
            throws EnforcerRuleException
        {
            addExcludeAndRunRule( ":::::::" );
        }

    }

    /**
     * Test includes.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testIncludes()
        throws IOException
    {
        ArtifactStubFactory factory = new ArtifactStubFactory();
        MockProject project = new MockProject();
        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
        project.setArtifacts( factory.getMixedArtifacts() );
        project.setDependencyArtifacts( factory.getScopedArtifacts() );
        BannedDependencies rule = newBannedDependenciesRule();

        List<String> excludes = new ArrayList<String>();
        List<String> includes = new ArrayList<String>();

        rule.setSearchTransitive( false );

        excludes.add( "*" );
        includes.add( "*" );

        rule.setExcludes( excludes );
        rule.setIncludes( includes );

        execute( rule, helper, false );

        excludes.clear();
        excludes.add( "*:runtime" );
        rule.setExcludes( excludes );

        execute( rule, helper, false );

        includes.clear();
        includes.add( "*:test" );
        rule.setIncludes( includes );
        execute( rule, helper, true );

    }

    private static BannedDependencies newBannedDependenciesRule()
    {
        BannedDependencies rule = new BannedDependencies()
        {
            @Override
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
     * Simpler wrapper to execute and deal with the expected result.
     * 
     * @param rule the rule
     * @param helper the helper
     * @param shouldFail the should fail
     */
    private void execute( BannedDependencies rule, EnforcerRuleHelper helper, boolean shouldFail )
    {
        try
        {
            rule.setMessage( null );
            rule.execute( helper );
            if ( shouldFail )
            {
                fail( "Exception expected." );
            }
        }
        catch ( EnforcerRuleException e )
        {
            if ( !shouldFail )
            {
                fail( "No Exception expected:" + e.getLocalizedMessage() );
            }
            // helper.getLog().debug(e.getMessage());
        }
    }
}
