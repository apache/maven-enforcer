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

import java.util.Iterator;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Exhaustively check the OS mojo.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
class TestRequireOS {

    /**
     * Test os.
     */
    @Test
    public void testOS() {
        Log log = new SystemStreamLog();

        RequireOS rule = new RequireOS(new OperatingSystemProfileActivator());

        Iterator<String> iter = Os.getValidFamilies().iterator();
        String validFamily;
        String invalidFamily = null;
        while (iter.hasNext()) {
            String fam = iter.next();
            if (!Os.isFamily(fam)) {
                invalidFamily = fam;
                break;
            }
        }

        validFamily = Os.OS_FAMILY;

        log.info("Testing Mojo Using Valid Family: " + validFamily + " Invalid Family: " + invalidFamily);

        rule.setFamily(validFamily);
        assertThat(rule.isAllowed()).isTrue();

        rule.setFamily(invalidFamily);
        assertThat(rule.isAllowed()).isFalse();

        rule.setFamily("!" + invalidFamily);
        assertThat(rule.isAllowed()).isTrue();

        rule.setFamily(null);
        rule.setArch(Os.OS_ARCH);
        assertThat(rule.isAllowed()).isTrue();

        rule.setArch("somecrazyarch");
        assertThat(rule.isAllowed()).isFalse();

        rule.setArch("!somecrazyarch");
        assertThat(rule.isAllowed()).isTrue();

        rule.setArch(null);

        rule.setName(Os.OS_NAME);
        assertThat(rule.isAllowed()).isTrue();

        rule.setName("somecrazyname");
        assertThat(rule.isAllowed()).isFalse();

        rule.setName("!somecrazyname");
        assertThat(rule.isAllowed()).isTrue();

        rule.setName(null);

        rule.setVersion(Os.OS_VERSION);
        assertThat(rule.isAllowed()).isTrue();

        rule.setVersion("somecrazyversion");
        assertThat(rule.isAllowed()).isFalse();

        rule.setVersion("!somecrazyversion");
        assertThat(rule.isAllowed()).isTrue();
    }

    @Test
    void testInvalidFamily() {
        RequireOS rule = new RequireOS(new OperatingSystemProfileActivator());
        rule.setLog(Mockito.mock(EnforcerLogger.class));

        rule.setFamily("junk");
        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleError.class)
                .hasMessageStartingWith("Invalid Family type used. Valid family types are: ");
    }

    @Test
    void testId() {
        RequireOS rule = new RequireOS(new OperatingSystemProfileActivator());
        rule.setVersion("1.2");
        assertThat(rule.getCacheId()).isNotEmpty();
    }
}
