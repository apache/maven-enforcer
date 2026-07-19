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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.dependency.ResolverUtil;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Scans dependencies via java.lang.module.ModuleFinder, which exists only on Java 9+.
@EnabledForJreRange(min = JRE.JAVA_9)
class BanSplitPackagesTest {

    @TempDir
    File outputDirectory;

    @TempDir
    File dependencyDirectory;

    private final MavenProject project = mock(MavenProject.class);
    private final ResolverUtil resolverUtil = mock(ResolverUtil.class);
    private final EnforcerLogger log = mock(EnforcerLogger.class);
    private BanSplitPackages rule;

    @BeforeEach
    void setUp() throws Exception {
        Build build = mock(Build.class);
        when(build.getOutputDirectory()).thenReturn(outputDirectory.getAbsolutePath());
        when(project.getBuild()).thenReturn(build);
        rule = new BanSplitPackages(project, resolverUtil);
        rule.setLog(log);
        dependencies(); // no dependencies unless a test sets some
    }

    private void dependencies(File... files) throws Exception {
        DefaultDependencyNode root = new DefaultDependencyNode(new DefaultArtifact("test:project:1.0"));
        List<DependencyNode> children = new ArrayList<>();
        int index = 0;
        for (File file : files) {
            Artifact artifact = new DefaultArtifact("test:dep" + ++index + ":1.0").setFile(file);
            children.add(new DefaultDependencyNode(new Dependency(artifact, "compile")));
        }
        root.setChildren(children);
        when(resolverUtil.resolveTransitiveDependencies(anyBoolean(), anyBoolean(), anyBoolean(), anyList()))
                .thenReturn(root);
    }

    private File modularDependency(String moduleName, String... classNames) throws Exception {
        ModuleInfoFixtures.Builder builder = ModuleInfoFixtures.module(moduleName);
        for (String className : classNames) {
            builder.packages(className.substring(0, className.lastIndexOf('.')));
        }
        return ModuleInfoFixtures.writeJar(
                new File(dependencyDirectory, moduleName + ".jar"), builder.toBytes(), null, classNames);
    }

    private void modularProject(String... classNames) throws Exception {
        ModuleInfoFixtures.module("com.example.app").writeTo(outputDirectory);
        for (String className : classNames) {
            ModuleInfoFixtures.writeDummyClass(outputDirectory, className);
        }
    }

    @Test
    void disjointPackagesPass() throws Exception {
        modularProject("com.example.app.Main");
        dependencies(modularDependency("com.acme.lib", "com.acme.lib.Util"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void overlapBetweenProjectModuleAndUnrequiredExplicitModuleFails() throws Exception {
        // the silent variant: the project does not even `requires` the split module
        modularProject("com.acme.shared.Copied", "com.example.app.Main");
        dependencies(modularDependency("com.acme.lib", "com.acme.shared.Original"));
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("com.acme.shared"));
        assertTrue(e.getMessage().contains("com.example.app"));
        assertTrue(e.getMessage().contains("com.acme.lib"));
    }

    @Test
    void overlapBetweenTwoExplicitDependencyModulesFails() throws Exception {
        modularProject("com.example.app.Main");
        dependencies(
                modularDependency("com.acme.one", "com.acme.shared.A"),
                modularDependency("com.acme.two", "com.acme.shared.B"));
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("com.acme.shared"));
    }

    @Test
    void overlapWithRequiredAutomaticModuleFails() throws Exception {
        ModuleInfoFixtures.module("com.example.app").requires("acme.auto").writeTo(outputDirectory);
        ModuleInfoFixtures.writeDummyClass(outputDirectory, "com.acme.shared.Copied");
        dependencies(ModuleInfoFixtures.writeJar(
                new File(dependencyDirectory, "auto.jar"), null, "acme.auto", "com.acme.shared.Original"));
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("acme.auto"));
    }

    @Test
    void overlapWithUnrequiredAutomaticModuleOnlyWarnsByDefault() throws Exception {
        modularProject("com.acme.shared.Copied");
        dependencies(ModuleInfoFixtures.writeJar(
                new File(dependencyDirectory, "auto.jar"), null, "acme.auto", "com.acme.shared.Original"));
        assertDoesNotThrow(rule::execute);
        ArgumentCaptor<CharSequence> warning = ArgumentCaptor.forClass(CharSequence.class);
        verify(log).warn(warning.capture());
        assertTrue(warning.getValue().toString().contains("com.acme.shared"));
    }

    @Test
    void overlapWithFilenameDerivedAutomaticModuleOnlyWarnsByDefault() throws Exception {
        modularProject("com.acme.shared.Copied");
        // no manifest entry: the automatic name is derived from the file name ("plain")
        dependencies(ModuleInfoFixtures.writeJar(
                new File(dependencyDirectory, "plain-1.0-SNAPSHOT.jar"), null, null, "com.acme.shared.Original"));
        assertDoesNotThrow(rule::execute);
        verify(log).warn(ArgumentCaptor.forClass(CharSequence.class).capture());
    }

    @Test
    void overlapWithNonModularDirectoryDependencyOnlyWarnsByDefault() throws Exception {
        // e.g. a reactor sibling resolved to its target/classes, without a module-info.class
        modularProject("com.acme.shared.Copied");
        File classesDirectory = new File(dependencyDirectory, "classes");
        ModuleInfoFixtures.writeDummyClass(classesDirectory, "com.acme.shared.Original");
        dependencies(classesDirectory);
        assertDoesNotThrow(rule::execute);
        ArgumentCaptor<CharSequence> warning = ArgumentCaptor.forClass(CharSequence.class);
        verify(log).warn(warning.capture());
        assertTrue(warning.getValue().toString().contains("com.acme.shared"));
    }

    @Test
    void classpathSeverityErrorFailsOnUnrequiredAutomaticModule() throws Exception {
        modularProject("com.acme.shared.Copied");
        dependencies(ModuleInfoFixtures.writeJar(
                new File(dependencyDirectory, "auto.jar"), null, "acme.auto", "com.acme.shared.Original"));
        rule.setClasspathSeverity("error");
        assertThrows(EnforcerRuleException.class, rule::execute);
    }

    @Test
    void classpathSeverityIgnoreStaysSilent() throws Exception {
        modularProject("com.acme.shared.Copied");
        dependencies(ModuleInfoFixtures.writeJar(
                new File(dependencyDirectory, "auto.jar"), null, "acme.auto", "com.acme.shared.Original"));
        rule.setClasspathSeverity("ignore");
        assertDoesNotThrow(rule::execute);
        verify(log, never()).warn((CharSequence) org.mockito.ArgumentMatchers.any());
    }

    @Test
    void allowedSplitPackagesPass() throws Exception {
        modularProject("com.acme.shared.Copied");
        dependencies(modularDependency("com.acme.lib", "com.acme.shared.Original"));
        rule.setAllowedSplitPackages(Arrays.asList("com.acme.shared"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void ignoredModulesPass() throws Exception {
        modularProject("com.acme.shared.Copied");
        dependencies(modularDependency("com.acme.lib", "com.acme.shared.Original"));
        rule.setIgnoredModules(Arrays.asList("com.acme.lib"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void ignoredArtifactsPass() throws Exception {
        modularProject("com.acme.shared.Copied");
        dependencies(modularDependency("com.acme.lib", "com.acme.shared.Original"));
        rule.setIgnoredArtifacts(Arrays.asList("test:dep1"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void moduleSourceHierarchyOverlapBetweenProjectModulesFails() throws Exception {
        // Maven 4 module source hierarchy: one output directory per module
        ModuleInfoFixtures.module("com.example.api").writeTo(new File(outputDirectory, "com.example.api"));
        ModuleInfoFixtures.writeDummyClass(new File(outputDirectory, "com.example.api"), "com.acme.shared.A");
        ModuleInfoFixtures.module("com.example.core").writeTo(new File(outputDirectory, "com.example.core"));
        ModuleInfoFixtures.writeDummyClass(new File(outputDirectory, "com.example.core"), "com.acme.shared.B");
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("com.acme.shared"));
        assertTrue(e.getMessage().contains("com.example.api"));
        assertTrue(e.getMessage().contains("com.example.core"));
    }

    @Test
    void invalidClasspathSeverityIsRejected() throws Exception {
        rule.setClasspathSeverity("fatal");
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertTrue(e.getMessage().contains("classpathSeverity"));
    }

    @Test
    void nonModularProjectWithoutOverlapPasses() throws Exception {
        ModuleInfoFixtures.writeDummyClass(outputDirectory, "com.example.app.Main");
        dependencies(modularDependency("com.acme.lib", "com.acme.lib.Util"));
        assertDoesNotThrow(rule::execute);
    }
}
