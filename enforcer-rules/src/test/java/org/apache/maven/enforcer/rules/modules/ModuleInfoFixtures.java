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
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Test-only helper that generates real {@code module-info.class} fixtures with ASM and writes them
 * (plus optional dummy classes) into an output directory, so the module rules can be exercised against
 * a {@code project.build.outputDirectory} exactly as they see it at build time.
 */
final class ModuleInfoFixtures {

    private ModuleInfoFixtures() {}

    static Builder module(String name) {
        return new Builder(name, false);
    }

    static Builder openModule(String name) {
        return new Builder(name, true);
    }

    /** Write an arbitrary (empty) compiled class so an output directory looks non-empty to a rule. */
    static void writeDummyClass(File outputDirectory, String binaryName) throws IOException {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, binaryName.replace('.', '/'), null, "java/lang/Object", null);
        cw.visitEnd();
        Files.createDirectories(outputDirectory.toPath());
        Files.write(new File(outputDirectory, binaryName + ".class").toPath(), cw.toByteArray());
    }

    static final class Builder {
        private final String name;
        private final boolean open;
        private final List<String> requires = new ArrayList<>();
        private final List<Directive> exports = new ArrayList<>();
        private final List<Directive> opens = new ArrayList<>();

        private Builder(String name, boolean open) {
            this.name = name;
            this.open = open;
        }

        Builder requires(String module) {
            requires.add(module);
            return this;
        }

        Builder exports(String packageName, String... targets) {
            exports.add(new Directive(packageName, targets));
            return this;
        }

        Builder opens(String packageName, String... targets) {
            opens.add(new Directive(packageName, targets));
            return this;
        }

        byte[] toBytes() {
            ClassWriter cw = new ClassWriter(0);
            cw.visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null);
            ModuleVisitor mv = cw.visitModule(name, open ? Opcodes.ACC_OPEN : 0, null);
            mv.visitRequire("java.base", Opcodes.ACC_MANDATED, null);
            for (String module : requires) {
                mv.visitRequire(module, 0, null);
            }
            for (Directive export : exports) {
                mv.visitExport(export.internalName(), 0, export.targetsOrNull());
            }
            for (Directive open : opens) {
                mv.visitOpen(open.internalName(), 0, open.targetsOrNull());
            }
            mv.visitEnd();
            cw.visitEnd();
            return cw.toByteArray();
        }

        /** Write {@code module-info.class} into {@code outputDirectory} and return that directory. */
        File writeTo(File outputDirectory) throws IOException {
            Files.createDirectories(outputDirectory.toPath());
            Files.write(new File(outputDirectory, "module-info.class").toPath(), toBytes());
            return outputDirectory;
        }
    }

    private static final class Directive {
        private final String packageName;
        private final String[] targets;

        private Directive(String packageName, String[] targets) {
            this.packageName = packageName;
            this.targets = targets;
        }

        String internalName() {
            return packageName.replace('.', '/');
        }

        /** ASM treats {@code null} (not an empty array) as an unqualified directive. */
        String[] targetsOrNull() {
            return targets.length == 0 ? null : targets;
        }
    }
}
