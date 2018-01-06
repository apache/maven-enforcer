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

import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugins.enforcer.utils.EnforcerRuleUtilsHelper;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Test class for the RequireSnapshotVersion rule.
 */
public class TestRequireSnapshotVersion
{

    private MavenProject project;

    private EnforcerRuleHelper helper;

    private ArtifactStubFactory factory;

    private RequireSnapshotVersion rule;

    @Before
    public void before()
    {
        project = new MockProject();
        helper = EnforcerTestUtils.getHelper( project );
        factory = new ArtifactStubFactory();
        rule = new RequireSnapshotVersion();
    }

    @Test
    public void testRequireSnapshot()
        throws IOException
    {
        project.setArtifact( factory.getReleaseArtifact() );
        EnforcerRuleUtilsHelper.execute( rule, helper, true );

        project.setArtifact( factory.getSnapshotArtifact() );
        EnforcerRuleUtilsHelper.execute( rule, helper, false );
    }

    @Test
    public void testWithParentShouldFail()
        throws IOException
    {
        project.setArtifact( factory.getSnapshotArtifact() );
        rule.setFailWhenParentIsRelease( true );

        MockProject parent = new MockProject();
        parent.setArtifact( factory.getReleaseArtifact() );
        project.setParent( parent );
        EnforcerRuleUtilsHelper.execute( rule, helper, true );

        parent = new MockProject();
        parent.setArtifact( factory.getSnapshotArtifact() );
        project.setParent( parent );
        EnforcerRuleUtilsHelper.execute( rule, helper, false );
    }

    @Test
    public void testWithParentShouldPass()
        throws IOException
    {
        project.setArtifact( factory.getSnapshotArtifact() );
        rule.setFailWhenParentIsRelease( false );

        MockProject parent = new MockProject();
        parent.setArtifact( factory.getReleaseArtifact() );
        project.setParent( parent );
        EnforcerRuleUtilsHelper.execute( rule, helper, false );

        parent = new MockProject();
        parent.setArtifact( factory.getSnapshotArtifact() );
        project.setParent( parent );
        EnforcerRuleUtilsHelper.execute( rule, helper, false );
    }

}
