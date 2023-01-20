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
package org.apache.maven.plugins.enforcer.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleBase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Manage enforcer rules.
 *
 * @author Slawomir Jaranowski
 * @since 3.2.0
 */
@Named
@Singleton
public class EnforcerRuleManager {

    private final Provider<MavenSession> sessionProvider;

    private final Provider<MojoExecution> mojoExecutionProvider;

    private final ComponentConfigurator componentConfigurator;

    private final PlexusContainer plexusContainer;

    @Inject
    public EnforcerRuleManager(
            Provider<MavenSession> sessionProvider,
            Provider<MojoExecution> mojoExecutionProvider,
            @Named("basic") ComponentConfigurator componentConfigurator,
            PlexusContainer plexusContainer) {
        this.sessionProvider = Objects.requireNonNull(sessionProvider, "sessionProvider must be not null");
        this.mojoExecutionProvider =
                Objects.requireNonNull(mojoExecutionProvider, "mojoExecutionProvider must be not null");
        this.componentConfigurator =
                Objects.requireNonNull(componentConfigurator, "componentConfigurator must be not null");
        this.plexusContainer = Objects.requireNonNull(plexusContainer, "plexusContainer must be not null");
    }

    /**
     * Create enforcer rules based on xml configuration.
     *
     * @param rules a rules configuration
     * @param log   a Mojo logger
     * @return List of rule instances
     * @throws EnforcerRuleManagerException report a problem during rules creating
     */
    public List<EnforcerRuleDesc> createRules(PlexusConfiguration rules, Log log) throws EnforcerRuleManagerException {

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

        EnforcerLogger enforcerLoggerError = new EnforcerLoggerError(log);
        EnforcerLogger enforcerLoggerWarn = new EnforcerLoggerWarn(log);

        for (PlexusConfiguration ruleConfig : rules.getChildren()) {
            // we need rule level before configuration in order to proper set logger
            EnforcerLevel ruleLevel = getRuleLevelFromConfig(ruleConfig);

            EnforcerRuleDesc ruleDesc = createRuleDesc(ruleConfig.getName(), ruleConfig.getAttribute("implementation"));
            // setup logger before rule configuration
            ruleDesc.getRule().setLog(ruleLevel == EnforcerLevel.ERROR ? enforcerLoggerError : enforcerLoggerWarn);
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

    private EnforcerLevel getRuleLevelFromConfig(PlexusConfiguration ruleConfig) {
        PlexusConfiguration levelConfig = ruleConfig.getChild("level", false);
        String level = Optional.ofNullable(levelConfig)
                .map(PlexusConfiguration::getValue)
                .orElse(EnforcerLevel.ERROR.name());
        return EnforcerLevel.valueOf(level);
    }

    private EnforcerRuleDesc createRuleDesc(String name, String implementation) throws EnforcerRuleManagerException {

        // component name should always start at lowercase character
        String ruleName = Character.toLowerCase(name.charAt(0)) + name.substring(1);

        if (plexusContainer.hasComponent(EnforcerRuleBase.class, ruleName)) {
            try {
                return new EnforcerRuleDesc(ruleName, plexusContainer.lookup(EnforcerRuleBase.class, ruleName));
            } catch (ComponentLookupException e) {
                throw new EnforcerRuleManagerException(e);
            }
        }

        String ruleClass;
        if (implementation != null && !implementation.isEmpty()) {
            ruleClass = implementation;
        } else {
            ruleClass = name;
        }

        if (!ruleClass.contains(".")) {
            ruleClass = "org.apache.maven.plugins.enforcer." + Character.toUpperCase(ruleClass.charAt(0))
                    + ruleClass.substring(1);
        }

        try {
            return new EnforcerRuleDesc(
                    ruleName, (EnforcerRuleBase) Class.forName(ruleClass).newInstance());
        } catch (Exception e) {
            throw new EnforcerRuleManagerException(
                    "Failed to create enforcer rules with name: " + ruleName + " or for class: " + ruleClass, e);
        }
    }
}
