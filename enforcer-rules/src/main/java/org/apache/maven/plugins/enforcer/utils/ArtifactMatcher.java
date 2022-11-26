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
package org.apache.maven.plugins.enforcer.utils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.enforcer.AbstractVersionEnforcer;

/**
 * This class is used for matching Artifacts against a list of patterns.
 *
 * @author Jakub Senko
 * @see org.apache.maven.plugins.enforcer.BanTransitiveDependencies
 */
public final class ArtifactMatcher {

    /**
     * @author I don't know
     */
    public static class Pattern {
        private String pattern;

        private String[] parts;

        public Pattern(String pattern) {
            if (pattern == null) {
                throw new NullPointerException("pattern");
            }

            this.pattern = pattern;

            parts = pattern.split(":", 7);

            if (parts.length == 7) {
                throw new IllegalArgumentException("Pattern contains too many delimiters.");
            }

            for (String part : parts) {
                if ("".equals(part)) {
                    throw new IllegalArgumentException("Pattern or its part is empty.");
                }
            }
        }

        public boolean match(Artifact artifact) throws InvalidVersionSpecificationException {
            Objects.requireNonNull(artifact, "artifact must not be null");
            return match(
                    artifact.getGroupId(),
                    artifact.getArtifactId(),
                    artifact.getVersion(),
                    artifact.getType(),
                    artifact.getScope(),
                    artifact.getClassifier());
        }

        public boolean match(Dependency dependency) throws InvalidVersionSpecificationException {
            Objects.requireNonNull(dependency, "dependency must not be null");
            return match(
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    dependency.getType(),
                    dependency.getScope(),
                    dependency.getClassifier());
        }

        private boolean match(
                String groupId, String artifactId, String version, String type, String scope, String classifier)
                throws InvalidVersionSpecificationException {
            switch (parts.length) {
                case 6:
                    if (!matches(parts[5], classifier)) {
                        return false;
                    }
                case 5:
                    if (scope == null || scope.equals("")) {
                        scope = Artifact.SCOPE_COMPILE;
                    }

                    if (!matches(parts[4], scope)) {
                        return false;
                    }
                case 4:
                    if (type == null || type.equals("")) {
                        type = "jar";
                    }

                    if (!matches(parts[3], type)) {
                        return false;
                    }

                case 3:
                    if (!matches(parts[2], version)) {
                        // CHECKSTYLE_OFF: LineLength
                        if (!AbstractVersionEnforcer.containsVersion(
                                VersionRange.createFromVersionSpec(parts[2]), new DefaultArtifactVersion(version)))
                        // CHECKSTYLE_ON: LineLength
                        {
                            return false;
                        }
                    }

                case 2:
                    if (!matches(parts[1], artifactId)) {
                        return false;
                    }
                case 1:
                    return matches(parts[0], groupId);
                default:
                    throw new AssertionError();
            }
        }

        private boolean matches(String expression, String input) {
            String regex = expression
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace(":", "\\:")
                    .replace('?', '.')
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace("(", "\\(")
                    .replace(")", "\\)");

            // TODO: Check if this can be done better or prevented earlier.
            if (input == null) {
                input = "";
            }

            return java.util.regex.Pattern.matches(regex, input);
        }

        @Override
        public String toString() {
            return pattern;
        }
    }

    private Collection<Pattern> patterns = new LinkedList<>();

    private Collection<Pattern> ignorePatterns = new LinkedList<>();

    /**
     * Construct class by providing patterns as strings. Empty strings are ignored.
     *
     * @param patterns includes
     * @param ignorePatterns excludes
     * @throws NullPointerException if any of the arguments is null
     */
    public ArtifactMatcher(final Collection<String> patterns, final Collection<String> ignorePatterns) {
        Objects.requireNonNull(patterns, "patterns must not be null");
        Objects.requireNonNull(ignorePatterns, "ignorePatterns must not be null");
        for (String pattern : patterns) {
            if (pattern != null && !"".equals(pattern)) {
                this.patterns.add(new Pattern(pattern));
            }
        }

        for (String ignorePattern : ignorePatterns) {
            if (ignorePattern != null && !"".equals(ignorePattern)) {
                this.ignorePatterns.add(new Pattern(ignorePattern));
            }
        }
    }

    /**
     * Check if artifact matches patterns.
     *
     * @param artifact the artifact to match
     * @return {@code true} if artifact matches any {@link #patterns} and none of the {@link #ignorePatterns}, otherwise
     *         {@code false}
     * @throws InvalidVersionSpecificationException if any pattern contains an invalid version range
     */
    public boolean match(Artifact artifact) throws InvalidVersionSpecificationException {
        for (Pattern pattern : patterns) {
            if (pattern.match(artifact)) {
                for (Pattern ignorePattern : ignorePatterns) {
                    if (ignorePattern.match(artifact)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Check if dependency matches patterns.
     *
     * @param dependency the dependency to match
     * @return {@code true} if dependency matches any {@link #patterns} and none of the {@link #ignorePatterns},
     *         otherwise {@code false}
     * @throws InvalidVersionSpecificationException if any pattern contains an invalid version range
     */
    public boolean match(Dependency dependency) throws InvalidVersionSpecificationException {
        for (Pattern pattern : patterns) {
            if (pattern.match(dependency)) {
                for (Pattern ignorePattern : ignorePatterns) {
                    if (ignorePattern.match(dependency)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
