/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.enforcer.rules;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.ExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The Class TestEvaluateBeanshell.
 *
 * @author hugonnem
 */
@ExtendWith(MockitoExtension.class)
class TestEvaluateBeanshell {

    @Mock
    private ExpressionEvaluator evaluator;

    @InjectMocks
    private EvaluateBeanshell rule;

    @BeforeEach
    public void setUp() throws Exception {
        rule.setLog(Mockito.mock(EnforcerLogger.class));

        // we need not testing ExpressionEvaluator
        when(evaluator.evaluate(anyString())).thenAnswer(i -> i.getArgument(0));
    }

    /**
     * Test rule.
     */
    @Test
    void testRulePass() throws Exception {

        rule.setCondition("\"This is a test.\" == \"This is a test.\"");
        rule.execute();
    }

    @Test
    void testRuleFail() {
        rule.setCondition("\"Test\" == null");
        rule.setMessage("We have a variable : ${env}");

        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            assertEquals(e.getMessage(), rule.getMessage());
        }
    }

    @Test
    void testRuleFailNoMessage() {
        rule.setCondition("\"Test\" == null");
        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            assertEquals("The expression \"\"Test\" == null\" is not true.", e.getLocalizedMessage());
            assertTrue(e.getLocalizedMessage().length() > 0);
        }
    }

    @Test
    void testRuleInvalidExpression() throws Exception {
        rule.setCondition("${env} == null");
        rule.setMessage("We have a variable : ${env}");

        when(evaluator.evaluate(rule.getCondition())).thenThrow(new ExpressionEvaluationException("expected error"));
        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            assertNotEquals(e.getLocalizedMessage(), rule.getMessage());
        }
    }

    @Test
    void testRuleInvalidBeanshell() {
        rule.setCondition("this is not valid beanshell");
        rule.setMessage("We have a variable : ${env}");
        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            assertNotEquals(e.getLocalizedMessage(), rule.getMessage());
        }
    }
}
