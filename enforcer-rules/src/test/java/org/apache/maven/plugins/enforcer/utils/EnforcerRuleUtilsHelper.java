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
package org.apache.maven.plugins.enforcer.utils;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The Class TestEnforcerRuleUtils.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class EnforcerRuleUtilsHelper {

    /**
     * Simpler wrapper to execute and deal with the expected result.
     *
     * @param rule the rule
     * @param helper the helper
     * @param shouldFail the should fail
     */
    public static void execute(EnforcerRule rule, EnforcerRuleHelper helper, boolean shouldFail) {
        try {
            rule.execute(helper);
            if (shouldFail) {
                fail("Exception expected.");
            }
        } catch (EnforcerRuleException e) {
            if (!shouldFail) {
                fail("No Exception expected:" + e.getMessage());
            }
            helper.getLog().debug("Rule failed as expected: " + e.getMessage());
        }
    }
}
