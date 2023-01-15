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

import java.util.Collections;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequirePrerequisiteTest {

    @Mock
    private MavenProject project;

    @Mock
    private EnforcerLogger log;

    @InjectMocks
    private RequirePrerequisite rule;

    @BeforeEach
    void before() {
        rule.setLog(log);
        when(project.getPackaging()).thenReturn("maven-plugin");
    }

    @Test
    void testNoPrerequisite() {
        assertThrows(EnforcerRuleException.class, () -> rule.execute());
    }

    @Test
    void testNoSpecifiedPrerequisite() throws Exception {
        when(project.getPrerequisites()).thenReturn(new Prerequisites());
        rule.execute();
    }

    @Test
    void testLowerMavenPrerequisite() {
        when(project.getPrerequisites()).thenReturn(new Prerequisites());

        assertThrows(EnforcerRuleException.class, () -> {
            rule.setMavenVersion("3.0");
            rule.execute();
        });
    }

    @Test
    void testLowerMavenRangePrerequisite() {
        when(project.getPrerequisites()).thenReturn(new Prerequisites());

        assertThrows(EnforcerRuleException.class, () -> {
            rule.setMavenVersion("[3.0,)");
            rule.execute();
        });
    }

    @Test
    void testMavenRangesPrerequisite() {
        assertThrows(EnforcerRuleException.class, () -> {
            Prerequisites prerequisites = new Prerequisites();
            prerequisites.setMaven("2.2.0");
            when(project.getPrerequisites()).thenReturn(prerequisites);

            rule.setMavenVersion("[2.0.6,2.1.0),(2.1.0,2.2.0),(2.2.0,)");
            rule.execute();
        });
    }

    @Test
    void testValidPrerequisite() throws Exception {
        Prerequisites prerequisites = new Prerequisites();
        prerequisites.setMaven("3.0");
        when(project.getPrerequisites()).thenReturn(prerequisites);

        rule.setMavenVersion("2.2.1");
        rule.execute();
    }

    @Test
    void testPomPackaging() throws Exception {
        when(project.getPackaging()).thenReturn("pom");

        rule.execute();

        verify(log).debug("Packaging is pom, skipping requirePrerequisite rule");
    }

    @Test
    void testMatchingPackagings() {
        assertThrows(EnforcerRuleException.class, () -> {
            when(project.getPackaging()).thenReturn("maven-plugin");

            rule.setPackagings(Collections.singletonList("maven-plugin"));
            rule.execute();
        });
    }

    @Test
    void testNotMatchingPackagings() throws Exception {
        when(project.getPackaging()).thenReturn("jar");

        rule.setPackagings(Collections.singletonList("maven-plugin"));
        rule.execute();

        verify(log).debug("Packaging is jar, skipping requirePrerequisite rule");
    }
}
