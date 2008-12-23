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

import junit.framework.TestCase;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.easymock.MockControl;

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
        rule.condition = "${env} == \"This is a test.\"";
        rule.message = "We have a variable : ${env}";

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
        rule.execute( helper );
    }

    public void testRuleFail()
        throws EnforcerRuleException, ExpressionEvaluationException
    {
        EvaluateBeanshell rule = new EvaluateBeanshell();
        // this property should be set by the surefire
        // plugin
        rule.condition = "${env} == null";
        rule.message = "We have a variable : ${env}";

        try
        {
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            assertEquals( e.getLocalizedMessage(), rule.message );
        }
    }

    public void testRuleFailNoMessage()
        throws EnforcerRuleException, ExpressionEvaluationException
    {
        EvaluateBeanshell rule = new EvaluateBeanshell();
        // this property should be set by the surefire
        // plugin
        rule.condition = "${env} == null";
        try
        {
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            assertEquals( e.getLocalizedMessage(), rule.message );
            assertTrue( e.getLocalizedMessage().length() > 0 );
        }
    }

    public void testRuleInvalidExpression()
        throws EnforcerRuleException, ExpressionEvaluationException
    {
        EvaluateBeanshell rule = new EvaluateBeanshell();
        rule.condition = "${env} == null";
        rule.message = "We have a variable : ${env}";
        MockControl evalControl = MockControl.createControl( ExpressionEvaluator.class );
        try
        {
            ExpressionEvaluator eval = (ExpressionEvaluator) evalControl.getMock();
            eval.evaluate( rule.condition );
            evalControl.expectAndDefaultThrow( null, new ExpressionEvaluationException( "expected error" ) );
            evalControl.replay();

            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project, eval );
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            assertFalse( e.getLocalizedMessage().equals( rule.message ) );
        }

        evalControl.verify();
    }

    public void testRuleInvalidBeanshell()
        throws EnforcerRuleException, ExpressionEvaluationException
    {
        EvaluateBeanshell rule = new EvaluateBeanshell();
        rule.condition = "this is not valid beanshell";
        rule.message = "We have a variable : ${env}";
        try
        {
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            assertFalse( e.getLocalizedMessage().equals( rule.message ) );
        }
    }
}
