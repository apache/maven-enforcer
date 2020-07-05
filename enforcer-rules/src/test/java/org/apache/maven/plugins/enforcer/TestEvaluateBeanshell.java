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
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 * The Class TestEvaluateBeanshell.
 *
 * @author hugonnem
 */
public class TestEvaluateBeanshell
    extends TestCase
{
    private MockProject project;

    public void setUp()
    {
        project = new MockProject();
        project.setProperty( "env", "\"This is a test.\"" );
    }

    /**
     * Test rule.
     */
    public void testRulePass()
        throws EnforcerRuleException, ExpressionEvaluationException
    {
        EvaluateBeanshell rule = new EvaluateBeanshell();
        // this property should not be set
        rule.setCondition( "${env} == \"This is a test.\"" );
        rule.setMessage( "We have a variable : ${env}" );

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
        rule.execute( helper );
    }

    public void testRuleFail()
        throws EnforcerRuleException, ExpressionEvaluationException
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

    public void testRuleFailNoMessage()
        throws EnforcerRuleException, ExpressionEvaluationException
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

    public void testRuleInvalidExpression()
        throws EnforcerRuleException, ExpressionEvaluationException
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
            assertFalse( e.getLocalizedMessage().equals( rule.getMessage() ) );
        }
    }

    public void testRuleInvalidBeanshell()
        throws EnforcerRuleException, ExpressionEvaluationException
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
            assertFalse( e.getLocalizedMessage().equals( rule.getMessage() ) );
        }
    }


    public void testRuleCanExecuteMultipleThreads() throws InterruptedException {
        final String condition = "String property1 = \"${property1}\";\n" +
                "(property1.equals(\"prop0\") && \"${property2}\".equals(\"prop0\"))\n" +
                "|| (property1.equals(\"prop1\") && \"${property2}\".equals(\"prop1\"))\n" +
                "|| (property1.equals(\"prop2\") && \"${property2}\".equals(\"prop2\"))\n";

        final List<Runnable> runnables = new ArrayList<>();

        runnables.add(new Runnable() {
            @Override
            public void run() {
                final int threadNumber = 0;
                MockProject multiProject = new MockProject();
                multiProject.setProperty("property1", "prop" + threadNumber);
                multiProject.setProperty("property2", "prop" + threadNumber);

                EvaluateBeanshell rule = new EvaluateBeanshell();
                rule.setCondition(condition);
                rule.setMessage("Race condition in thread " + threadNumber);
                EnforcerRuleHelper helper = EnforcerTestUtils.getHelper(multiProject);
                try {
                    rule.execute(helper);

                } catch (EnforcerRuleException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        runnables.add(new Runnable() {
            @Override
            public void run() {
                final int threadNumber = 1;
                MockProject multiProject = new MockProject();
                multiProject.setProperty("property1", "prop" + threadNumber);
                multiProject.setProperty("property2", "prop" + threadNumber);

                EvaluateBeanshell rule = new EvaluateBeanshell();
                rule.setCondition(condition);
                rule.setMessage("Race condition in thread " + threadNumber);
                EnforcerRuleHelper helper = EnforcerTestUtils.getHelper(multiProject);
                try {
                    rule.execute(helper);

                } catch (EnforcerRuleException e) {
                    throw new RuntimeException(e);
                }

            }
        });
        runnables.add(new Runnable() {
            @Override
            public void run() {
                final int threadNumber = 2;
                MockProject multiProject = new MockProject();
                multiProject.setProperty("property1", "prop" + threadNumber);
                multiProject.setProperty("property2", "prop" + threadNumber);

                EvaluateBeanshell rule = new EvaluateBeanshell();
                rule.setCondition(condition);
                rule.setMessage("Race condition in thread " + threadNumber);
                EnforcerRuleHelper helper = EnforcerTestUtils.getHelper(multiProject);
                try {
                    rule.execute(helper);
                } catch (EnforcerRuleException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        assertConcurrent( runnables, 4);
    }

    private static void assertConcurrent(final List<? extends Runnable> runnables, final int maxTimeoutSeconds) throws InterruptedException {
        final int numThreads = runnables.size();
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
        final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
            final CountDownLatch afterInitBlocker = new CountDownLatch(1);
            final CountDownLatch allDone = new CountDownLatch(numThreads);
            for (final Runnable submittedTestRunnable : runnables) {
                threadPool.submit(new Runnable() {
                    public void run() {
                        allExecutorThreadsReady.countDown();
                        try {
                            afterInitBlocker.await();
                            submittedTestRunnable.run();
                        } catch (final Throwable e) {
                            exceptions.add(e);
                        } finally {
                            allDone.countDown();
                        }
                    }
                });
            }
            // wait until all threads are ready
            assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent", allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
            // start all test runners
            afterInitBlocker.countDown();
            assertTrue("Timeout! More than" + maxTimeoutSeconds + "seconds", allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
        } finally {
            threadPool.shutdownNow();
        }
        assertTrue("Failed with exception(s)" + exceptions, exceptions.isEmpty());
    }
}
