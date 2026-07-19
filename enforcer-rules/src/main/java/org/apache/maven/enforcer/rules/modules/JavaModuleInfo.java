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

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Immutable view of the {@code Module} attribute of a {@code module-info.class}:
 * the module name plus its {@code requires} / {@code exports} / {@code opens} directives
 * and the module's package set.
 * Package and module names are in the source form (dot-separated).
 */
final class JavaModuleInfo {

    private final String name;
    private final boolean open;
    private final List<String> requires;
    private final List<Directive> exports;
    private final List<Directive> opens;
    private final Set<String> packages;

    JavaModuleInfo(
            String name,
            boolean open,
            List<String> requires,
            List<Directive> exports,
            List<Directive> opens,
            Set<String> packages) {
        this.name = name;
        this.open = open;
        this.requires = Collections.unmodifiableList(requires);
        this.exports = Collections.unmodifiableList(exports);
        this.opens = Collections.unmodifiableList(opens);
        this.packages = Collections.unmodifiableSet(packages);
    }

    /** The module name, e.g. {@code com.example.foo}. */
    String name() {
        return name;
    }

    /**
     * {@code true} for an {@code open module}, which implicitly opens <em>all</em> its packages for
     * deep reflection (its {@link #opens()} list is then empty).
     */
    boolean isOpen() {
        return open;
    }

    /** Names of required modules. */
    List<String> requires() {
        return requires;
    }

    /** {@code exports} directives. */
    List<Directive> exports() {
        return exports;
    }

    /** {@code opens} directives. */
    List<Directive> opens() {
        return opens;
    }

    /**
     * All packages of the module, as reported by {@code ModuleDescriptor.packages()}.
     *
     * <p>Complete when the descriptor comes from a packaged artifact (the {@code ModulePackages}
     * attribute is written at packaging time, and {@code ModuleFinder} falls back to scanning the
     * artifact). For a bare {@code module-info.class} read from an output directory it may contain
     * only the exported/opened packages — enumerate the directory instead when completeness
     * matters there.</p>
     */
    Set<String> packages() {
        return packages;
    }

    /** A single {@code exports}/{@code opens} directive: a package and its optional target modules. */
    static final class Directive {
        private final String packageName;
        private final List<String> targets;

        Directive(String packageName, List<String> targets) {
            this.packageName = packageName;
            this.targets = Collections.unmodifiableList(targets);
        }

        /** Exported/opened package, e.g. {@code com.example.foo.api}. */
        String packageName() {
            return packageName;
        }

        /** Target modules for a qualified directive; empty for an unqualified one. */
        List<String> targets() {
            return targets;
        }

        /** {@code true} if this is a qualified {@code exports ... to} / {@code opens ... to}. */
        boolean isQualified() {
            return !targets.isEmpty();
        }
    }
}
