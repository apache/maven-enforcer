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

import java.util.stream.Stream;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    @ParameterizedTest(name = "{0} should be in version range \"{1}\", because {2}")
    @MethodSource("provideIsInVersionsrange")
    @DisplayName("The provided version")
    void shouldBeInVersionsRange(String runtimeVersion, String rulesVersionRange) throws EnforcerRuleException {
        when(runtimeInformation.getMavenVersion()).thenReturn(runtimeVersion);
        rule.setVersion(rulesVersionRange);
        rule.execute();
    }


    private static Stream<Arguments> provideIsInVersionsrange() {
        // Based on from https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
        // in combination with the there linked https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification
        return Stream.of(
          Arguments.of("3.9.12", "3.9.11", "3.9.12 >= 3.9.11 (\"Minimum in enforcer\")"),
          Arguments.of("4.0.0-rc-5", "3.6.3", "4.0.0-rc-5 >= 3.9.12 (\"Minimum in enforcer\")"),
          Arguments.of("3.9.12", "(,3.9.12]", "3.9.12 <= 3.9.12"),
          Arguments.of("3.9.12", "(,3.9.13]", "3.9.12 < 3.9.13"),
          Arguments.of("3.9.12", "[3.9.12]", "3.9.12 == 3.9.12"),
          Arguments.of("3.9.12", "[3.9.12,)", "3.9.12 >= 3.9.12"),
          Arguments.of("3.9.12", "[3.9.11,)", "3.9.12 >= 3.9.11"),
          Arguments.of("3.9.12", "(3.9.11,)", "3.9.12 > 3.9.11"),
          Arguments.of("3.9.12", "(3.9.12-alpha-1,)", "3.9.12 > 3.9.12-alpha-1"),
          Arguments.of("3.9.12", "(3.6.3,3.9.13)", "3.6.3 < 3.9.12 < 3.9.13"),
          Arguments.of("3.9.12", "(3.6.3,3.9.12]", "3.6.3 < 3.9.12 <= 3.9.12"),
          Arguments.of("3.9.12", "(,3.9.11],[3.9.12,)", "3.9.12 <= 3.9.11 (false) OR 3.9.12 >= 3.9.12 (true)"),
          Arguments.of("3.9.12", "(,3.9.12],[3.9.11,)", "3.9.12 <= 3.9.12 (true) OR 3.9.11 >= 3.9.12 (false)"),
          Arguments.of("3.9.12", "(,3.9.11],(,3.9.10,[3.9.12,)", "3.9.12 <= 3.9.11 (false) OR 3.9.12 <= 3.9.10 (false) OR  3.9.12 >= 3.9.12 (true)"),
          Arguments.of("3.9.11", "(,3.9.12),(3.9.12,)", "3.9.12 != 3.9.12"),
          Arguments.of("4.0.0-rc-5", "(3.6.3,4.0.0)", "3.6.3 < 4.0.0-rc-5 < 4.0.0")
          );
    }

    @ParameterizedTest(name = "{0} should NOT be in version range \"{1}\", because {2}")
    @MethodSource("provideIsNotInVersionsrange")
    @DisplayName("The provided version")
    void shouldNotBeInVersionsRange(String runtimeVersion, String rulesVersionRange) {
        when(runtimeInformation.getMavenVersion()).thenReturn(runtimeVersion);
        rule.setVersion(rulesVersionRange);
        try {
            rule.execute();
            fail("Expected an exception.");
        } catch (EnforcerRuleException e) {
            // expected that this fails
        }
    }


    private static Stream<Arguments> provideIsNotInVersionsrange() {
        // Based on from https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
        // in combination with the there linked https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification
        return Stream.of(
          Arguments.of("3.9.11", "3.9.12", "3.9.11 is not >= 3.9.12 (\"Minimum in enforcer\")"),
          Arguments.of("3.9.13", "(,3.9.12]", "3.9.13 is not <= 3.9.12"),
          Arguments.of("3.9.13", "(,3.9.13]", "3.9.13 is not < 3.9.13"),
          Arguments.of("3.9.13", "[3.9.12]", "3.9.13 is not == 3.9.12"),
          Arguments.of("3.9.11", "[3.9.12,)", "3.9.11 is not >= 3.9.12"),
          Arguments.of("3.9.11", "[3.9.12,)", "3.9.11 is not >= 3.9.12"),
          Arguments.of("3.9.11", "(3.9.11,)", "3.9.11 is not > 3.9.11"),
          Arguments.of("3.9.12-alpha-1", "(3.9.12-alpha-2,)", "3.9.12-alpha 1 is not > 3.9.12-alpha-2"),
          Arguments.of("3.9.11", "(3.6.12,3.9.13)", "3.6.12 is not < 3.9.11 but 3.9.11 is < 3.9.13"),
          Arguments.of("3.9.13", "(3.6.11,3.9.12)", "3.6.12 is < 3.9.13 but 3.9.13 is not < 3.9.12"),
          Arguments.of("3.9.12", "(3.6.3,3.9.11]", "3.6.3 is < 3.9.12 but 3.9.12 is not <= 3.9.11"),
          Arguments.of("3.9.12", "(,3.9.11],[3.9.13,)", "3.9.12 is not <= 3.9.11 and 3.9.12 is not >= 3.9.13"),
          Arguments.of("3.9.12", "(,3.9.11],[3.9.12,)", "3.9.12 is not <= 3.9.11 but 3.9.12 is >= 3.9.13"),
          Arguments.of("3.9.12", "(,3.9.12],[3.9.13,)", "3.9.12 is <= 3.9.12 and 3.9.12 is not >= 3.9.13"),
          Arguments.of("3.9.12", "(,3.9.12],(,3.9.12,[3.9.13,)", "3.9.12 is <= 3.9.11 and 3.9.12 is <= 3.9.10 but  3.9.12 is not >= 3.9.13"),
          Arguments.of("3.9.12", "(,3.9.12),(3.9.12,)", "3.9.11 is not != 3.9.12"),
          Arguments.of("4.0.0-rc-5", "(3.6.12,3.99.99)", "3.6.12 is < 4.0.0-rc-5 but 4.0.0-rc-5 is not < 3.99.99")
          );
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
