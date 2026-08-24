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

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EnforceBytecodeVersionTest {

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
