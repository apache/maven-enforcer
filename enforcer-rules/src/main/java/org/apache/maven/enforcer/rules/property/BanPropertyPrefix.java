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
package org.apache.maven.enforcer.rules.property;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;
import org.apache.maven.project.MavenProject;

/**
 * This rule checks that no Maven property starts with any of the configured banned prefixes and fails the build if
 * one or more offending properties are found. Each prefix entry may also contain several prefixes separated by
 * commas. This is useful to forbid properties that are reserved for other tooling or no longer supported.
 *
 * @see <a href="https://issues.apache.org/jira/browse/MENFORCER-433">MENFORCER-433</a>
 * @since 3.7.0
 */
@Named("banPropertyPrefix")
public final class BanPropertyPrefix extends AbstractStandardEnforcerRule {

    private static final String PREFIX_SEPARATOR = ",";

    private static final String MESSAGE_SEPARATOR = ", ";

    /**
     * List of prefixes that identify banned properties. Any project property whose name starts with one of these
     * prefixes makes the rule fail. Each entry may also contain several prefixes separated by commas.
     */
    private List<String> prefixes;

    private final MavenProject project;

    @Inject
    public BanPropertyPrefix(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    public void setPrefixes(List<String> prefixes) {
        this.prefixes = prefixes;
    }

    public List<String> getPrefixes() {
        return prefixes;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        Set<String> bannedPrefixes = normalizePrefixes(prefixes);
        if (bannedPrefixes.isEmpty()) {
            return;
        }

        Properties properties = project.getProperties();
        Set<String> bannedPropertiesFound = properties.stringPropertyNames().stream()
                .filter(property -> bannedPrefixes.stream().anyMatch(property::startsWith))
                .collect(Collectors.toCollection(TreeSet::new));

        if (!bannedPropertiesFound.isEmpty()) {
            String message = getMessage();
            if (message == null) {
                message = "Banned properties found (prefixes: " + String.join(MESSAGE_SEPARATOR, bannedPrefixes)
                        + "): "
                        + String.join(MESSAGE_SEPARATOR, bannedPropertiesFound);
            }
            throw new EnforcerRuleException(message);
        }
    }

    static Set<String> normalizePrefixes(List<String> prefixes) {
        if (prefixes == null) {
            return new LinkedHashSet<>();
        }
        return prefixes.stream()
                .filter(Objects::nonNull)
                .flatMap(entry -> Arrays.stream(entry.split(PREFIX_SEPARATOR)))
                .map(String::trim)
                .filter(prefix -> !prefix.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String toString() {
        return "BanPropertyPrefix[prefixes=" + String.join(MESSAGE_SEPARATOR, normalizePrefixes(prefixes)) + "]";
    }
}
