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
}
