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

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * The Class TestRequireJavaVersion.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
class TestRequireJavaVersion {

    private RequireJavaVersion rule;

    @BeforeEach
    void setup() {
        rule = new RequireJavaVersion();
        rule.setLog(mock(EnforcerLogger.class));
    }
    /**
     * Test fix jdk version.
     */
    @Test
    void testFixJDKVersion() {
        // test that we only take the first 3 versions for
        // comparison
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.5.0_11")).isEqualTo("1.5.0-11");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.5.1")).isEqualTo("1.5.1");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.5.2-1.b11")).isEqualTo("1.5.2-1");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.5.3_11")).isEqualTo("1.5.3-11");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.5.4.5_11")).isEqualTo("1.5.4-5");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.5.5.6_11.2")).isEqualTo("1.5.5-6");

        // test for non-standard versions
        assertThat(RequireJavaVersion.normalizeJDKVersion("1-5-0-11")).isEqualTo("1.5.0-11");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1-_5-_0-_11")).isEqualTo("1.5.0-11");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1_5_0_11")).isEqualTo("1.5.0-11");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.5.0-07")).isEqualTo("1.5.0-7");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.5.0-b7")).isEqualTo("1.5.0-7");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.5.0-;7")).isEqualTo("1.5.0-7");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.6.0-dp")).isEqualTo("1.6.0");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.6.0-dp2")).isEqualTo("1.6.0-2");
        assertThat(RequireJavaVersion.normalizeJDKVersion("1.8.0_73")).isEqualTo("1.8.0-73");
        assertThat(RequireJavaVersion.normalizeJDKVersion("9")).isEqualTo("9");

        assertThat(RequireJavaVersion.normalizeJDKVersion("17")).isEqualTo("17");
    }

    /**
     * Test rule.
     *
     * @throws EnforcerRuleException the enforcer rule exception
     */
    @Test
    void settingsTheJavaVersionAsNormalizedVersionShouldNotFail() throws EnforcerRuleException {
        String normalizedJDKVersion = RequireJavaVersion.normalizeJDKVersion(SystemUtils.JAVA_VERSION);

        rule.setVersion(normalizedJDKVersion);

        // test the singular version
        rule.execute();
        // intentionally no assertThat(...) because we don't expect and exception.
    }

    @Test
    void excludingTheCurrentJavaVersionViaRangeThisShouldFailWithException() {
        String thisVersion = RequireJavaVersion.normalizeJDKVersion(SystemUtils.JAVA_VERSION);
        String requiredVersion = "(" + thisVersion;
        rule.setVersion(requiredVersion);

        assertThatThrownBy(() -> rule.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessage("The requested JDK version %s is invalid.", requiredVersion);
    }

    @Test
    void shouldIncludeJavaHomeLocationInTheErrorMessage() {
        String thisVersion = RequireJavaVersion.normalizeJDKVersion(SystemUtils.JAVA_VERSION);
        String requiredVersion = "10000";
        rule.setVersion(requiredVersion);

        assertThatThrownBy(() -> rule.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessage(
                        "Detected JDK version %s (JAVA_HOME=%s) is not in the allowed range %s.",
                        thisVersion, SystemUtils.JAVA_HOME, "[" + requiredVersion + ",)");
    }

    @Test
    void shouldUseCustomErrorMessage() {
        String requiredVersion = "10000";
        String message = "My custom error message";
        rule.setVersion(requiredVersion);
        rule.setMessage(message);

        assertThatThrownBy(() -> rule.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessage(message);
    }

    /**
     * Test id.
     */
    @Test
    void testId() {
        assertThat(rule.getCacheId()).isEqualTo("0");
    }

    static Stream<Arguments> fixJava8ShortVersion() {
        return Stream.of(
                Arguments.of("1.8", "1.8"),
                Arguments.of("8", "1.8"),
                Arguments.of(".8", ".8"),
                Arguments.of("8.", "8."),
                Arguments.of("8,)", "1.8,)"),
                Arguments.of("[8,)", "[1.8,)"),
                Arguments.of("(1.7,8]", "(1.7,1.8]"),
                Arguments.of("[1.8,)", "[1.8,)"),
                Arguments.of("(1.8,8]", "(1.8,1.8]"),
                Arguments.of("(8,8]", "(1.8,1.8]"),
                Arguments.of("(8,)", "(1.8,)"),
                Arguments.of("[8]", "[1.8]"),
                Arguments.of("(9,11],[8]", "(9,11],[1.8]"),
                Arguments.of("(11.0.18", "(11.0.18"),
                Arguments.of("(15.1.8", "(15.1.8"),
                Arguments.of("(15.1.2", "(15.1.2"),
                Arguments.of("18", "18"),
                Arguments.of("18.", "18."),
                Arguments.of(".18", ".18"),
                Arguments.of("38", "38"));
    }

    @ParameterizedTest
    @MethodSource
    void fixJava8ShortVersion(String input, String expected) {
        rule.setVersion(input);
        assertThat(rule.getVersion()).isEqualTo(expected);
    }
}
