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

import javax.inject.Provider;

import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcerRuleManagerTest {

    @Mock
    private Provider<MavenSession> sessionProvider;

    @Mock
    private Provider<MojoExecution> mojoExecutionProvider;

    @Mock
    private ComponentConfigurator componentConfigurator;

    @InjectMocks
    private EnforcerRuleManager enforcerRuleManager;

    void setupMocks() {
        MojoExecution mojoExecution = mock(MojoExecution.class);
        when(mojoExecutionProvider.get()).thenReturn(mojoExecution);

        MojoDescriptor mojoDescriptor = mock(MojoDescriptor.class);
        when(mojoExecution.getMojoDescriptor()).thenReturn(mojoDescriptor);

        when(mojoDescriptor.getPluginDescriptor()).thenReturn(mock(PluginDescriptor.class));

        when(sessionProvider.get()).thenReturn(mock(MavenSession.class));
    }

    @Test
    void nulConfigReturnEmptyRules() throws Exception {

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(null);

        assertThat(rules).isEmpty();
    }

    @Test
    void emptyConfigReturnEmptyRules() throws Exception {

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(new DefaultPlexusConfiguration("rules"));

        assertThat(rules).isEmpty();
    }

    @Test
    void unKnownRuleThrowException() {

        setupMocks();

        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules").addChild("UnKnowRule", null);

        assertThatCode(() -> enforcerRuleManager.createRules(configuration))
                .isInstanceOf(EnforcerRuleManagerException.class)
                .hasMessage("Failed to create enforcer rules for class: org.apache.maven.plugins.enforcer.UnKnowRule")
                .hasCauseInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void invalidConfigurationThrowException() throws ComponentConfigurationException {

        setupMocks();

        PlexusConfiguration ruleConfig =
                new DefaultPlexusConfiguration("alwaysPass").addChild("message", "messageValue");
        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules");
        configuration.addChild(ruleConfig);

        doThrow(ComponentConfigurationException.class)
                .when(componentConfigurator)
                .configureComponent(any(), any(), any(), any());

        assertThatCode(() -> enforcerRuleManager.createRules(configuration))
                .isInstanceOf(EnforcerRuleManagerException.class)
                .hasCauseInstanceOf(ComponentConfigurationException.class);
    }

    @Test
    void createSimpleRule() throws EnforcerRuleManagerException {

        setupMocks();

        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules")
                .addChild("AlwaysFail", null)
                .addChild("alwaysPass", null);

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(configuration);

        assertThat(rules)
                .hasSize(2)
                .map(EnforcerRuleDesc::getRule)
                .hasExactlyElementsOfTypes(AlwaysFail.class, AlwaysPass.class);

        assertThat(rules).hasSize(2).map(EnforcerRuleDesc::getName).containsExactly("AlwaysFail", "alwaysPass");
    }

    @Test
    void createRuleWithImplementation() throws EnforcerRuleManagerException {

        setupMocks();

        PlexusConfiguration ruleConfig = new DefaultPlexusConfiguration("alwaysPassWithImp");
        ruleConfig.setAttribute("implementation", AlwaysPass.class.getName());

        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules");
        configuration.addChild(ruleConfig);

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(configuration);

        assertThat(rules).hasSize(1).map(EnforcerRuleDesc::getRule).hasExactlyElementsOfTypes(AlwaysPass.class);

        assertThat(rules).hasSize(1).map(EnforcerRuleDesc::getName).containsExactly("alwaysPassWithImp");
    }

    @Test
    void ruleShouldBeConfigured() throws EnforcerRuleManagerException, ComponentConfigurationException {

        setupMocks();

        PlexusConfiguration ruleConfig =
                new DefaultPlexusConfiguration("alwaysPass").addChild("message", "messageValue");
        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules");
        configuration.addChild(ruleConfig);

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(configuration);
        assertThat(rules).hasSize(1);

        ArgumentCaptor<EnforcerRule> ruleCaptor = ArgumentCaptor.forClass(EnforcerRule.class);
        ArgumentCaptor<PlexusConfiguration> configurationCaptor = ArgumentCaptor.forClass(PlexusConfiguration.class);

        verify(componentConfigurator)
                .configureComponent(ruleCaptor.capture(), configurationCaptor.capture(), any(), any());

        assertThat(ruleCaptor.getValue()).isInstanceOf(AlwaysPass.class);
        assertThat(configurationCaptor.getValue()).isSameAs(ruleConfig);
    }
}
