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

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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
        private BannedDependenciesTestSetup setup;

        @Before
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

        @Before
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

        private BannedDependenciesTestSetup setup;
        

        @Before
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
    
    public static class ExcludesUsingExcludesUrl
    {

        private BannedDependenciesTestSetup setup;
        private Server server = null;
        private String excludesUrl = "http://localhost:8081/";
        private String invalidExcludesUrl = "http://hgggkhkh";
        private String blacklistFilename = "./target/test-classes/testBannedDependencies/blacklist.txt";
        
        @Before
        public void beforeMethod()
            throws Exception
        {
            this.setup = new BannedDependenciesTestSetup();
            this.setup.setSearchTransitive( true );
            startEmbeddedJettyServer();
        }
        
        private void startEmbeddedJettyServer() throws Exception{
          
          if (server == null) {
            server = new Server(8081);
            server.setStopAtShutdown(true);
            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setResourceBase(blacklistFilename);

            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[] { resourceHandler });
            server.setHandler(handlers);
            server.start();
            //System.out.println("Jetty server started..isServerRunning="+server.isRunning());  
          }
        }
 
        private void addExcludeUrlAndRunRule( String url )
            throws EnforcerRuleException
        { 
            this.setup.addExcludeUrlAndRunRule( url);
        }
        
        @Test( expected = EnforcerRuleException.class ) 
        public void testInvalidExcludesUrl()
            throws Exception
        {
          addExcludeUrlAndRunRule( invalidExcludesUrl );
        }
        
        @Test( expected = EnforcerRuleException.class )
        public void testBlacklistedParent()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\npg1:pa1:[4.0]" );
          addExcludeUrlAndRunRule( excludesUrl );
        }
        
        @Test( expected = EnforcerRuleException.class )
        public void testWildcardBlacklistedParent()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\npg1:pa1:*" );
          addExcludeUrlAndRunRule( excludesUrl );
        }

        @Test
        public void testWildcardForGroupIdArtifactIdVersion()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\n*:release:1.2" );
          addExcludeUrlAndRunRule( excludesUrl );
          
        }

        @Test( expected = EnforcerRuleException.class )
        public void testWildCardForGroupIdArtifactId()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\n*:release" );
          addExcludeUrlAndRunRule( excludesUrl );
          
        }

        @Test( expected = EnforcerRuleException.class )
        public void testWildcardForGroupIdWildcardForArtifactIdVersion()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\n*:*:1.0" );
          addExcludeUrlAndRunRule( excludesUrl );
          
        }

        @Test( expected = EnforcerRuleException.class )
        public void testWildcardForGroupIdArtifactIdWildcardForVersion()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\n*:release:*" );
          addExcludeUrlAndRunRule( excludesUrl );
        }
        
        @Test( expected = EnforcerRuleException.class )
        public void testGroupId()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\ntestGroupId" );
          addExcludeUrlAndRunRule( excludesUrl );
        }
        
        @Test( expected = EnforcerRuleException.class )
        public void testGroupIdArtifactIdVersion()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\ntestGroupId:release:1.0" );
          addExcludeUrlAndRunRule( excludesUrl );
        }

        @Test( expected = EnforcerRuleException.class )
        public void testGroupIdArtifactId()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\ntestGroupId:release" );
          addExcludeUrlAndRunRule( excludesUrl );
        }

        @Test( expected = EnforcerRuleException.class )
        public void testSpaceTrimmingGroupIdArtifactIdVersion()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\n  testGroupId  :  release   :   1.0    " );
          addExcludeUrlAndRunRule( excludesUrl );
        }

        @Test( expected = EnforcerRuleException.class )
        public void groupIdArtifactIdVersionType()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\ng:a:1.0:war" );
          addExcludeUrlAndRunRule( excludesUrl );
        }

        @Test( expected = EnforcerRuleException.class )
        public void groupIdArtifactIdVersionTypeScope()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\ng:a:1.0:war:compile" );
          addExcludeUrlAndRunRule( excludesUrl );
        }
        
        @Test( expected = IllegalArgumentException.class )
        public void onlyThreeColonsWithoutAnythingElse()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\n:::" );
          addExcludeUrlAndRunRule( excludesUrl );
        }

        @Test( expected = IllegalArgumentException.class )
        public void onlySevenColonsWithoutAnythingElse()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\n:::::::" );
          addExcludeUrlAndRunRule( excludesUrl );
        }
        
        @Test( expected = EnforcerRuleException.class )
        public void groupIdArtifactIdWithWildcard()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\ntestGroupId:re*" );
          addExcludeUrlAndRunRule( excludesUrl );
        }

        @Test( expected = EnforcerRuleException.class )
        public void groupIdArtifactIdVersionTypeWildcardScope()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\ng:a:1.0:war:co*" );
          addExcludeUrlAndRunRule( excludesUrl );
        }

        @Test( expected = EnforcerRuleException.class )
        public void groupIdArtifactIdVersionWildcardTypeScope()
            throws Exception
        {
          FileUtils.fileAppend( blacklistFilename, "\ng:a:1.0:w*:compile" );
          addExcludeUrlAndRunRule( excludesUrl );
        }
        
        @After
        public void shutDownEmbeddedJettyServer() throws Exception{
          if (server != null)
          {
            server.stop();
          }
          FileUtils.fileDelete(blacklistFilename);
        }

    }

    public static class PartialWildcardExcludesUsingTransitiveDependencies
    {

        private BannedDependenciesTestSetup setup;

        @Before
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
        private BannedDependenciesTestSetup setup;

        @Before
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

    public static class IncludesExcludesNoTransitive
    {
        private BannedDependenciesTestSetup setup;

        @Before
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

        @Test( expected = EnforcerRuleException.class )
        public void includeEverythingAndExcludeEveryGroupIdAndScopeRuntimeYYYY()
            throws EnforcerRuleException
        {
            addIncludeExcludeAndRunRule( "*:test", "*:runtime" );
        }
    }

}
