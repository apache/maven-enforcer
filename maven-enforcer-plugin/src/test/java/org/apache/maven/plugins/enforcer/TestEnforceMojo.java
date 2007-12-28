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

import junit.framework.TestCase;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Exhaustively check the enforcer mojo.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestEnforceMojo
    extends TestCase
{

    public void testEnforceMojo ()
        throws MojoExecutionException
    {
        EnforceMojo mojo = new EnforceMojo();
        mojo.setFail( false );
        mojo.setSession( EnforcerTestUtils.getMavenSession() );
        mojo.setProject( new MockProject() );

        try
        {
            mojo.execute();
            fail( "Expected a Mojo Execution Exception." );
        }
        catch ( MojoExecutionException e )
        {
            System.out.println( "Caught Expected Exception:" + e.getLocalizedMessage() );
        }

        EnforcerRule[] rules = new EnforcerRule[10];
        rules[0] = new MockEnforcerRule( true );
        rules[1] = new MockEnforcerRule( true );
        mojo.setRules( rules );

        mojo.execute();

        try
        {
            mojo.setFailFast( false );
            mojo.setFail( true );
            mojo.execute();
            fail( "Expected a Mojo Execution Exception." );
        }
        catch ( MojoExecutionException e )
        {
            System.out.println( "Caught Expected Exception:" + e.getLocalizedMessage() );
        }

        try
        {
            mojo.setFailFast( true );
            mojo.setFail( true );
            mojo.execute();
            fail( "Expected a Mojo Execution Exception." );
        }
        catch ( MojoExecutionException e )
        {
            System.out.println( "Caught Expected Exception:" + e.getLocalizedMessage() );
        }

        ( (MockEnforcerRule) rules[0] ).setFailRule( false );
        ( (MockEnforcerRule) rules[1] ).setFailRule( false );
        mojo.execute();

    }

    public void testCaching () throws MojoExecutionException
    {
        EnforceMojo mojo = new EnforceMojo();
        mojo.setFail( true );
        mojo.setSession( EnforcerTestUtils.getMavenSession() );
        mojo.setProject( new MockProject() );

        MockEnforcerRule[] rules = new MockEnforcerRule[10];
        
        //check that basic caching works.
        rules[0] = new MockEnforcerRule( false, "", true, true );
        rules[1] = new MockEnforcerRule( false, "", true, true );
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.execute();
        
        assertTrue( "Expected this rule to be executed.",rules[0].executed );
        assertFalse( "Expected this rule not to be executed.",rules[1].executed);
        
        //check that skip caching works.
        rules[0] = new MockEnforcerRule( false, "", true, true );
        rules[1] = new MockEnforcerRule( false, "", true, true );
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.ignoreCache = true;
        mojo.execute();
        
        assertTrue( "Expected this rule to be executed.",rules[0].executed );
        assertTrue( "Expected this rule to be executed.",rules[1].executed );
        
        mojo.ignoreCache = false;
        
        //check that different ids are compared.
        rules[0] = new MockEnforcerRule( false, "1", true, true );
        rules[1] = new MockEnforcerRule( false, "2", true, true );
        rules[2] = new MockEnforcerRule( false, "2", true, true );
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.execute();
        
        assertTrue( "Expected this rule to be executed.",rules[0].executed );
        assertTrue( "Expected this rule to be executed.",rules[1].executed);
        assertFalse( "Expected this rule not to be executed.",rules[2].executed);
        
        //check that future overrides are working
        rules[0] = new MockEnforcerRule( false, "1", true, true );
        rules[1] = new MockEnforcerRule( false, "1", false, true );
        rules[2] = null;
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.execute();
        
        assertTrue( "Expected this rule to be executed.",rules[0].executed );
        assertTrue( "Expected this rule to be executed.",rules[1].executed);

        //check that future isResultValid is used
        rules[0] = new MockEnforcerRule( false, "1", true, true );
        rules[1] = new MockEnforcerRule( false, "1", true, false );
        rules[2] = null;
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.execute();
        
        assertTrue( "Expected this rule to be executed.",rules[0].executed );
        assertTrue( "Expected this rule to be executed.",rules[1].executed);

    }
    
    public void testCachePersistence1() throws MojoExecutionException
    {
        EnforceMojo mojo = new EnforceMojo();
        mojo.setFail( true );
        mojo.setSession( EnforcerTestUtils.getMavenSession() );
        mojo.setProject( new MockProject() );

        MockEnforcerRule[] rules = new MockEnforcerRule[10];
        
        //check that basic caching works.
        rules[0] = new MockEnforcerRule( false, "", true, true );
        rules[1] = new MockEnforcerRule( false, "", true, true );
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.execute();
        
        assertTrue( "Expected this rule to be executed.",rules[0].executed );
        assertFalse( "Expected this rule not to be executed.",rules[1].executed);
        
    }
    
    public void testCachePersistence2() throws MojoExecutionException
    {
        EnforceMojo mojo = new EnforceMojo();
        mojo.setFail( true );
        mojo.setSession( EnforcerTestUtils.getMavenSession() );
        mojo.setProject( new MockProject() );

        MockEnforcerRule[] rules = new MockEnforcerRule[10];
        
        //check that basic caching works.
        rules[0] = new MockEnforcerRule( false, "", true, true );
        rules[1] = new MockEnforcerRule( false, "", true, true );
        mojo.setRules( rules );

        mojo.execute();
        
        assertFalse( "Expected this rule not to be executed.",rules[0].executed);
        assertFalse( "Expected this rule not to be executed.",rules[1].executed);
        
    }
    
    public void testCachePersistence3() throws MojoExecutionException
    {
        System.gc();
        
        try
        {
            Thread.sleep( 1000 );
        }
        catch ( InterruptedException e )
        {
        }
        
        EnforceMojo mojo = new EnforceMojo();
        mojo.setFail( true );
        mojo.setSession( EnforcerTestUtils.getMavenSession() );
        mojo.setProject( new MockProject() );

        MockEnforcerRule[] rules = new MockEnforcerRule[10];
        
        //check that basic caching works.
        rules[0] = new MockEnforcerRule( false, "", true, true );
        rules[1] = new MockEnforcerRule( false, "", true, true );
        mojo.setRules( rules );

        mojo.execute();
        
        assertFalse( "Expected this rule not to be executed.",rules[0].executed);
        assertFalse( "Expected this rule not to be executed.",rules[1].executed);
        
    }
}
