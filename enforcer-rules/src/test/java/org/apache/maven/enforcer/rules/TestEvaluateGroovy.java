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
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestEvaluateGroovy {

    @Mock
    private ExpressionEvaluator evaluator;

    @InjectMocks
    private EvaluateGroovy rule;

    @BeforeEach
    void setUp() throws Exception {
        rule.setLog(Mockito.mock(EnforcerLogger.class));
        // we need not testing ExpressionEvaluator
        lenient().when(evaluator.evaluate(anyString())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void conditionThatHoldsPasses() throws Exception {
        rule.setCondition("'This is a test.' == 'This is a test.'");
        rule.execute();
    }

    @Test
    void groovyIdiomsAreAvailable() throws Exception {
        rule.setCondition("'1.0-SNAPSHOT'.endsWith('-SNAPSHOT') && [1, 2, 3].every { it > 0 }");
        rule.execute();
    }

    @Test
    void conditionThatFailsUsesTheMessage() {
        rule.setCondition("'Test' == null");
        rule.setMessage("We have a variable : ${env}");
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, () -> rule.execute());
        assertEquals(rule.getMessage(), e.getMessage());
    }

    @Test
    void conditionThatFailsWithoutMessage() {
        rule.setCondition("'Test' == null");
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, () -> rule.execute());
        assertEquals("The expression \"'Test' == null\" is not true.", e.getMessage());
    }

    @Test
    void conditionThatIsNotBooleanIsRejected() {
        rule.setCondition("'a string'");
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, () -> rule.execute());
        assertTrue(e.getMessage().contains("must evaluate to a boolean"), e.getMessage());
    }

    @Test
    void conditionThatDoesNotCompileIsReported() {
        rule.setCondition("this is not groovy ((");
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, () -> rule.execute());
        assertTrue(e.getMessage().startsWith("Couldn't evaluate condition"), e.getMessage());
    }

    @Test
    void missingConditionIsAnError() {
        assertThrows(EnforcerRuleError.class, () -> rule.execute());
    }

    @Test
    void unresolvableExpressionIsReported() throws Exception {
        rule.setCondition("${env} == null");
        when(evaluator.evaluate(rule.getCondition())).thenThrow(new ExpressionEvaluationException("expected error"));
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, () -> rule.execute());
        assertEquals("Unable to evaluate an expression '${env} == null'", e.getMessage());
    }
}
