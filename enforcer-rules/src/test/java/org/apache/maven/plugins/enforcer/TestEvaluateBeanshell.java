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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The Class TestEvaluateBeanshell.
 *
 * @author hugonnem
 */
public class TestEvaluateBeanshell
{
    private MockProject project;

    @BeforeEach
    public void setUp()
    {
        project = new MockProject();
        project.setProperty( "env", "\"This is a test.\"" );
    }

    /**
     * Test rule.
     */
    @Test
    public void testRulePass()
        throws Exception
    {
        EvaluateBeanshell rule = new EvaluateBeanshell();
        // this property should not be set
        rule.setCondition( "${env} == \"This is a test.\"" );
        rule.setMessage( "We have a variable : ${env}" );

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
        rule.execute( helper );
    }

    @Test
    public void testRuleFail()
    {
        EvaluateBeanshell rule = new EvaluateBeanshell();
        // this property should be set by the surefire
        // plugin
        rule.setCondition( "${env} == null" );
        rule.setMessage( "We have a variable : ${env}" );

        try
        {
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            assertEquals( e.getLocalizedMessage(), rule.getMessage() );
        }
    }

    @Test
    public void testRuleFailNoMessage()
    {
        EvaluateBeanshell rule = new EvaluateBeanshell();
        // this property should be set by the surefire
        // plugin
        rule.setCondition( "${env} == null" );
        try
        {
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            assertEquals( e.getLocalizedMessage(), "The expression \"${env} == null\" is not true." );
            assertTrue( e.getLocalizedMessage().length() > 0 );
        }
    }

    @Test
    public void testRuleInvalidExpression()
        throws Exception
    {
        EvaluateBeanshell rule = new EvaluateBeanshell();
        rule.setCondition( "${env} == null" );
        rule.setMessage( "We have a variable : ${env}" );

        ExpressionEvaluator eval = mock( ExpressionEvaluator.class );
        when( eval.evaluate( rule.getCondition() ) ).thenThrow( new ExpressionEvaluationException( "expected error" ) );
        try
        {
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project, eval );
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            assertNotEquals( e.getLocalizedMessage(), rule.getMessage() );
        }
    }

    @Test
    public void testRuleInvalidBeanshell()
    {
        EvaluateBeanshell rule = new EvaluateBeanshell();
        rule.setCondition( "this is not valid beanshell" );
        rule.setMessage( "We have a variable : ${env}" );
        try
        {
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            assertNotEquals( e.getLocalizedMessage(), rule.getMessage() );
        }
    }

    @Test
    public void testRuleCanExecuteMultipleThreads()
        throws Exception
    {
        final String condition = "String property1 = \"${property1}\";\n"
            + "(property1.equals(\"prop0\") && \"${property2}\".equals(\"prop0\"))\n"
            + "|| (property1.equals(\"prop1\") && \"${property2}\".equals(\"prop1\"))\n"
            + "|| (property1.equals(\"prop2\") && \"${property2}\".equals(\"prop2\"))\n";

        final List<Runnable> runnables = new ArrayList<>();

        runnables.add( () -> {
            final int threadNumber = 0;
            MockProject multiProject = new MockProject();
            multiProject.setProperty( "property1", "prop" + threadNumber );
            multiProject.setProperty( "property2", "prop" + threadNumber );

            EvaluateBeanshell rule = new EvaluateBeanshell();
            rule.setCondition( condition );
            rule.setMessage( "Race condition in thread " + threadNumber );
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( multiProject );
            try
            {
                rule.execute( helper );

            }
            catch ( EnforcerRuleException e )
            {
                throw new RuntimeException( e );
            }
        } );
        runnables.add( () -> {
            final int threadNumber = 1;
            MockProject multiProject = new MockProject();
            multiProject.setProperty( "property1", "prop" + threadNumber );
            multiProject.setProperty( "property2", "prop" + threadNumber );

            EvaluateBeanshell rule = new EvaluateBeanshell();
            rule.setCondition( condition );
            rule.setMessage( "Race condition in thread " + threadNumber );
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( multiProject );
            try
            {
                rule.execute( helper );

            }
            catch ( EnforcerRuleException e )
            {
                throw new RuntimeException( e );
            }

        } );
        runnables.add( () -> {
            final int threadNumber = 2;
            MockProject multiProject = new MockProject();
            multiProject.setProperty( "property1", "prop" + threadNumber );
            multiProject.setProperty( "property2", "prop" + threadNumber );

            EvaluateBeanshell rule = new EvaluateBeanshell();
            rule.setCondition( condition );
            rule.setMessage( "Race condition in thread " + threadNumber );
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( multiProject );
            try
            {
                rule.execute( helper );
            }
            catch ( EnforcerRuleException e )
            {
                throw new RuntimeException( e );
            }
        } );

        assertConcurrent( runnables, 4 );
    }

    private static void assertConcurrent( final List<? extends Runnable> runnables, final int maxTimeoutSeconds )
        throws InterruptedException
    {
        final int numThreads = runnables.size();
        final List<Throwable> exceptions = Collections.synchronizedList( new ArrayList<>() );
        final ExecutorService threadPool = Executors.newFixedThreadPool( numThreads );
        try
        {
            final CountDownLatch allExecutorThreadsReady = new CountDownLatch( numThreads );
            final CountDownLatch afterInitBlocker = new CountDownLatch( 1 );
            final CountDownLatch allDone = new CountDownLatch( numThreads );
            for ( final Runnable submittedTestRunnable : runnables )
            {
                threadPool.submit( () -> {
                    allExecutorThreadsReady.countDown();
                    try
                    {
                        afterInitBlocker.await();
                        submittedTestRunnable.run();
                    }
                    catch ( final Throwable e )
                    {
                        exceptions.add( e );
                    }
                    finally
                    {
                        allDone.countDown();
                    }
                } );
            }
            // wait until all threads are ready
            assertTrue( allExecutorThreadsReady.await( runnables.size() * 10, TimeUnit.MILLISECONDS ),
                        "Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent" );
            // start all test runners
            afterInitBlocker.countDown();
            assertTrue( allDone.await( maxTimeoutSeconds, TimeUnit.SECONDS ),
                        "Timeout! More than" + maxTimeoutSeconds + "seconds" );
        }
        finally
        {
            threadPool.shutdownNow();
        }
        assertTrue( exceptions.isEmpty(), "Failed with exception(s)" + exceptions );
    }
}
