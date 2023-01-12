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
package org.apache.maven.enforcer.rules.property;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.ExpressionEvaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * The Class TestRequireProperty.
 *
 * @author Paul Gier
 */
@ExtendWith(MockitoExtension.class)
class TestRequireProperty {

    @Mock
    private ExpressionEvaluator evaluator;

    @InjectMocks
    private RequireProperty rule;

    /**
     * Test rule.
     *
     */
    @Test
    void testRule() throws Exception {

        // this property should not be set
        rule.setProperty("testPropJunk");

        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            // expected to catch this.
        }

        when(evaluator.evaluate("${testProp}")).thenReturn("This is a test.");

        // this property should be set by the surefire
        // plugin
        rule.setProperty("testProp");
        try {
            rule.execute();
        } catch (EnforcerRuleException e) {
            fail("This should not throw an exception");
        }
    }

    /**
     * Test rule with regex.
     *
     */
    @Test
    void testRuleWithRegex() throws Exception {

        when(evaluator.evaluate("${testProp}")).thenReturn("This is a test.");

        rule.setProperty("testProp");
        // This expression should not match the property
        // value
        rule.setRegex("[^abc]");

        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            // expected to catch this.
        }

        // this expr should match the property
        rule.setRegex("[This].*[.]");
        try {
            rule.execute();
        } catch (EnforcerRuleException e) {
            fail("This should not throw an exception");
        }
    }

    /**
     * Test id.
     */
    @Test
    void ruleShouldNotBeCached() {
        assertThat(rule.getCacheId()).isNull();
    }
}
