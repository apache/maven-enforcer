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
package org.apache.maven.enforcer.rules.modules;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Reads module-info via java.lang.module.ModuleDescriptor, which exists only on Java 9+.
@EnabledForJreRange(min = JRE.JAVA_9)
class BanUnjustifiedOpensTest {

    @TempDir
    File outputDirectory;

    private final MavenProject project = mock(MavenProject.class);
    private BanUnjustifiedOpens rule;

    @BeforeEach
    void setUp() {
        Build build = mock(Build.class);
        when(build.getOutputDirectory()).thenReturn(outputDirectory.getAbsolutePath());
        when(project.getBuild()).thenReturn(build);
        rule = new BanUnjustifiedOpens(project);
        rule.setLog(mock(EnforcerLogger.class));
    }

    @Test
    void qualifiedOpensPasses() throws Exception {
        ModuleInfoFixtures.module("com.example.foo")
                .requires("com.fasterxml.jackson.databind")
                .opens("com.example.foo.dto", "com.fasterxml.jackson.databind")
                .writeTo(outputDirectory);
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void unqualifiedOpensFails() throws Exception {
        ModuleInfoFixtures.module("com.example.foo")
                .opens("com.example.foo.dto")
                .writeTo(outputDirectory);
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("com.example.foo.dto"), e.getMessage());
    }

    @Test
    void whitelistedUnqualifiedOpensPasses() throws Exception {
        ModuleInfoFixtures.module("com.example.foo")
                .opens("com.example.foo.dto")
                .writeTo(outputDirectory);
        rule.setAllowedOpens(Arrays.asList("com.example.foo.dto"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void anOpenModuleFails() throws Exception {
        ModuleInfoFixtures.openModule("com.example.foo")
                .exports("com.example.foo.api")
                .writeTo(outputDirectory);
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("open module"), e.getMessage());
    }

    @Test
    void aModuleWithoutOpensPasses() throws Exception {
        ModuleInfoFixtures.module("com.example.foo")
                .exports("com.example.foo.api")
                .writeTo(outputDirectory);
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void aNonModularProjectPasses() {
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void anUnqualifiedOpensInAModuleSourceHierarchyFails() throws Exception {
        // Maven 4 module source hierarchy: each module compiles to its own subdirectory.
        ModuleInfoFixtures.module("com.example.api")
                .exports("com.example.api")
                .writeTo(new File(outputDirectory, "com.example.api"));
        ModuleInfoFixtures.module("com.example.cli")
                .opens("com.example.cli")
                .writeTo(new File(outputDirectory, "com.example.cli"));
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("com.example.cli"), e.getMessage());
    }
}
