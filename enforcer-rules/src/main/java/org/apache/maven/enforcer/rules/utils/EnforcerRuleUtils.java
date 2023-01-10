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

import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * The Class EnforcerRuleUtils.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class EnforcerRuleUtils {
    private EnforcerRuleHelper helper;

    /**
     * Instantiates a new enforcer rule utils.
     *
     * @param helper the helper
     */
    public EnforcerRuleUtils(EnforcerRuleHelper helper) {
        this.helper = helper;
    }

    private void resolve(Plugin plugin) {
        try {
            plugin.setGroupId((String) helper.evaluate(plugin.getGroupId()));
            plugin.setArtifactId((String) helper.evaluate(plugin.getArtifactId()));
            plugin.setVersion((String) helper.evaluate(plugin.getVersion()));
        } catch (ExpressionEvaluationException e) {
            // this should have gone already before
        }
    }

    private void resolve(ReportPlugin plugin) {
        try {
            plugin.setGroupId((String) helper.evaluate(plugin.getGroupId()));
            plugin.setArtifactId((String) helper.evaluate(plugin.getArtifactId()));
            plugin.setVersion((String) helper.evaluate(plugin.getVersion()));
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
