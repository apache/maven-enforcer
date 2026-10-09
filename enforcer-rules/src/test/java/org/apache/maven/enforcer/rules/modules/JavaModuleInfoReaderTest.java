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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Reads module-info via java.lang.module.ModuleDescriptor, which exists only on Java 9+.
@EnabledForJreRange(min = JRE.JAVA_9)
class JavaModuleInfoReaderTest {

    /** Build a real {@code module-info.class} with ASM (test scope only). */
    private static byte[] moduleInfo() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null);
        ModuleVisitor mv = cw.visitModule("com.example.foo", 0, null);
        mv.visitRequire("java.base", Opcodes.ACC_MANDATED, null);
        mv.visitRequire("com.example.bar", 0, null);
        mv.visitExport("com/example/foo/api", 0); // unqualified
        mv.visitExport("com/example/foo/internal", 0, "com.example.bar"); // qualified
        mv.visitOpen("com/example/foo/impl", 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static List<String> packages(List<JavaModuleInfo.Directive> directives) {
        List<String> names = new ArrayList<String>();
        for (JavaModuleInfo.Directive d : directives) {
            names.add(d.packageName());
        }
        return names;
    }

    @Test
    void readsModuleNameRequiresExportsAndOpens() throws Exception {
        JavaModuleInfo m = JavaModuleInfoReader.read(new ByteArrayInputStream(moduleInfo()));

        assertNotNull(m);
        assertEquals("com.example.foo", m.name());

        assertTrue(m.requires().contains("java.base"));
        assertTrue(m.requires().contains("com.example.bar"));

        List<String> exported = packages(m.exports());
        assertTrue(exported.contains("com.example.foo.api"), "expected unqualified export");
        assertTrue(exported.contains("com.example.foo.internal"), "expected qualified export");

        assertEquals(1, m.opens().size());
        assertEquals("com.example.foo.impl", m.opens().get(0).packageName());
    }

    @Test
    void distinguishesQualifiedFromUnqualifiedExports() throws Exception {
        JavaModuleInfo m = JavaModuleInfoReader.read(new ByteArrayInputStream(moduleInfo()));

        for (JavaModuleInfo.Directive d : m.exports()) {
            if (d.packageName().equals("com.example.foo.api")) {
                assertTrue(!d.isQualified(), "api export must be unqualified");
            } else if (d.packageName().equals("com.example.foo.internal")) {
                assertTrue(d.isQualified(), "internal export must be qualified");
                assertEquals("com.example.bar", d.targets().get(0));
            }
        }
    }

    @Test
    void returnsNullForPlainClassWithoutModuleAttribute() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Plain", null, "java/lang/Object", null);
        cw.visitEnd();
        assertNull(JavaModuleInfoReader.read(new ByteArrayInputStream(cw.toByteArray())));
    }

    @Test
    void readsTheOpenModuleFlag() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null);
        ModuleVisitor mv = cw.visitModule("com.example.foo", Opcodes.ACC_OPEN, null);
        mv.visitRequire("java.base", Opcodes.ACC_MANDATED, null);
        mv.visitEnd();
        cw.visitEnd();

        JavaModuleInfo open = JavaModuleInfoReader.read(new ByteArrayInputStream(cw.toByteArray()));
        assertNotNull(open);
        assertTrue(open.isOpen(), "expected an open module");

        assertFalse(JavaModuleInfoReader.read(new ByteArrayInputStream(moduleInfo()))
                .isOpen());
    }

    @Test
    void failsClearlyForAClassFileNewerThanTheRuntime() {
        byte[] classFile = moduleInfo();
        // bump the class-file major version to one beyond this JVM so ModuleDescriptor.read cannot parse it
        int runtimeMajor =
                Integer.parseInt(System.getProperty("java.class.version").split("\\.")[0]);
        int tooNew = runtimeMajor + 1;
        classFile[6] = (byte) ((tooNew >> 8) & 0xFF);
        classFile[7] = (byte) (tooNew & 0xFF);

        IOException e =
                assertThrows(IOException.class, () -> JavaModuleInfoReader.read(new ByteArrayInputStream(classFile)));
        assertTrue(e.getMessage().contains("newer than the JVM"), e.getMessage());
    }
}
