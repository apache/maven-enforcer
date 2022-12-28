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
package org.apache.maven.plugins.enforcer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Manage enforcer rules.
 */
@Named
@Singleton
public class EnforcerRuleManager {

    @Inject
    private Provider<MavenSession> sessionProvider;

    @Inject
    private Provider<MojoExecution> mojoExecutionProvider;

    @Inject
    @Named("basic")
    private ComponentConfigurator componentConfigurator;

    /**
     * Create enforcer rules based on xml configuration.
     *
     * @param rules a rules configuration
     * @return List of rule instances
     * @throws EnforcerRuleManagerException report a problem during rules creating
     */
    public List<EnforcerRuleDesc> createRules(PlexusConfiguration rules) throws EnforcerRuleManagerException {

        List<EnforcerRuleDesc> result = new ArrayList<>();

        if (rules == null || rules.getChildCount() == 0) {
            return result;
        }

        ClassRealm classRealm = mojoExecutionProvider
                .get()
                .getMojoDescriptor()
                .getPluginDescriptor()
                .getClassRealm();

        ExpressionEvaluator evaluator =
                new PluginParameterExpressionEvaluator(sessionProvider.get(), mojoExecutionProvider.get());

        for (PlexusConfiguration ruleConfig : rules.getChildren()) {
            EnforcerRuleDesc ruleDesc = createRuleDesc(ruleConfig.getName(), ruleConfig.getAttribute("implementation"));
            if (ruleConfig.getChildCount() > 0) {
                try {
                    componentConfigurator.configureComponent(ruleDesc.getRule(), ruleConfig, evaluator, classRealm);
                } catch (ComponentConfigurationException e) {
                    throw new EnforcerRuleManagerException(e);
                }
            }
            result.add(ruleDesc);
        }
        return result;
    }

    private EnforcerRuleDesc createRuleDesc(String name, String implementation) throws EnforcerRuleManagerException {

        String ruleClass;
        if (implementation != null && !implementation.isEmpty()) {
            ruleClass = implementation;
        } else {
            ruleClass = name;
        }

        if (!ruleClass.contains(".")) {
            ruleClass = getClass().getPackage().getName() + "." + Character.toUpperCase(ruleClass.charAt(0))
                    + ruleClass.substring(1);
        }

        try {
            return new EnforcerRuleDesc(
                    name, (EnforcerRule) Class.forName(ruleClass).newInstance());
        } catch (Exception e) {
            throw new EnforcerRuleManagerException("Failed to create enforcer rules for class: " + ruleClass, e);
        }
    }
}
