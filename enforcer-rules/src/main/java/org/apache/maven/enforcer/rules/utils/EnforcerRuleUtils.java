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

import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;
import java.util.Objects;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * The Class EnforcerRuleUtils.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@Named
public class EnforcerRuleUtils {

    private final ExpressionEvaluator evaluator;
    /**
     * Instantiates a new enforcer rule utils.
     *
     * @param evaluator the expression evaluator
     */
    @Inject
    public EnforcerRuleUtils(ExpressionEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator);
    }

    private void resolve(Plugin plugin) {
        try {
            plugin.setGroupId((String) evaluator.evaluate(plugin.getGroupId()));
            plugin.setArtifactId((String) evaluator.evaluate(plugin.getArtifactId()));
            plugin.setVersion((String) evaluator.evaluate(plugin.getVersion()));
        } catch (ExpressionEvaluationException e) {
            // this should have gone already before
        }
    }

    private void resolve(ReportPlugin plugin) {
        try {
            plugin.setGroupId((String) evaluator.evaluate(plugin.getGroupId()));
            plugin.setArtifactId((String) evaluator.evaluate(plugin.getArtifactId()));
            plugin.setVersion((String) evaluator.evaluate(plugin.getVersion()));
        } catch (ExpressionEvaluationException e) {
            // this should have gone already before
        }
    }

    public List<Plugin> resolvePlugins(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            resolve(plugin);
        }
        return plugins;
    }

    public List<ReportPlugin> resolveReportPlugins(List<ReportPlugin> reportPlugins) {
        for (ReportPlugin plugin : reportPlugins) {
            resolve(plugin);
        }
        return reportPlugins;
    }
}
