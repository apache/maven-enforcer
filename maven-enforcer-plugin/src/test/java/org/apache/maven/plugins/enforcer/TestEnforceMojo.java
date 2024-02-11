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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.internal.EnforcerRuleDesc;
import org.apache.maven.plugins.enforcer.internal.EnforcerRuleManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exhaustively check the enforcer mojo.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@ExtendWith(MockitoExtension.class)
class TestEnforceMojo {

    @Mock
    private EnforcerRuleManager ruleManager;

    @Mock
    private MavenSession session;

    @InjectMocks
    private EnforceMojo mojo;

    @BeforeEach
    void setup() {
        lenient().when(session.getSystemProperties()).thenReturn(new Properties());
        lenient().when(session.getUserProperties()).thenReturn(new Properties());
    }

    @Test
    void emptyRuleListShouldThrowException() {
        mojo.setFail(false);

        assertThatCode(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessage("No rules are configured. Use the skip flag if you want to disable execution.");
    }

    @Test
    void failedRulesAndNoFailPassBuild() throws Exception {
        // set fail to false
        mojo.setFail(false);

        Log logSpy = setupLogSpy();

        // two rules which fail
        EnforcerRuleDesc[] rules = new EnforcerRuleDesc[2];
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(true));
        rules[1] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(true));

        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        mojo.execute();

        verify(logSpy)
                .warn(Mockito.contains(
                        "Rule 0: org.apache.maven.plugins.enforcer.MockEnforcerRule failed with message"));
        verify(logSpy)
                .warn(Mockito.contains(
                        "Rule 1: org.apache.maven.plugins.enforcer.MockEnforcerRule failed with message"));
    }

    @Test
    void breakBuildImmediately() throws Exception {
        // set fail to false
        mojo.setFail(false);

        Log logSpy = setupLogSpy();

        // this rule break build immediately
        EnforcerRule ruleBreakBuild = Mockito.mock(EnforcerRule.class);
        Mockito.doThrow(EnforcerRuleError.class).when(ruleBreakBuild).execute(any(EnforcerRuleHelper.class));

        EnforcerRuleDesc[] rules = new EnforcerRuleDesc[3];
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false));
        rules[1] = new EnforcerRuleDesc("ruleBreakBuild", ruleBreakBuild);
        rules[2] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false));

        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        Assertions.assertThatCode(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasCauseInstanceOf(EnforcerRuleError.class);

        assertTrue(((MockEnforcerRule) rules[0].getRule()).executed, "Expected this rule to be executed.");
        assertFalse(((MockEnforcerRule) rules[2].getRule()).executed, "Expected this rule to be not executed.");

        verify(logSpy).info(Mockito.contains("Rule 0: org.apache.maven.plugins.enforcer.MockEnforcerRule passed"));
    }

    @Test
    void testEnforceMojo() throws Exception {
        mojo.setFail(true);

        EnforcerRuleDesc[] rules = new EnforcerRuleDesc[2];
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(true));
        rules[1] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(true));

        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        try {
            mojo.setFailFast(false);
            mojo.setFail(true);
            mojo.execute();
            fail("Expected a Mojo Execution Exception.");
        } catch (MojoExecutionException e) {
            System.out.println("Caught Expected Exception:" + e.getLocalizedMessage());
        }

        try {
            mojo.setFailFast(true);
            mojo.setFail(true);
            mojo.execute();
            fail("Expected a Mojo Execution Exception.");
        } catch (MojoExecutionException e) {
            System.out.println("Caught Expected Exception:" + e.getLocalizedMessage());
        }

        ((MockEnforcerRule) rules[0].getRule()).setFailRule(false);
        ((MockEnforcerRule) rules[1].getRule()).setFailRule(false);
        mojo.execute();
    }

    @Test
    void testCaching() throws Exception {
        mojo.setFail(true);

        EnforcerRuleDesc[] rules = new EnforcerRuleDesc[10];

        // check that basic caching works.
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "", true, true));
        rules[1] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "", true, true));
        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        EnforceMojo.cache.clear();
        mojo.execute();

        assertTrue(((MockEnforcerRule) rules[0].getRule()).executed, "Expected this rule to be executed.");
        assertFalse(((MockEnforcerRule) rules[1].getRule()).executed, "Expected this rule not to be executed.");

        // check that skip caching works.
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "", true, true));
        rules[1] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "", true, true));
        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        EnforceMojo.cache.clear();
        mojo.ignoreCache = true;
        mojo.execute();

        assertTrue(((MockEnforcerRule) rules[0].getRule()).executed, "Expected this rule to be executed.");
        assertTrue(((MockEnforcerRule) rules[1].getRule()).executed, "Expected this rule to be executed.");

        mojo.ignoreCache = false;

        // check that different ids are compared.
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "1", true, true));
        rules[1] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "2", true, true));
        rules[2] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "2", true, true));
        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        EnforceMojo.cache.clear();
        mojo.execute();

        assertTrue(((MockEnforcerRule) rules[0].getRule()).executed, "Expected this rule to be executed.");
        assertTrue(((MockEnforcerRule) rules[1].getRule()).executed, "Expected this rule to be executed.");
        assertFalse(((MockEnforcerRule) rules[2].getRule()).executed, "Expected this rule not to be executed.");

        // check that future overrides are working
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "1", true, true));
        rules[1] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "1", false, true));
        rules[2] = null;
        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        EnforceMojo.cache.clear();
        mojo.execute();

        assertTrue(((MockEnforcerRule) rules[0].getRule()).executed, "Expected this rule to be executed.");
        assertTrue(((MockEnforcerRule) rules[1].getRule()).executed, "Expected this rule to be executed.");

        // check that future isResultValid is used
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "1", true, true));
        rules[1] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "1", true, false));
        rules[2] = null;
        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        EnforceMojo.cache.clear();
        mojo.execute();

        assertTrue(((MockEnforcerRule) rules[0].getRule()).executed, "Expected this rule to be executed.");
        assertTrue(((MockEnforcerRule) rules[1].getRule()).executed, "Expected this rule to be executed.");
    }

    @Test
    void testCachePersistence1() throws Exception {
        mojo.setFail(true);

        EnforcerRuleDesc[] rules = new EnforcerRuleDesc[10];

        // check that basic caching works.
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "", true, true));
        rules[1] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "", true, true));
        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        EnforceMojo.cache.clear();
        mojo.execute();

        assertTrue(((MockEnforcerRule) rules[0].getRule()).executed, "Expected this rule to be executed.");
        assertFalse(((MockEnforcerRule) rules[1].getRule()).executed, "Expected this rule not to be executed.");
    }

    @Test
    void testCachePersistence2() throws Exception {
        mojo.setFail(true);

        EnforcerRuleDesc[] rules = new EnforcerRuleDesc[10];

        // check that basic caching works.
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "", true, true));
        rules[1] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "", true, true));
        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        mojo.execute();

        assertFalse(((MockEnforcerRule) rules[0].getRule()).executed, "Expected this rule not to be executed.");
        assertFalse(((MockEnforcerRule) rules[1].getRule()).executed, "Expected this rule not to be executed.");
    }

    @Test
    void testCachePersistence3() throws Exception {
        System.gc();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        mojo.setFail(true);

        EnforcerRuleDesc[] rules = new EnforcerRuleDesc[10];

        // check that basic caching works.
        rules[0] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "", true, true));
        rules[1] = new EnforcerRuleDesc("mockEnforcerRule", new MockEnforcerRule(false, "", true, true));
        when(ruleManager.createRules(any(), any())).thenReturn(Arrays.asList(rules));

        mojo.execute();

        assertFalse(((MockEnforcerRule) rules[0].getRule()).executed, "Expected this rule not to be executed.");
        assertFalse(((MockEnforcerRule) rules[1].getRule()).executed, "Expected this rule not to be executed.");
    }

    @Test
    void testLoggingOnEnforcerRuleExceptionWithMessage() throws Exception {
        // fail=false because this is out of scope here (also allows for cleaner test code without catch block)
        mojo.setFail(false);

        // the regular kind of EnforcerRuleException:
        EnforcerRuleException ruleException = new EnforcerRuleException("testMessage");

        EnforcerRule ruleMock = Mockito.mock(EnforcerRule.class);
        when(ruleMock.getLevel()).thenReturn(EnforcerLevel.ERROR);
        Mockito.doThrow(ruleException).when(ruleMock).execute(any(EnforcerRuleHelper.class));
        when(ruleManager.createRules(any(), any()))
                .thenReturn(Collections.singletonList(new EnforcerRuleDesc("mock", ruleMock)));

        Log logSpy = setupLogSpy();

        mojo.execute();

        verify(logSpy, Mockito.never()).warn(Mockito.anyString(), any(Throwable.class));

        verify(logSpy)
                .warn(Mockito.matches(".* failed with message:" + System.lineSeparator() + ruleException.getMessage()));
    }

    @Test
    void testLoggingOnEnforcerRuleExceptionWithoutMessage() throws Exception {
        // fail=false because this is out of scope here (also allows for cleaner test code without catch block)
        mojo.setFail(false);

        // emulate behaviour of various rules that just catch Exception and wrap into EnforcerRuleException:
        NullPointerException npe = new NullPointerException();
        EnforcerRuleException enforcerRuleException = new EnforcerRuleException(npe.getLocalizedMessage(), npe);

        EnforcerRule ruleMock = Mockito.mock(EnforcerRule.class);
        when(ruleMock.getLevel()).thenReturn(EnforcerLevel.ERROR);
        Mockito.doThrow(enforcerRuleException).when(ruleMock).execute(any(EnforcerRuleHelper.class));

        when(ruleManager.createRules(any(), any()))
                .thenReturn(Collections.singletonList(new EnforcerRuleDesc("mock", ruleMock)));

        Log logSpy = setupLogSpy();

        mojo.execute();

        Mockito.verify(logSpy).warn(Mockito.matches(".* failed without a message"));
    }

    @Test
    void testFailIfNoTests() throws MojoExecutionException {
        mojo.setFail(false);
        mojo.setFailIfNoRules(false);

        Log logSpy = setupLogSpy();

        mojo.execute();

        verify(logSpy).warn("No rules are configured.");
        Mockito.verifyNoMoreInteractions(logSpy);
    }

    @Test
    void testFailIfBothRuleOverridePropertiesAreSet() throws MojoExecutionException {
        mojo.setFail(false);

        Log logSpy = setupLogSpy();
        List<String> rules = Arrays.asList("rule1", "rule2");
        mojo.setRulesToExecute(rules);

        assertThatThrownBy(() -> mojo.setCommandLineRules(rules))
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining(
                        "Detected the usage of both '-Drules' (which is deprecated) and '-Denforcer.rules'");
    }

    @Test
    void testShouldPrintWarnWhenUsingDeprecatedRulesProperty() throws MojoExecutionException {
        mojo.setFail(false);

        Log logSpy = setupLogSpy();

        mojo.setCommandLineRules(Arrays.asList("rule1", "rule2"));

        Mockito.verify(logSpy)
                .warn("Detected the usage of property '-Drules' which is deprecated. Use '-Denforcer.rules' instead.");
    }

    @Test
    void testShouldNotPrintWarnWhenDeprecatedRulesPropertyIsEmpty() throws MojoExecutionException {
        mojo.setFail(false);

        Log logSpy = setupLogSpy();

        mojo.setCommandLineRules(Collections.emptyList());

        Mockito.verifyNoInteractions(logSpy);
    }

    private Log setupLogSpy() {
        Log spy = Mockito.spy(mojo.getLog());
        mojo.setLog(spy);
        return spy;
    }
}
