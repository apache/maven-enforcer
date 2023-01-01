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

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Class TestRequireJavaVendor.
 *
 * @author Tim Sijstermans
 */
class TestRequireJavaVendor {
    private static final String NON_MATCHING_VENDOR = "non-matching-vendor";

    private RequireJavaVendor underTest;

    @BeforeEach
    public void prepareTest() {
        underTest = new RequireJavaVendor();
    }

    @Test
    void matchingInclude() throws EnforcerRuleException {
        // Set the required vendor to the current system vendor
        underTest.setIncludes(Collections.singletonList(SystemUtils.JAVA_VENDOR));

        assertThatCode(() -> underTest.execute()).doesNotThrowAnyException();
    }

    @Test
    void nonMatchingInclude() {
        // Set the included vendor to something irrelevant
        underTest.setIncludes(Collections.singletonList(NON_MATCHING_VENDOR));

        assertThatThrownBy(() -> underTest.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessage(
                        "%s is not an included Required Java Vendor (JAVA_HOME=%s)",
                        SystemUtils.JAVA_VENDOR, SystemUtils.JAVA_HOME);
    }

    @Test
    void matchingExclude() {
        // Set the excluded vendor to current vendor name
        underTest.setExcludes(Collections.singletonList(SystemUtils.JAVA_VENDOR));

        assertThatThrownBy(() -> underTest.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessage(
                        "%s is an excluded Required Java Vendor (JAVA_HOME=%s)",
                        SystemUtils.JAVA_VENDOR, SystemUtils.JAVA_HOME);
    }

    @Test
    void nonMatchingExclude() throws EnforcerRuleException {
        // Set the excluded vendor to something nonsensical
        underTest.setExcludes(Collections.singletonList(NON_MATCHING_VENDOR));

        assertThatCode(() -> underTest.execute()).doesNotThrowAnyException();
    }

    @Test
    void matchingIncludeAndMatchingExclude() {
        underTest.setExcludes(Collections.singletonList(SystemUtils.JAVA_VENDOR));
        underTest.setIncludes(Collections.singletonList(SystemUtils.JAVA_VENDOR));

        assertThatThrownBy(() -> underTest.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessage(
                        "%s is an excluded Required Java Vendor (JAVA_HOME=%s)",
                        SystemUtils.JAVA_VENDOR, SystemUtils.JAVA_HOME);
    }

    @Test
    void matchAnyExclude() {
        // Set a bunch of excluded vendors
        underTest.setExcludes(Arrays.asList(SystemUtils.JAVA_VENDOR, SystemUtils.JAVA_VENDOR + "modified"));

        assertThatThrownBy(() -> underTest.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessage(
                        "%s is an excluded Required Java Vendor (JAVA_HOME=%s)",
                        SystemUtils.JAVA_VENDOR, SystemUtils.JAVA_HOME);
    }

    @Test
    void matchAnyInclude() throws EnforcerRuleException {
        // Set a bunch of included vendors
        underTest.setIncludes(Arrays.asList(SystemUtils.JAVA_VENDOR, SystemUtils.JAVA_VENDOR + "modified"));

        assertThatCode(() -> underTest.execute()).doesNotThrowAnyException();
    }

    @Test
    void defaultRule() throws EnforcerRuleException {

        assertThatCode(() -> underTest.execute()).doesNotThrowAnyException();
    }
}
