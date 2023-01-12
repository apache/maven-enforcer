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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * The Class TestAbstractVersionEnforcer.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
class TestAbstractVersionEnforcer {

    /**
     * Enforce false.
     *
     * @param rule the rule
     * @param var the var
     * @param range the range
     * @param version the version
     */
    private void enforceFalse(AbstractVersionEnforcer rule, String var, String range, ArtifactVersion version) {
        try {
            rule.enforceVersion(var, range, version);
            fail("Expected to receive EnforcerRuleException because:" + version + " is not contained by " + range);
        } catch (Exception e) {
            if (e instanceof EnforcerRuleException) {
                // log.info( "Caught Expected Exception: " +
                // e.getLocalizedMessage() );
            } else {
                fail("Received wrong exception. Expected EnforcerRuleExeption. Received:" + e);
            }
        }
    }

    /**
     * Test enforce version.
     */
    @Test
    void testEnforceVersion() throws Exception {
        RequireJavaVersion rule = new RequireJavaVersion();
        rule.setLog(mock(EnforcerLogger.class));

        ArtifactVersion version = new DefaultArtifactVersion("2.0.5");

        // test ranges
        rule.enforceVersion("test", "[2.0.5,)", version);
        rule.enforceVersion("test", "[2.0.4,)", version);
        rule.enforceVersion("test", "[2.0.4,2.0.5]", version);
        rule.enforceVersion("test", "[2.0.4,2.0.6]", version);
        rule.enforceVersion("test", "[2.0.4,2.0.6)", version);
        rule.enforceVersion("test", "[2.0,)", version);
        rule.enforceVersion("test", "[2.0.0,)", version);

        // test singular versions -> 2.0.5 == [2.0.5,) or x >= 2.0.5
        rule.enforceVersion("test", "2.0", version);
        rule.enforceVersion("test", "2.0.4", version);
        rule.enforceVersion("test", "2.0.5", version);

        enforceFalse(rule, "test", "[2.0.6,)", version);
        enforceFalse(rule, "test", "(2.0.5,)", version);
        enforceFalse(rule, "test", "2.0.6", version);

        enforceFalse(rule, "test", "[2.0.4,2.0.5)", version);

        // make sure to handle the invalid range specification
        enforceFalse(rule, "test", "[[2.0.4,2.0.5)", version);
    }
}
