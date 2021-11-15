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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The Class TestBannedDependencies.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class TestBannedDependencies
{

    public static class ExcludesDoNotUseTransitiveDependencies
    {
        private BannedDependenciesTestSetup setup;

        @BeforeEach
        public void beforeMethod()
            throws IOException
        {
            this.setup = new BannedDependenciesTestSetup();
            this.setup.setSearchTransitive( false );
        }

        private void addExcludeAndRunRule( String toAdd )
            throws EnforcerRuleException
        {
            this.setup.addExcludeAndRunRule( toAdd );
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

        private BannedDependenciesTestSetup setup;

        @BeforeEach
        public void beforeMethod()
            throws IOException
        {
            this.setup = new BannedDependenciesTestSetup();
            this.setup.setSearchTransitive( true );
        }

        private void addExcludeAndRunRule( String toAdd )
            throws EnforcerRuleException
        {
            this.setup.addExcludeAndRunRule( toAdd );
        }

        @Test
        public void testGroupIdArtifactIdVersion()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "testGroupId:release:1.0" ) );
        }

        @Test
        public void testGroupIdArtifactId()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "testGroupId:release" ) );
        }

        @Test
        public void testGroupId()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "testGroupId" ) );
        }

        @Test
        public void testSpaceTrimmingGroupIdArtifactIdVersion()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "  testGroupId  :  release   :   1.0    " ) );
        }

        @Test
        public void groupIdArtifactIdVersionType()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "g:a:1.0:war" ) );
        }

        @Test
        public void groupIdArtifactIdVersionTypeScope()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "g:a:1.0:war:compile" ) );
        }

        // @Test(expected = EnforcerRuleException.class)
        // public void groupIdArtifactIdVersionTypeScopeClassifier() throws Exception {
        // addExcludeAndRunRule("g:compile:1.0:jar:compile:one");
        // }
        //
    }

    public static class WildcardExcludesUsingTransitiveDependencies
    {

        private BannedDependenciesTestSetup setup;

        @BeforeEach
        public void beforeMethod()
            throws IOException
        {
            this.setup = new BannedDependenciesTestSetup();
            this.setup.setSearchTransitive( true );
        }

        private void addExcludeAndRunRule( String toAdd )
            throws EnforcerRuleException
        {
            this.setup.addExcludeAndRunRule( toAdd );
        }

        @Test
        public void testWildcardForGroupIdArtifactIdVersion()
            throws Exception
        {
            addExcludeAndRunRule( "*:release:1.2" );
        }

        @Test
        public void testWildCardForGroupIdArtifactId()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "*:release" ) );
        }

        @Test
        public void testWildcardForGroupIdWildcardForArtifactIdVersion()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "*:*:1.0" ) );
        }

        @Test
        public void testWildcardForGroupIdArtifactIdWildcardForVersion()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "*:release:*" ) );
        }

    }

    public static class PartialWildcardExcludesUsingTransitiveDependencies
    {

        private BannedDependenciesTestSetup setup;

        @BeforeEach
        public void beforeMethod()
            throws IOException
        {
            this.setup = new BannedDependenciesTestSetup();
            this.setup.setSearchTransitive( true );
        }

        private void addExcludeAndRunRule( String toAdd )
            throws EnforcerRuleException
        {
            this.setup.addExcludeAndRunRule( toAdd );
        }

        @Test
        public void groupIdArtifactIdWithWildcard()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "testGroupId:re*" ) );
        }

        @Test
        public void groupIdArtifactIdVersionTypeWildcardScope()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "g:a:1.0:war:co*" ) );
        }

        @Test
        public void groupIdArtifactIdVersionWildcardTypeScope()
        {
            assertThrows( EnforcerRuleException.class, () -> addExcludeAndRunRule( "g:a:1.0:w*:compile" ) );
        }
    }

    public static class IllegalFormatsTests
    {
        private BannedDependenciesTestSetup setup;

        @BeforeEach
        public void beforeMethod()
            throws IOException
        {
            this.setup = new BannedDependenciesTestSetup();
            this.setup.setSearchTransitive( true );
        }

        private void addExcludeAndRunRule( String toAdd )
            throws EnforcerRuleException
        {
            this.setup.addExcludeAndRunRule( toAdd );
        }

        @Test
        public void onlyThreeColonsWithoutAnythingElse()
        {
            assertThrows( IllegalArgumentException.class, () -> addExcludeAndRunRule( ":::" ) );
        }

        @Test
        public void onlySevenColonsWithoutAnythingElse()
        {
            assertThrows( IllegalArgumentException.class, () -> addExcludeAndRunRule( ":::::::" ) );
        }

    }

    public static class IncludesExcludesNoTransitive
    {
        private BannedDependenciesTestSetup setup;

        @BeforeEach
        public void beforeMethod()
            throws IOException
        {
            this.setup = new BannedDependenciesTestSetup();
            this.setup.setSearchTransitive( false );
        }

        private void addIncludeExcludeAndRunRule( String incAdd, String excAdd )
            throws EnforcerRuleException
        {
            this.setup.addIncludeExcludeAndRunRule( incAdd, excAdd );
        }

        @Test
        public void includeEverythingAndExcludeEverythign()
            throws EnforcerRuleException
        {
            addIncludeExcludeAndRunRule( "*", "*" );
        }

        @Test
        public void includeEverythingAndExcludeEveryGroupIdAndScopeRuntime()
            throws EnforcerRuleException
        {
            addIncludeExcludeAndRunRule( "*", "*:runtime" );
        }

        @Test
        public void includeEverythingAndExcludeEveryGroupIdAndScopeRuntimeYYYY()
        {
            assertThrows( EnforcerRuleException.class, () -> addIncludeExcludeAndRunRule( "*:test", "*:runtime" ) );
        }
    }

}
