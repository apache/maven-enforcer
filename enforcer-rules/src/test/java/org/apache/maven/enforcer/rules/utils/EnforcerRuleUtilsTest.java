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

import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnforcerRuleUtilsTest {

    // strips "${" and "}" like MockEnforcerExpressionEvaluator
    private final EnforcerRuleUtils utils = new EnforcerRuleUtils(evaluator());

    private static ExpressionEvaluator evaluator() {
        try {
            ExpressionEvaluator evaluator = mock(ExpressionEvaluator.class);
            when(evaluator.evaluate(anyString()))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0, String.class).replaceAll("\\$\\{|}", ""));
            return evaluator;
        } catch (ExpressionEvaluationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void resolvePluginsDoesNotChangeModelPlugins() {
        Plugin plugin = new Plugin();
        plugin.setGroupId("${group}");
        plugin.setArtifactId("${artifact}");
        plugin.setVersion("${version}");

        List<Plugin> resolved = utils.resolvePlugins(Collections.singletonList(plugin));

        assertEquals("group", resolved.get(0).getGroupId());
        assertEquals("artifact", resolved.get(0).getArtifactId());
        assertEquals("version", resolved.get(0).getVersion());
        assertEquals("${group}", plugin.getGroupId());
        assertEquals("${artifact}", plugin.getArtifactId());
        assertEquals("${version}", plugin.getVersion());
    }

    @Test
    void resolveReportPluginsDoesNotChangeModelPlugins() {
        ReportPlugin plugin = new ReportPlugin();
        plugin.setGroupId("${group}");
        plugin.setArtifactId("${artifact}");
        plugin.setVersion("${version}");

        List<ReportPlugin> resolved = utils.resolveReportPlugins(Collections.singletonList(plugin));

        assertEquals("group", resolved.get(0).getGroupId());
        assertEquals("artifact", resolved.get(0).getArtifactId());
        assertEquals("version", resolved.get(0).getVersion());
        assertEquals("${group}", plugin.getGroupId());
        assertEquals("${artifact}", plugin.getArtifactId());
        assertEquals("${version}", plugin.getVersion());
    }
}
