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

import java.util.Collections;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Test the "require no repositories" rule.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class TestRequireNoRepositories
    extends PlexusTestCase
{
    private EnforcerRuleHelper helper;

    private RequireNoRepositories rule;

    private MockProject project;

    public void setUp()
        throws Exception
    {
        super.setUp();

        rule = new RequireNoRepositories();
        rule.message = "my message";

        project = new MockProject();
        project.setGroupId( "org.apache.maven.plugins.enforcer.test" );
        project.setVersion( "1.0-SNAPSHOT" );

        helper = EnforcerTestUtils.getHelper( project );
    }

    public void testAllBannedNoRepositories()
        throws EnforcerRuleException
    {
        project.setArtifactId( "no-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/no-repositories/child" ) );

        rule.execute( helper );
    }

    public void testAllBannedWithRepositories()
        throws EnforcerRuleException
    {
        project.setArtifactId( "with-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/with-repositories/child" ) );

        try
        {
            rule.execute( helper );
            fail( "Should have exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertTrue( true );
        }
    }

    public void testAllBannedWithAllowedRepositories()
        throws EnforcerRuleException
    {
        rule.allowedRepositories = Collections.singletonList( "repo" );

        project.setArtifactId( "with-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/with-repositories/child" ) );

        rule.execute( helper );
    }

    public void testAllBannedWithAllowedPluginRepositories()
        throws EnforcerRuleException
    {
        rule.allowedPluginRepositories = Collections.singletonList( "repo" );

        project.setArtifactId( "with-plugin-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/with-plugin-repositories/child" ) );

        rule.execute( helper );
    }

    public void testReposNotBannedNoRepositories()
        throws EnforcerRuleException
    {
        rule.banRepositories = false;

        project.setArtifactId( "no-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/no-repositories/child" ) );

        rule.execute( helper );
    }

    public void testReposNotBannedWithRepositories()
        throws EnforcerRuleException
    {
        rule.banRepositories = false;

        project.setArtifactId( "with-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/with-repositories/child" ) );

        rule.execute( helper );
    }

    public void testReposNotBannedWithPluginRepositories()
        throws EnforcerRuleException
    {
        rule.banRepositories = false;

        project.setArtifactId( "with-plugin-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/with-plugin-repositories/child" ) );

        try
        {
            rule.execute( helper );
            fail( "Should have exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertTrue( true );
        }
    }

    public void testPluginReposNotBannedNoRepositories()
        throws EnforcerRuleException
    {
        rule.banPluginRepositories = false;

        project.setArtifactId( "no-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/no-repositories/child" ) );

        rule.execute( helper );
    }

    public void testPluginReposNotBannedWithRepositories()
        throws EnforcerRuleException
    {
        rule.banPluginRepositories = false;

        project.setArtifactId( "with-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/with-repositories/child" ) );

        try
        {
            rule.execute( helper );
            fail( "Should have exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertTrue( true );
        }
    }

    public void testPluginReposNotBannedWithPluginRepositories()
        throws EnforcerRuleException
    {
        rule.banPluginRepositories = false;

        project.setArtifactId( "with-plugin-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/with-plugin-repositories/child" ) );

        rule.execute( helper );
    }

    public void testReposNotAllowedWithSnapshotRepositories()
        throws EnforcerRuleException
    {
        rule.allowSnapshotRepositories = true;

        project.setArtifactId( "snapshot-plugin-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/snapshot-plugin-repositories/child" ) );

        try
        {
            rule.execute( helper );
            fail( "Should have exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertTrue( true );
        }
    }

    public void testReposAllowedWithSnapshotRepositories()
        throws EnforcerRuleException
    {
        rule.allowSnapshotRepositories = true;

        project.setArtifactId( "snapshot-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/snapshot-repositories/child" ) );

        rule.execute( helper );
    }

    public void testPluginReposNotAllowedWithSnapshotRepositories()
        throws EnforcerRuleException
    {
        rule.allowSnapshotPluginRepositories = true;

        project.setArtifactId( "snapshot-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/snapshot-repositories/child" ) );

        try
        {
            rule.execute( helper );
            fail( "Should have exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertTrue( true );
        }
    }

    public void testPluginReposAllowedWithSnapshotPluginRepositories()
        throws EnforcerRuleException
    {
        rule.allowSnapshotPluginRepositories = true;

        project.setArtifactId( "snapshot-plugin-repositories-child" );
        project.setBaseDir( getTestFile( "target/test-classes/requireNoRepositories/snapshot-plugin-repositories/child" ) );

        rule.execute( helper );
    }

    /**
     * Test id.
     */
    public void testId()
    {
        RequireNoRepositories rule = new RequireNoRepositories();
        rule.getCacheId();
    }
}
