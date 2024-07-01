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
package org.apache.maven.enforcer.rules;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.ExpressionEvaluator;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestExternalRules {

    @Mock
    private MojoExecution mojoExecution;

    @Mock
    private ExpressionEvaluator evaluator;

    @Mock
    private EnforcerLogger logger;

    @InjectMocks
    private ExternalRules rule;

    @BeforeEach
    void setup() {
        rule.setLog(logger);
    }

    @Test
    void shouldFailIfNoLocationIsSet() {
        assertThatExceptionOfType(EnforcerRuleException.class)
                .isThrownBy(() -> rule.getRulesConfig())
                .withMessage("No location provided");
    }

    @Test
    void shouldFailIfClasspathLocationIsNotFound() {

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setRealm(EnforcerTestUtils.getTestClassRealm());
        when(mojoExecution.getMojoDescriptor()).thenReturn(mojoDescriptor);
        rule.setLocation("classpath:foo");

        assertThatExceptionOfType(EnforcerRuleException.class)
                .isThrownBy(() -> rule.getRulesConfig())
                .withMessage("Location 'foo' not found in classpath");
    }

    @Test
    void shouldFailIfFileLocationIsNotFound() {
        when(evaluator.alignToBaseDirectory(any())).thenAnswer(i -> i.getArgument(0));

        rule.setLocation("blah.xml");
        assertThatExceptionOfType(EnforcerRuleException.class)
                .isThrownBy(() -> rule.getRulesConfig())
                .withMessageMatching("Could not read descriptor in .*blah.xml");
    }

    @Test
    void shouldLoadRulesFromClassPath() throws EnforcerRuleException {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setRealm(EnforcerTestUtils.getTestClassRealm());
        when(mojoExecution.getMojoDescriptor()).thenReturn(mojoDescriptor);
        rule.setLocation("classpath:enforcer-rules/pass.xml");

        Xpp3Dom rulesConfig = rule.getRulesConfig();
        assertNotNull(rulesConfig);
        assertEquals(2, rulesConfig.getChildCount());
    }

    @Test
    void shouldFilterRules() throws EnforcerRuleException {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setRealm(EnforcerTestUtils.getTestClassRealm());
        when(mojoExecution.getMojoDescriptor()).thenReturn(mojoDescriptor);
        rule.setLocation("classpath:enforcer-rules/banned-dependencies.xml");
        rule.setXsltLocation("classpath:enforcer-rules/allow-findbugs.xsl");

        Xpp3Dom rulesConfig = rule.getRulesConfig();
        assertNotNull(rulesConfig);
        assertEquals(1, rulesConfig.getChildCount());
        assertEquals("bannedDependencies", rulesConfig.getChild(0).getName());
        assertEquals(1, rulesConfig.getChild(0).getChildCount());
        assertEquals("excludes", rulesConfig.getChild(0).getChild(0).getName());
        assertEquals(1, rulesConfig.getChild(0).getChild(0).getChildCount());
        assertEquals("exclude", rulesConfig.getChild(0).getChild(0).getChild(0).getName());
        assertEquals(
                "com.google.guava:listenablefuture",
                rulesConfig.getChild(0).getChild(0).getChild(0).getValue());
    }
}
