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
package org.apache.maven.plugins.enforcer;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugins.enforcer.utils.TestEnforcerRuleUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class TestNoSnapshots.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class TestNoSnapshots
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
        RequireReleaseDeps rule = new RequireReleaseDeps();
        rule.setSearchTransitive( false );

        TestEnforcerRuleUtils.execute( rule, helper, false );

        rule.setSearchTransitive( true );

        TestEnforcerRuleUtils.execute( rule, helper, true );

        // test onlyWhenRelease in each case
        
        project.setArtifact( factory.getSnapshotArtifact() );
        
        TestEnforcerRuleUtils.execute( rule, helper, true );
        
        rule.onlyWhenRelease = true;

        TestEnforcerRuleUtils.execute( rule, helper, false );

        project.setArtifact( factory.getReleaseArtifact() );
        
        TestEnforcerRuleUtils.execute( rule, helper, true );
    } 

    /**
     * Test id.
     */
    public void testId()
    {
        RequireReleaseDeps rule = new RequireReleaseDeps();
        rule.getCacheId();
    }
}
