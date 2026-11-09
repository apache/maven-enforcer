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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Reads module-info via java.lang.module.ModuleDescriptor, which exists only on Java 9+.
@EnabledForJreRange(min = JRE.JAVA_9)
class RequireMinimalExportsTest {

    @TempDir
    File outputDirectory;

    @TempDir
    File artifactDirectory;

    private final MavenProject project = mock(MavenProject.class);
    private RequireMinimalExports rule;
    private EnforcerLogger log;

    @BeforeEach
    void setUp() {
        Build build = mock(Build.class);
        when(build.getOutputDirectory()).thenReturn(outputDirectory.getAbsolutePath());
        when(project.getBuild()).thenReturn(build);
        rule = new RequireMinimalExports(project);
        log = mock(EnforcerLogger.class);
        rule.setLog(log);
    }

    @Test
    void exportingOnlyApiPackagesPasses() throws Exception {
        ModuleInfoFixtures.module("com.example.foo")
                .exports("com.example.foo.api")
                .writeTo(outputDirectory);
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void exportingAnInternalPackageFails() throws Exception {
        ModuleInfoFixtures.module("com.example.foo")
                .exports("com.example.foo.api")
                .exports("com.example.foo.internal")
                .writeTo(outputDirectory);
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("com.example.foo.internal"), e.getMessage());
    }

    @Test
    void qualifiedInternalExportIsIgnoredByDefault() throws Exception {
        ModuleInfoFixtures.module("com.example.foo")
                .requires("com.example.bar")
                .exports("com.example.foo.internal", "com.example.bar")
                .writeTo(outputDirectory);
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void qualifiedInternalExportFailsWhenNotIgnored() throws Exception {
        ModuleInfoFixtures.module("com.example.foo")
                .requires("com.example.bar")
                .exports("com.example.foo.internal", "com.example.bar")
                .writeTo(outputDirectory);
        rule.setIgnoreQualifiedExports(false);
        assertThrows(EnforcerRuleException.class, rule::execute);
    }

    @Test
    void whitelistedInternalExportPasses() throws Exception {
        ModuleInfoFixtures.module("com.example.foo")
                .exports("com.example.foo.internal")
                .writeTo(outputDirectory);
        rule.setAllowedExports(Arrays.asList("com.example.foo.internal"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void customPatternIsHonoured() throws Exception {
        ModuleInfoFixtures.module("com.example.foo")
                .exports("com.example.foo.secret")
                .writeTo(outputDirectory);
        rule.setInternalPackagePattern(".*\\.secret");
        assertThrows(EnforcerRuleException.class, rule::execute);
    }

    @Test
    void aNonModularProjectPasses() {
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void aModuleSourceHierarchyWithCleanExportsPasses() throws Exception {
        // Maven 4 module source hierarchy: each module compiles to its own subdirectory.
        ModuleInfoFixtures.module("com.example.api")
                .exports("com.example.api")
                .writeTo(new File(outputDirectory, "com.example.api"));
        ModuleInfoFixtures.module("com.example.core")
                .exports("com.example.core.service")
                .writeTo(new File(outputDirectory, "com.example.core"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void anInternalExportInAModuleSourceHierarchyFails() throws Exception {
        ModuleInfoFixtures.module("com.example.api")
                .exports("com.example.api")
                .writeTo(new File(outputDirectory, "com.example.api"));
        ModuleInfoFixtures.module("com.example.core")
                .exports("com.example.core.internal")
                .writeTo(new File(outputDirectory, "com.example.core"));
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("com.example.core.internal"), e.getMessage());
    }

    @Test
    void anUnbuiltJarProjectWarnsThatNothingWasChecked() {
        givenJarPackaging();
        assertDoesNotThrow(rule::execute);
        verify(log).warn(contains("checked nothing"));
        verify(log).warn(contains("enforcer-rules/requireMinimalExports.html"));
    }

    @Test
    void aBuiltButNonModularProjectStaysSilent() throws Exception {
        // Compiled classes without a descriptor: the project simply is not a module, which this rule
        // does not police -- warning here would fire on every non-modular project in the build.
        givenJarPackaging();
        ModuleInfoFixtures.writeDummyClass(outputDirectory, "com.example.foo.Bar");
        assertDoesNotThrow(rule::execute);
        verify(log, never()).warn(contains("checked nothing"));
    }

    @Test
    void anExportFoundOnlyInAnInjectedDescriptorIsStillChecked() throws Exception {
        // Moditect-style project: nothing in target/classes, the descriptor lives in the packaged JAR.
        // Bound at or after package the rule reads it from there, so the check is not quietly skipped.
        givenJarArtifactCarrying(ModuleInfoFixtures.module("com.example.foo")
                .exports("com.example.foo.internal")
                .toBytes());
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("com.example.foo.internal"), e.getMessage());
    }

    @Test
    void aConfiguredDescriptorInjectorIsNamedInTheWarning() throws Exception {
        // Bound before package with Moditect in the build: there are classes but no descriptor anywhere,
        // and no artifact yet -- the one case the "no compiled classes" guard cannot recognise.
        Plugin moditect = new Plugin();
        moditect.setGroupId("org.moditect");
        moditect.setArtifactId("moditect-maven-plugin");
        when(project.getBuildPlugins()).thenReturn(Collections.singletonList(moditect));
        givenJarPackaging();
        ModuleInfoFixtures.writeDummyClass(outputDirectory, "com.example.foo.Bar");
        assertDoesNotThrow(rule::execute);
        verify(log).warn(contains("moditect-maven-plugin"));
    }

    /** Make the project look like one that builds a JAR, the precondition for the "checked nothing" warning. */
    private void givenJarPackaging() {
        ArtifactHandler handler = mock(ArtifactHandler.class);
        when(handler.getExtension()).thenReturn("jar");
        Artifact artifact = mock(Artifact.class);
        when(artifact.getArtifactHandler()).thenReturn(handler);
        when(project.getArtifact()).thenReturn(artifact);
    }

    /** A packaged artifact carrying the given {@code module-info.class}, as a descriptor injector leaves it. */
    private void givenJarArtifactCarrying(byte[] descriptor) throws IOException {
        File jar = new File(artifactDirectory, "artifact.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
            out.putNextEntry(new JarEntry("module-info.class"));
            out.write(descriptor);
            out.closeEntry();
        }
        ArtifactHandler handler = mock(ArtifactHandler.class);
        when(handler.getExtension()).thenReturn("jar");
        Artifact artifact = mock(Artifact.class);
        when(artifact.getArtifactHandler()).thenReturn(handler);
        when(artifact.getFile()).thenReturn(jar);
        when(project.getArtifact()).thenReturn(artifact);
    }
}
