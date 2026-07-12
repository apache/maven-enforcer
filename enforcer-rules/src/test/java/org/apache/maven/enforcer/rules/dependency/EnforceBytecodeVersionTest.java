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
package org.apache.maven.enforcer.rules.dependency;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

class EnforceBytecodeVersionTest {

    @Test
    void filterDependenciesExcludesMatchesWithoutIncludes() throws Exception {
        EnforceBytecodeVersion rule = newRule();
        setField(rule, "excludes", Collections.singletonList("org.example:excluded"));

        Dependency included = dependency("org.example", "included");
        Dependency excluded = dependency("org.example", "excluded");

        assertEquals(Collections.singletonList(included), filterDependencies(rule, Arrays.asList(included, excluded)));
    }

    @Test
    void filterDependenciesIncludesOverrideExcludes() throws Exception {
        EnforceBytecodeVersion rule = newRule();
        setField(rule, "excludes", Collections.singletonList("org.example:*"));
        setField(rule, "includes", Collections.singletonList("org.example:included"));

        Dependency included = dependency("org.example", "included");
        Dependency excluded = dependency("org.example", "excluded");

        assertEquals(Collections.singletonList(included), filterDependencies(rule, Arrays.asList(included, excluded)));
    }

    private static EnforceBytecodeVersion newRule() {
        return new EnforceBytecodeVersion(
                mock(org.apache.maven.execution.MavenSession.class), mock(ResolverUtil.class));
    }

    private static Dependency dependency(String groupId, String artifactId) {
        return new Dependency(new DefaultArtifact(groupId + ":" + artifactId + ":jar:1.0"), "compile");
    }

    private static void setField(EnforceBytecodeVersion rule, String name, List<String> value) throws Exception {
        Field field = EnforceBytecodeVersion.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(rule, value);
    }

    @SuppressWarnings("unchecked")
    private static List<Dependency> filterDependencies(EnforceBytecodeVersion rule, List<Dependency> dependencies)
            throws Exception {
        Method method = EnforceBytecodeVersion.class.getDeclaredMethod("filterDependencies", List.class);
        method.setAccessible(true);
        return (List<Dependency>) method.invoke(rule, dependencies);
    }

    static Stream<Arguments> renderVersion() {
        return Stream.of(
                arguments("44.0", 44, 0),
                arguments("JDK 1.5", 49, 0),
                arguments("JDK 1.7", 51, 0),
                arguments("51.3", 51, 3),
                arguments("JDK 8", 52, 0),
                arguments("JDK 11", 55, 0),
                arguments("JDK 12", 56, 0),
                arguments("JDK 21", 65, 0),
                arguments("JDK 26", 70, 0),
                arguments("JDK 57", 101, 0));
    }

    @ParameterizedTest
    @MethodSource
    void renderVersion(String expected, int major, int minor) {
        assertEquals(expected, EnforceBytecodeVersion.renderVersion(major, minor));
    }

    public static Stream<Arguments> decodeMajorVersion() {
        return Stream.of(
                arguments("1.1", 45),
                arguments("1.2", 46),
                arguments("1.3", 47),
                arguments("1.4", 48),
                arguments("1.5", 49),
                arguments("1.6", 50),
                arguments("1.7", 51),
                arguments("1.8", 52),
                arguments("8", 52),
                arguments("1.9", 53),
                arguments("9", 53),
                arguments("11", 55),
                arguments("12", 56),
                arguments("21", 65),
                arguments("26", 70),
                arguments("57", 101));
    }

    @ParameterizedTest
    @MethodSource
    void decodeMajorVersion(String version, int expectedMajor) {
        assertEquals(expectedMajor, EnforceBytecodeVersion.decodeMajorVersion(version));
    }
}
