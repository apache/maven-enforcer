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
package org.apache.maven.enforcer.rules.utils;

import org.apache.maven.enforcer.rules.EnforcerTestUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The Class TestMockEnforcerExpressionEvaluator.
 */
public class TestMockEnforcerExpressionEvaluator {

    /**
     * Test evaluate.
     */
    @Test
    public void testEvaluate() {
        MavenSession session = EnforcerTestUtils.getMavenSession();

        PluginParameterExpressionEvaluator ev = new MockEnforcerExpressionEvaluator(session);
        assertMatch(ev, "SNAPSHOT");
        assertMatch(ev, "RELEASE");
        assertMatch(ev, "SNAPSHOT");
        assertMatch(ev, "LATEST");
        assertMatch(ev, "1.0");
    }

    /**
     * Assert match.
     *
     * @param ev the ev
     * @param exp the exp
     */
    public void assertMatch(PluginParameterExpressionEvaluator ev, String exp) {
        // the mock enforcer should return the name of the expression as the value.
        try {
            assertEquals(exp, ev.evaluate("${" + exp + "}"));
        } catch (ExpressionEvaluationException e) {
            fail(e.getLocalizedMessage());
        }
    }
}
