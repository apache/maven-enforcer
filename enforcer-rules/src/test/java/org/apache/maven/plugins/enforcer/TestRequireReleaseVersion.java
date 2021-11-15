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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugins.enforcer.utils.EnforcerRuleUtilsHelper;
import org.junit.jupiter.api.Test;

/**
 * The Class TestRequireReleaseVersion.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class TestRequireReleaseVersion
{

    /**
     * Test mojo.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testMojo()
        throws IOException
    {
        ArtifactStubFactory factory = new ArtifactStubFactory();
        MockProject project = new MockProject();
        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );

        project.setArtifact( factory.getReleaseArtifact() );

        RequireReleaseVersion rule = new RequireReleaseVersion();

        EnforcerRuleUtilsHelper.execute( rule, helper, false );

        project.setArtifact( factory.getSnapshotArtifact() );

        EnforcerRuleUtilsHelper.execute( rule, helper, true );

        project.setArtifact( factory.getReleaseArtifact() );

        MockProject parent = new MockProject();
        parent.setArtifact( factory.getSnapshotArtifact() );
        project.setParent( parent );
        helper = EnforcerTestUtils.getHelper( project );

        rule.setFailWhenParentIsSnapshot( true );
        EnforcerRuleUtilsHelper.execute( rule, helper, true );

        rule.setFailWhenParentIsSnapshot( false );
        EnforcerRuleUtilsHelper.execute( rule, helper, false );

    }

    /**
     * Test cache.
     */
    @Test
    public void testCache()
    {
        EnforcerRule rule = new RequireReleaseVersion();
        assertFalse( rule.isCacheable() );
        assertFalse( rule.isResultValid( null ) );
        assertEquals( "0", rule.getCacheId() );
    }

}
