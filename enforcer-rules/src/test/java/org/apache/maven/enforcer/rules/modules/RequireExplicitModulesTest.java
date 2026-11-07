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
class RequireExplicitModulesTest {

    @TempDir
    File outputDirectory;

    private final MavenProject project = mock(MavenProject.class);
    private RequireExplicitModules rule;

    @BeforeEach
    void setUp() {
        Build build = mock(Build.class);
        when(build.getOutputDirectory()).thenReturn(outputDirectory.getAbsolutePath());
        when(project.getBuild()).thenReturn(build);
        rule = new RequireExplicitModules(project);
        rule.setLog(mock(EnforcerLogger.class));
    }

    @Test
    void aModularProjectWithClassesPasses() throws Exception {
        ModuleInfoFixtures.writeDummyClass(outputDirectory, "com.example.foo.Bar");
        ModuleInfoFixtures.module("com.example.foo").exports("com.example.foo").writeTo(outputDirectory);
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void compiledClassesWithoutModuleInfoFail() throws Exception {
        when(project.getArtifactId()).thenReturn("foo");
        ModuleInfoFixtures.writeDummyClass(outputDirectory, "com.example.foo.Bar");
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("automatic module"), e.getMessage());
    }

    @Test
    void aProjectWithNoCompiledClassesPasses() {
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void anOutputWithOnlyModuleInfoPasses() throws Exception {
        // module-info.class but no other classes: nothing to encapsulate, so the rule stays silent.
        ModuleInfoFixtures.module("com.example.foo").writeTo(outputDirectory);
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void aModuleSourceHierarchyWithExplicitModulesPasses() throws Exception {
        // Maven 4 module source hierarchy: each module compiles to its own subdirectory.
        File api = new File(outputDirectory, "com.example.api");
        ModuleInfoFixtures.writeDummyClass(api, "com.example.api.Service");
        ModuleInfoFixtures.module("com.example.api").exports("com.example.api").writeTo(api);
        File core = new File(outputDirectory, "com.example.core");
        ModuleInfoFixtures.writeDummyClass(core, "com.example.core.Impl");
        ModuleInfoFixtures.module("com.example.core")
                .requires("com.example.api")
                .writeTo(core);
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void aModuleSourceHierarchyWithANonModularOutputFails() throws Exception {
        when(project.getArtifactId()).thenReturn("foo");
        File api = new File(outputDirectory, "com.example.api");
        ModuleInfoFixtures.writeDummyClass(api, "com.example.api.Service");
        ModuleInfoFixtures.module("com.example.api").exports("com.example.api").writeTo(api);
        // sibling output with classes but no module descriptor
        File legacy = new File(outputDirectory, "com.example.legacy");
        ModuleInfoFixtures.writeDummyClass(legacy, "com.example.legacy.Old");
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("com.example.legacy"), e.getMessage());
    }

    @Test
    void aCustomMessageIsUsed() throws Exception {
        when(project.getArtifactId()).thenReturn("foo");
        ModuleInfoFixtures.writeDummyClass(outputDirectory, "com.example.foo.Bar");
        rule.setMessage("please add module-info.java");
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("please add module-info.java"), e.getMessage());
    }
}
