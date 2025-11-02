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
package org.apache.maven.enforcer.rules.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.enforcer.rules.utils.ArtifactMatcher.Pattern;
import org.apache.maven.enforcer.rules.version.RequireJavaVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TestArtifactMatcher {

    Collection<String> patterns = new ArrayList<>();

    Collection<String> ignorePatterns = new ArrayList<>();

    @Test
    void patternInvalidInput() {
        try {
            new Pattern(null);
            fail("NullPointerException expected.");
        } catch (NullPointerException ignored) {
        }

        try {
            new Pattern("a:b:c:d:e:f:g");
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            new Pattern("a::");
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            Pattern p = new Pattern("*");
            p.match((Artifact) null);
            fail("NullPointerException expected.");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    void pattern() throws Exception {
        assertPatternDoesMatch("groupId:artifactId:1.0:jar:compile", "groupId", "artifactId", "1.0", "compile", "jar");

        assertPatternDoesMatch("groupId:artifactId:1.0:jar:compile", "groupId", "artifactId", "1.0", "", "");

        assertPatternDoesMatch("groupId:artifactId:1.0", "groupId", "artifactId", "1.0", "", "");

        assertPatternDoesMatch("groupId:artifactId:1.0", "groupId", "artifactId", "1.1", "", "");

        assertPatternDoesNotMatch("groupId:artifactId:[1.0]", "groupId", "artifactId", "1.1", "", "");

        assertPatternDoesMatch("groupId:*:1.0", "groupId", "artifactId", "1.0", "test", "");

        assertPatternDoesMatch("*:*:1.0", "groupId", "artifactId", "1.0", "", "");

        assertPatternDoesMatch("*:artifactId:*", "groupId", "artifactId", "1.0", "", "");

        assertPatternDoesMatch("*", "groupId", "artifactId", "1.0", "", "");

        // MENFORCER-74/75
        assertPatternDoesMatchWithClassifier(
                "*:*:*:jar:compile:tests", "groupId", "artifactId", "1.0", "", "", "tests");

        // MENFORCER-83
        assertPatternDoesMatch("*upId", "groupId", "artifactId", "1.0", "", "");

        assertPatternDoesMatch("gr*pId:?rt?f?ct?d:1.0", "groupId", "artifactId", "1.0", "", "");

        assertPatternDoesMatch("org.apache.*:maven-*:*", "org.apache.maven", "maven-core", "3.0", "", "");
    }

    @Test
    void match() {
        patterns.add("groupId:artifactId:1.0");
        patterns.add("*:anotherArtifact");

        ignorePatterns.add("badGroup:*:*:test");
        ignorePatterns.add("*:anotherArtifact:1.1");

        ArtifactMatcher matcher = new ArtifactMatcher(patterns, ignorePatterns);

        executeMatch(matcher, "groupId", "artifactId", "1.0", "", "", true);

        executeMatch(matcher, "groupId", "anotherArtifact", "1.0", "", "", true);

        executeMatch(matcher, "badGroup", "artifactId", "1.0", "", "test", false);

        executeMatch(matcher, "badGroup", "anotherArtifact", "1.0", "", "", true);

        executeMatch(matcher, "groupId", "anotherArtifact", "1.1", "", "", false);
    }

    private void assertPatternDoesMatch(
            final String pattern,
            final String groupId,
            final String artifactId,
            final String versionRange,
            final String scope,
            final String type) {
        assertPatternDoesMatchWithClassifier(pattern, groupId, artifactId, versionRange, scope, type, "");
    }

    private void assertPatternDoesNotMatch(
            final String pattern,
            final String groupId,
            final String artifactId,
            final String versionRange,
            final String scope,
            final String type) {
        assertPatternDoesNotMatchWithClassifier(pattern, groupId, artifactId, versionRange, scope, type, "");
    }

    private void assertPatternDoesMatchWithClassifier(
            final String pattern,
            final String groupId,
            final String artifactId,
            final String versionRange,
            final String scope,
            final String type,
            final String classifier) {
        assertTrue(new ArtifactMatcher.Pattern(pattern)
                .match(createMockArtifact(groupId, artifactId, versionRange, scope, type, classifier)));
    }

    private void assertPatternDoesNotMatchWithClassifier(
            final String pattern,
            final String groupId,
            final String artifactId,
            final String versionRange,
            final String scope,
            final String type,
            final String classifier) {
        assertFalse(new ArtifactMatcher.Pattern(pattern)
                .match(createMockArtifact(groupId, artifactId, versionRange, scope, type, classifier)));
    }

    private void executeMatch(
            final ArtifactMatcher matcher,
            final String groupId,
            final String artifactId,
            final String versionRange,
            final String scope,
            final String type,
            final boolean expectedResult) {
        assertEquals(
                expectedResult, matcher.match(createMockArtifact(groupId, artifactId, versionRange, scope, type, "")));
    }

    private static Artifact createMockArtifact(
            final String groupId,
            final String artifactId,
            final String versionRange,
            final String scope,
            final String type,
            final String classifier) {
        ArtifactHandler artifactHandler = new DefaultArtifactHandler();

        VersionRange version = VersionRange.createFromVersion(versionRange);
        return new DefaultArtifact(groupId, artifactId, version, scope, type, classifier, artifactHandler);
    }

    /**
     * Test contains version.
     *
     * @throws InvalidVersionSpecificationException the invalid version specification exception
     */
    @Test
    void containsVersion() throws Exception {
        ArtifactVersion version = new DefaultArtifactVersion("2.0.5");
        // test ranges
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.0.5,)"), version));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.0.4,)"), version));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.0.4,2.0.5]"), version));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.0.4,2.0.6]"), version));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.0.4,2.0.6)"), version));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.0,)"), version));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.0.0,)"), version));
        // not matching versions
        assertFalse(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.0.4,2.0.5)"), version));
        assertFalse(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.0.6,)"), version));
        assertFalse(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("(2.0.5,)"), version));

        // test singular versions -> 2.0.5 == [2.0.5,) or x >= 2.0.5
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("2.0"), version));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("2.0.4"), version));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("2.0.5"), version));

        assertFalse(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("2.0.6"), version));

        version = new DefaultArtifactVersion("1.5.0-7");
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[1.5.0,)"), version));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[1.5,1.6)"), version));

        version = new DefaultArtifactVersion(RequireJavaVersion.normalizeJDKVersion("1.5.0-07"));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[1.5.0,)"), version));
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[1.5,1.6)"), version));

        // MENFORCER-50
        version = new DefaultArtifactVersion("2.1.0-M1-RC12");
        assertTrue(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.1.0-M1-RC12,)"), version));
        assertFalse(ArtifactMatcher.containsVersion(VersionRange.createFromVersionSpec("[2.1.0-M1,)"), version));
    }
}
