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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Exhaustively check the enforcer mojo.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@RunWith( MockitoJUnitRunner.class )
public class TestEnforceMojo
{

    @InjectMocks
    EnforceMojo mojo;

    @Test
    public void testEnforceMojo()
        throws MojoExecutionException
    {
        setupBasics( false );

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

    @Test
    public void testCaching()
        throws MojoExecutionException
    {
        setupBasics( true );

        MockEnforcerRule[] rules = new MockEnforcerRule[10];

        // check that basic caching works.
        rules[0] = new MockEnforcerRule( false, "", true, true );
        rules[1] = new MockEnforcerRule( false, "", true, true );
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.execute();

        assertTrue( "Expected this rule to be executed.", rules[0].executed );
        assertFalse( "Expected this rule not to be executed.", rules[1].executed );

        // check that skip caching works.
        rules[0] = new MockEnforcerRule( false, "", true, true );
        rules[1] = new MockEnforcerRule( false, "", true, true );
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.ignoreCache = true;
        mojo.execute();

        assertTrue( "Expected this rule to be executed.", rules[0].executed );
        assertTrue( "Expected this rule to be executed.", rules[1].executed );

        mojo.ignoreCache = false;

        // check that different ids are compared.
        rules[0] = new MockEnforcerRule( false, "1", true, true );
        rules[1] = new MockEnforcerRule( false, "2", true, true );
        rules[2] = new MockEnforcerRule( false, "2", true, true );
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.execute();

        assertTrue( "Expected this rule to be executed.", rules[0].executed );
        assertTrue( "Expected this rule to be executed.", rules[1].executed );
        assertFalse( "Expected this rule not to be executed.", rules[2].executed );

        // check that future overrides are working
        rules[0] = new MockEnforcerRule( false, "1", true, true );
        rules[1] = new MockEnforcerRule( false, "1", false, true );
        rules[2] = null;
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.execute();

        assertTrue( "Expected this rule to be executed.", rules[0].executed );
        assertTrue( "Expected this rule to be executed.", rules[1].executed );

        // check that future isResultValid is used
        rules[0] = new MockEnforcerRule( false, "1", true, true );
        rules[1] = new MockEnforcerRule( false, "1", true, false );
        rules[2] = null;
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.execute();

        assertTrue( "Expected this rule to be executed.", rules[0].executed );
        assertTrue( "Expected this rule to be executed.", rules[1].executed );

    }

    @Test
    public void testCachePersistence1()
        throws MojoExecutionException
    {
        setupBasics( true );

        MockEnforcerRule[] rules = new MockEnforcerRule[10];

        // check that basic caching works.
        rules[0] = new MockEnforcerRule( false, "", true, true );
        rules[1] = new MockEnforcerRule( false, "", true, true );
        mojo.setRules( rules );

        EnforceMojo.cache.clear();
        mojo.execute();

        assertTrue( "Expected this rule to be executed.", rules[0].executed );
        assertFalse( "Expected this rule not to be executed.", rules[1].executed );

    }

    @Test
    public void testCachePersistence2()
        throws MojoExecutionException
    {
        setupBasics( true );

        MockEnforcerRule[] rules = new MockEnforcerRule[10];

        // check that basic caching works.
        rules[0] = new MockEnforcerRule( false, "", true, true );
        rules[1] = new MockEnforcerRule( false, "", true, true );
        mojo.setRules( rules );

        mojo.execute();

        assertFalse( "Expected this rule not to be executed.", rules[0].executed );
        assertFalse( "Expected this rule not to be executed.", rules[1].executed );

    }

    @Test
    public void testCachePersistence3()
        throws MojoExecutionException
    {
        System.gc();

        try
        {
            Thread.sleep( 1000 );
        }
        catch ( InterruptedException e )
        {
        }

        setupBasics( true );

        MockEnforcerRule[] rules = new MockEnforcerRule[10];

        // check that basic caching works.
        rules[0] = new MockEnforcerRule( false, "", true, true );
        rules[1] = new MockEnforcerRule( false, "", true, true );
        mojo.setRules( rules );

        mojo.execute();

        assertFalse( "Expected this rule not to be executed.", rules[0].executed );
        assertFalse( "Expected this rule not to be executed.", rules[1].executed );

    }

    @Test
    public void testLoggingOnEnforcerRuleExceptionWithMessage()
        throws MojoExecutionException, EnforcerRuleException
    {
        // fail=false because this is out of scope here (also allows for cleaner test code without catch block)
        setupBasics( false );

        // the regular kind of EnforcerRuleException:
        EnforcerRuleException ruleException = new EnforcerRuleException( "testMessage" );

        EnforcerRule ruleMock = Mockito.mock( EnforcerRule.class );
        Mockito.doThrow( ruleException ).when( ruleMock ).execute( Mockito.any( EnforcerRuleHelper.class ) );

        mojo.setRules( new EnforcerRule[] { ruleMock } );

        Log logSpy = setupLogSpy();

        mojo.execute();

        Mockito.verify( logSpy ).debug(
                Mockito.anyString() , Mockito.same( ruleException ) );

        Mockito.verify( logSpy, Mockito.never() ).warn(
                Mockito.anyString(), Mockito.any( Throwable.class ) );

        Mockito.verify( logSpy ).warn(
                Mockito.matches( ".* failed with message:" + System.lineSeparator() + ruleException.getMessage() ) );
    }

    @Test
    public void testLoggingOnEnforcerRuleExceptionWithoutMessage()
        throws MojoExecutionException, EnforcerRuleException
    {
        // fail=false because this is out of scope here (also allows for cleaner test code without catch block)
        setupBasics( false );

        // emulate behaviour of various rules that just catch Exception and wrap into EnforcerRuleException:
        NullPointerException npe = new NullPointerException();
        EnforcerRuleException enforcerRuleException = new EnforcerRuleException( npe.getLocalizedMessage(), npe );

        EnforcerRule ruleMock = Mockito.mock( EnforcerRule.class );
        Mockito.doThrow( enforcerRuleException ).when( ruleMock ).execute( Mockito.any( EnforcerRuleHelper.class ) );

        mojo.setRules( new EnforcerRule[] { ruleMock } );

        Log logSpy = setupLogSpy();

        mojo.execute();

        Mockito.verify( logSpy ).warn(
                Mockito.contains("failed without a message"), Mockito.same( enforcerRuleException ) );

        Mockito.verify( logSpy ).warn(
                Mockito.matches( ".* failed with message:" + System.lineSeparator() + "null" ) );
    }

    private void setupBasics( boolean fail ) {
        mojo.setFail( fail );
        mojo.setSession( EnforcerTestUtils.getMavenSession() );
        mojo.setProject( new MockProject() );
    }

    private Log setupLogSpy() {
        Log spy = Mockito.spy( mojo.getLog() );
        mojo.setLog( spy );
        return spy;
    }
}
