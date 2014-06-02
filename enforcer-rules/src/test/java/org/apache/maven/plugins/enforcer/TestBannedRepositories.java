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
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Test the "banned repositories" rule.
 * 
 * @author <a href="mailto:wangyf2010@gmail.com">Simon Wang</a>
 */
public class TestBannedRepositories
    extends PlexusTestCase
{
    private EnforcerRuleHelper helper;

    private BannedRepositories rule;

    private MockProject project;

    public void setUp()
        throws Exception
    {
        super.setUp();

        rule = new BannedRepositories();
        rule.setMessage( "my message" );

        project = new MockProject();
        project.setGroupId( "org.apache.maven.plugins.enforcer.test" );
        project.setVersion( "1.0-SNAPSHOT" );

        helper = EnforcerTestUtils.getHelper( project );
    }

    public void testNoCheckRules()
        throws EnforcerRuleException
    {
        DefaultArtifactRepository repo1 = new DefaultArtifactRepository( "repo1", "http://repo1/", null );
        List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
        repos.add( repo1 );

        project.setRemoteArtifactRepositories( repos );
        project.setPluginArtifactRepositories( repos );

        rule.execute( helper );
    }

    public void testBannedRepositories()
    {
        DefaultArtifactRepository repo1 = new DefaultArtifactRepository( "repo1", "http://repo1/", null );
        DefaultArtifactRepository repo2 = new DefaultArtifactRepository( "repo1", "http://repo1/test", null );
        DefaultArtifactRepository repo3 = new DefaultArtifactRepository( "repo1", "http://repo2/test", null );
        List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
        repos.add( repo1 );
        repos.add( repo2 );
        repos.add( repo3 );

        project.setRemoteArtifactRepositories( repos );
        project.setPluginArtifactRepositories( repos );

        List<String> bannedRepositories = new ArrayList<String>();
        String pattern1 = "http://repo1/*";

        bannedRepositories.add( pattern1 );

        rule.setBannedRepositories( bannedRepositories );

        try
        {
            rule.execute( helper );
            fail( "should throw exception" );
        }
        catch ( EnforcerRuleException e )
        {
        }

    }

    public void testAllowedRepositoriesAllOK()
        throws EnforcerRuleException
    {
        DefaultArtifactRepository repo1 = new DefaultArtifactRepository( "repo1", "http://repo1/", null );
        DefaultArtifactRepository repo2 = new DefaultArtifactRepository( "repo1", "http://repo1/test", null );

        List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
        repos.add( repo1 );
        repos.add( repo2 );

        project.setRemoteArtifactRepositories( repos );
        project.setPluginArtifactRepositories( repos );

        List<String> bannedRepositories = new ArrayList<String>();
        String pattern1 = "http://repo1/*";

        bannedRepositories.add( pattern1 );

        rule.setAllowedRepositories( bannedRepositories );
        rule.setAllowedPluginRepositories( bannedRepositories );

        rule.execute( helper );
    }

    public void testAllowedRepositoriesException()
    {
        DefaultArtifactRepository repo1 = new DefaultArtifactRepository( "repo1", "http://repo1/", null );
        DefaultArtifactRepository repo2 = new DefaultArtifactRepository( "repo1", "http://repo1/test", null );
        DefaultArtifactRepository repo3 = new DefaultArtifactRepository( "repo1", "http://repo2/test", null );
        List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>();
        repos.add( repo1 );
        repos.add( repo2 );
        repos.add( repo3 );

        project.setRemoteArtifactRepositories( repos );
        project.setPluginArtifactRepositories( repos );

        List<String> patterns = new ArrayList<String>();
        String pattern1 = "http://repo1/*";

        patterns.add( pattern1 );

        rule.setAllowedPluginRepositories( patterns );
        rule.setAllowedRepositories( patterns );

        try
        {
            rule.execute( helper );
            fail( "should throw exception" );
        }
        catch ( EnforcerRuleException e )
        {
        }

    }
}
