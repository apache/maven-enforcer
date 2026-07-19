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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Reads the {@code Module} attribute of a {@code module-info.class} (name, {@code requires},
 * {@code exports}, {@code opens}) by delegating to {@link java.lang.module.ModuleDescriptor}.
 *
 * <p><b>Design decision.</b> This plugin compiles with {@code --release 8}, so it cannot
 * reference {@code java.lang.module.ModuleDescriptor} (a Java&nbsp;9 API) directly. The obvious
 * alternative — a multi-release JAR overlay ({@code src/main/java9}) so the module-reading code
 * could be compiled for Java&nbsp;9 — is deliberately <em>not</em> used: multi-release JAR support
 * in Maven&nbsp;3 is incomplete and can even produce invalid JARs (cf. MNG-6892 / MNG-6293 and
 * {@code maven-jar-plugin#484}); it is only cleanly solved in Maven&nbsp;4 (POM model 4.1.0 +
 * {@code maven-compiler-plugin} 4.0.0-beta-3). To keep these rules usable on <b>Maven&nbsp;3</b>
 * and a Java&nbsp;8 source baseline, we instead access {@code ModuleDescriptor} <b>reflectively</b>
 * through this small wrapper class: the API is present at runtime whenever a
 * {@code module-info.class} exists (such a project is necessarily built on Java&nbsp;9+), and the
 * rules simply do nothing when there is no module descriptor. See {@code apache/maven-enforcer#995}.
 */
final class JavaModuleInfoReader {

    private static final String MODULE_DESCRIPTOR = "java.lang.module.ModuleDescriptor";
    private static final String INVALID_DESCRIPTOR = "java.lang.module.InvalidModuleDescriptorException";

    private JavaModuleInfoReader() {}

    /**
     * Parse a {@code module-info.class}.
     *
     * @param in the class-file bytes of a {@code module-info.class}
     * @return the parsed module info, or {@code null} if the bytes are not a valid module descriptor
     * @throws IOException if the bytes cannot be read, or if {@code java.lang.module} is unavailable
     *                     (i.e. running on a Java&nbsp;8 runtime)
     */
    static JavaModuleInfo read(InputStream in) throws IOException {
        byte[] classFile = readAllBytes(in);
        // ModuleDescriptor.read cannot parse a class file newer than the running JVM; without this
        // check it would throw InvalidModuleDescriptorException and we would wrongly treat the module
        // as "not present". Surface a clear diagnostic instead.
        checkReadableVersion(classFile);
        try {
            Class<?> descriptorType = Class.forName(MODULE_DESCRIPTOR);
            Object descriptor = descriptorType
                    .getMethod("read", InputStream.class)
                    .invoke(null, new ByteArrayInputStream(classFile));

            String name = (String) descriptorType.getMethod("name").invoke(descriptor);
            boolean open = (Boolean) descriptorType.getMethod("isOpen").invoke(descriptor);
            List<String> requires = requireNames(descriptor, descriptorType);
            List<JavaModuleInfo.Directive> exports = directives(descriptor, descriptorType, "exports", "Exports");
            List<JavaModuleInfo.Directive> opens = directives(descriptor, descriptorType, "opens", "Opens");
            return new JavaModuleInfo(name, open, requires, exports, opens);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null && INVALID_DESCRIPTOR.equals(cause.getClass().getName())) {
                return null; // not a valid module-info.class
            }
            throw new IOException("Could not read module descriptor", cause != null ? cause : e);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IOException(
                    "java.lang.module is not available; reading module-info requires a Java 9+ runtime", e);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Could not read module descriptor", e);
        }
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    /**
     * Fail fast if the {@code module-info.class} was built for a newer Java release than the JVM
     * running the enforcer, in which case {@code ModuleDescriptor.read} cannot parse it.
     */
    private static void checkReadableVersion(byte[] classFile) throws IOException {
        if (classFile.length < 8
                || (classFile[0] & 0xFF) != 0xCA
                || (classFile[1] & 0xFF) != 0xFE
                || (classFile[2] & 0xFF) != 0xBA
                || (classFile[3] & 0xFF) != 0xBE) {
            throw new IOException("Not a Java class file (bad magic)");
        }
        int major = ((classFile[6] & 0xFF) << 8) | (classFile[7] & 0xFF);
        int runtimeMajor = runtimeClassFileMajor();
        if (major > runtimeMajor) {
            throw new IOException("module-info.class has class file version " + major + " (Java " + (major - 44)
                    + "), which is newer than the JVM running the enforcer (class file version " + runtimeMajor
                    + ", Java " + (runtimeMajor - 44) + "). Run the build on that Java release or newer.");
        }
    }

    /** The class-file major version of the running JVM (e.g. 52 for Java&nbsp;8, 69 for Java&nbsp;25). */
    private static int runtimeClassFileMajor() {
        // "java.class.version" is "52.0", "61.0", "69.0", ... — available since Java 1.1.
        String version = System.getProperty("java.class.version", "52.0");
        int dot = version.indexOf('.');
        return Integer.parseInt(dot >= 0 ? version.substring(0, dot) : version);
    }

    private static List<String> requireNames(Object descriptor, Class<?> descriptorType)
            throws ReflectiveOperationException {
        Set<?> requires = (Set<?>) descriptorType.getMethod("requires").invoke(descriptor);
        Method name = Class.forName(MODULE_DESCRIPTOR + "$Requires").getMethod("name");
        List<String> result = new ArrayList<String>(requires.size());
        for (Object require : requires) {
            result.add((String) name.invoke(require));
        }
        // ModuleDescriptor#requires() is a Set: sort for stable diagnostics across JDKs
        Collections.sort(result);
        return result;
    }

    private static List<JavaModuleInfo.Directive> directives(
            Object descriptor, Class<?> descriptorType, String accessor, String innerType)
            throws ReflectiveOperationException {
        Set<?> entries = (Set<?>) descriptorType.getMethod(accessor).invoke(descriptor);
        Class<?> entryType = Class.forName(MODULE_DESCRIPTOR + "$" + innerType);
        Method source = entryType.getMethod("source");
        Method targets = entryType.getMethod("targets");
        List<JavaModuleInfo.Directive> result = new ArrayList<JavaModuleInfo.Directive>(entries.size());
        for (Object entry : entries) {
            String pkg = (String) source.invoke(entry);
            Set<?> to = (Set<?>) targets.invoke(entry); // empty for an unqualified directive
            List<String> moduleTargets = new ArrayList<String>(to.size());
            for (Object target : to) {
                moduleTargets.add((String) target);
            }
            // targets and directives come from Sets: sort for stable diagnostics across JDKs
            Collections.sort(moduleTargets);
            result.add(new JavaModuleInfo.Directive(pkg, moduleTargets));
        }
        result.sort(Comparator.comparing(JavaModuleInfo.Directive::packageName));
        return result;
    }
}
