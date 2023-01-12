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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for {@link RequireEnvironmentVariable}}
 *
 * @author <a href='mailto:marvin[at]marvinformatics[dot]com'>Marvin Froeder</a>
 */
class TestRequireEnvironmentVariable {

    private RequireEnvironmentVariable rule = new RequireEnvironmentVariable();
    /**
     * Test rule.
     *
     */
    @Test
    void testRule() {
        // this env variable should not be set
        rule.setVariableName("JUNK");

        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            // expected to catch this.
        }

        // PATH shall be common to windows and linux
        rule.setVariableName("PATH");
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
    void testRuleWithRegex() {

        rule.setVariableName("PATH");
        // This expression should not match the property
        // value
        rule.setRegex("[^abc]");

        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            // expected to catch this.
        }

        // can't really predict what a PATH will looks like, just enforce it ain't empty
        rule.setRegex(".{1,}");
        try {
            rule.execute();
        } catch (EnforcerRuleException e) {
            fail("This should not throw an exception");
        }
    }

    @Test
    void ruleShouldBeCached() {
        rule.setVariableName("TEST");
        Assertions.assertThat(rule.getCacheId()).isNotEmpty();
    }
}
