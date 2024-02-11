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

import javax.inject.Provider;

import java.util.List;
import java.util.Properties;

import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerRuleBase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.TestRule1;
import org.apache.maven.plugins.enforcer.TestRule2;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private PlexusContainer plexusContainer;

    @Mock
    private Log mojoLog;

    private EnforcerRuleManager enforcerRuleManager;

    @BeforeEach
    void setup() {
        enforcerRuleManager =
                new EnforcerRuleManager(sessionProvider, mojoExecutionProvider, componentConfigurator, plexusContainer);
    }

    void setupMocks() {
        setupMocks(false);
    }

    void setupMocks(Boolean hasComponent) {
        MojoExecution mojoExecution = mock(MojoExecution.class);
        when(mojoExecutionProvider.get()).thenReturn(mojoExecution);

        MojoDescriptor mojoDescriptor = mock(MojoDescriptor.class);
        when(mojoExecution.getMojoDescriptor()).thenReturn(mojoDescriptor);

        when(mojoDescriptor.getPluginDescriptor()).thenReturn(mock(PluginDescriptor.class));

        MavenSession mavenSession = mock(MavenSession.class);
        when(mavenSession.getSystemProperties()).thenReturn(new Properties());
        when(mavenSession.getUserProperties()).thenReturn(new Properties());
        when(sessionProvider.get()).thenReturn(mavenSession);

        when(plexusContainer.hasComponent(any(Class.class), anyString())).thenReturn(hasComponent);
    }

    @Test
    void nullConfigReturnEmptyRules() {

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(null, mojoLog);

        assertThat(rules).isEmpty();
    }

    @Test
    void emptyConfigReturnEmptyRules() {

        List<EnforcerRuleDesc> rules =
                enforcerRuleManager.createRules(new DefaultPlexusConfiguration("rules"), mojoLog);

        assertThat(rules).isEmpty();
    }

    @Test
    void unKnownRuleThrowException() throws Exception {

        setupMocks();

        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules").addChild("UnKnowRule", null);

        assertThatCode(() -> enforcerRuleManager.createRules(configuration, mojoLog))
                .isInstanceOf(EnforcerRuleManagerException.class)
                .hasMessage(
                        "Failed to create enforcer rules with name: unKnowRule or for class: org.apache.maven.plugins.enforcer.UnKnowRule")
                .hasCauseInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void invalidConfigurationThrowException() throws Exception {

        setupMocks();

        PlexusConfiguration ruleConfig =
                new DefaultPlexusConfiguration("testRule1").addChild("message", "messageValue");
        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules");
        configuration.addChild(ruleConfig);

        doThrow(ComponentConfigurationException.class)
                .when(componentConfigurator)
                .configureComponent(any(), any(), any(), any());

        assertThatCode(() -> enforcerRuleManager.createRules(configuration, mojoLog))
                .isInstanceOf(EnforcerRuleManagerException.class)
                .hasCauseInstanceOf(ComponentConfigurationException.class);
    }

    @Test
    void createSimpleRule() throws Exception {

        setupMocks();

        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules")
                .addChild("TestRule1", null)
                .addChild("testRule2", null);

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(configuration, mojoLog);

        assertThat(rules)
                .hasSize(2)
                .map(EnforcerRuleDesc::getRule)
                .hasExactlyElementsOfTypes(TestRule1.class, TestRule2.class);

        assertThat(rules).hasSize(2).map(EnforcerRuleDesc::getName).containsExactly("testRule1", "testRule2");
    }

    @Test
    void createSimpleRuleFromComponentAndClasses() throws Exception {

        setupMocks();

        when(plexusContainer.hasComponent(any(Class.class), eq("testRule1"))).thenReturn(true);
        Mockito.doReturn(new TestRule1()).when(plexusContainer).lookup(EnforcerRuleBase.class, "testRule1");

        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules")
                .addChild("TestRule1", null)
                .addChild("testRule2", null);

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(configuration, mojoLog);

        assertThat(rules)
                .hasSize(2)
                .map(EnforcerRuleDesc::getRule)
                .hasExactlyElementsOfTypes(TestRule1.class, TestRule2.class);

        assertThat(rules).hasSize(2).map(EnforcerRuleDesc::getName).containsExactly("testRule1", "testRule2");
    }

    @Test
    void shouldThrowExceptionFormComponentCreation() throws Exception {

        setupMocks(true);

        doThrow(ComponentLookupException.class).when(plexusContainer).lookup(any(Class.class), anyString());

        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules").addChild("TestRule1", null);

        assertThatCode(() -> enforcerRuleManager.createRules(configuration, mojoLog))
                .isInstanceOf(EnforcerRuleManagerException.class)
                .hasCauseInstanceOf(ComponentLookupException.class);
    }

    @Test
    void createRuleWithImplementation() throws Exception {

        setupMocks();

        PlexusConfiguration ruleConfig = new DefaultPlexusConfiguration("testRuleWithImp");
        ruleConfig.setAttribute("implementation", TestRule1.class.getName());

        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules");
        configuration.addChild(ruleConfig);

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(configuration, mojoLog);

        assertThat(rules).hasSize(1).map(EnforcerRuleDesc::getRule).hasExactlyElementsOfTypes(TestRule1.class);

        assertThat(rules).hasSize(1).map(EnforcerRuleDesc::getName).containsExactly("testRuleWithImp");
    }

    @Test
    void ruleShouldBeConfigured() throws Exception {

        setupMocks();

        PlexusConfiguration ruleConfig =
                new DefaultPlexusConfiguration("testRule1").addChild("message", "messageValue");
        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules");
        configuration.addChild(ruleConfig);

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(configuration, mock(Log.class));
        assertThat(rules).hasSize(1);

        ArgumentCaptor<EnforcerRuleBase> ruleCaptor = ArgumentCaptor.forClass(EnforcerRuleBase.class);
        ArgumentCaptor<PlexusConfiguration> configurationCaptor = ArgumentCaptor.forClass(PlexusConfiguration.class);

        verify(componentConfigurator)
                .configureComponent(ruleCaptor.capture(), configurationCaptor.capture(), any(), any());

        assertThat(ruleCaptor.getValue()).isInstanceOf(TestRule1.class);
        assertThat(configurationCaptor.getValue()).isSameAs(ruleConfig);
    }

    @Test
    void ruleLevelShouldBeDisoveredFromConfigured() throws Exception {

        setupMocks();

        PlexusConfiguration ruleConfig = new DefaultPlexusConfiguration("testRule1").addChild("level", "WARN");
        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules");
        configuration.addChild(ruleConfig);

        List<EnforcerRuleDesc> rules = enforcerRuleManager.createRules(configuration, mock(Log.class));
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getLevel()).isEqualTo(EnforcerLevel.ERROR);
    }
}
