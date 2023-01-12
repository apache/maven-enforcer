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
package org.apache.maven.enforcer.rules.version;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * The Class TestMavenVersion.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@ExtendWith(MockitoExtension.class)
class TestMavenVersion {

    @Mock
    private RuntimeInformation runtimeInformation;

    @Mock
    private EnforcerLogger log;

    @InjectMocks
    private RequireMavenVersion rule;

    @BeforeEach
    void setup() {
        rule.setLog(log);
    }

    /**
     * Test rule.
     *
     * @throws EnforcerRuleException the enforcer rule exception
     */
    @Test
    void testRule() throws EnforcerRuleException {

        when(runtimeInformation.getMavenVersion()).thenReturn("3.0");

        rule.setVersion("2.0.5");

        // test the singular version
        rule.execute();

        // exclude this version
        rule.setVersion("(2.0.5");

        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            // expected to catch this.
        }

        // this shouldn't crash
        rule.setVersion("2.0.5_01");
        rule.execute();
    }

    /**
     * Test few more cases
     *
     * @throws EnforcerRuleException the enforcer rule exception
     */
    @Test
    void checkRequireVersionMatrix() throws EnforcerRuleException {

        when(runtimeInformation.getMavenVersion()).thenReturn("3.6.1");
        rule.setVersion("3.6.0");
        rule.execute();

        when(runtimeInformation.getMavenVersion()).thenReturn("3.6.2");
        rule.setVersion("3.6.0");
        rule.execute();
        rule.setVersion("3.6.1");
        rule.execute();
        rule.setVersion("3.6.2");
        rule.execute();
        rule.setVersion("3.6.3");
        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            // expected to catch this.
        }
    }

    /**
     * Test id.
     */
    @Test
    void testId() {
        rule.setVersion("3.3.3");
        assertThat(rule.getCacheId()).isNotEmpty();
    }
}
