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
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Determines the module identity and package set of one dependency artifact (a JAR or an exploded
 * classes directory, e.g. a reactor sibling's {@code target/classes}).
 *
 * <p>Modular artifacts and automatic modules are read through
 * {@code java.lang.module.ModuleFinder}, which derives automatic-module names and always reports
 * the complete package set. Like {@link JavaModuleInfoReader}, the Java&nbsp;9 module API is
 * accessed reflectively to keep the Java&nbsp;8 compile baseline (see the design note there).
 * Artifacts the finder cannot model (no descriptor and no derivable automatic name, or running on
 * a Java&nbsp;8 JVM) fall back to a plain class-file scan and are reported as non-modular.</p>
 */
final class ArtifactModuleScanner {

    private ArtifactModuleScanner() {}

    /** The module view of one scanned artifact. */
    static final class ScannedArtifact {
        private final JavaModuleInfo moduleInfo;
        private final boolean automatic;
        private final Set<String> packages;

        private ScannedArtifact(JavaModuleInfo moduleInfo, boolean automatic, Set<String> packages) {
            this.moduleInfo = moduleInfo;
            this.automatic = automatic;
            this.packages = packages;
        }

        /**
         * The module descriptor ({@link JavaModuleInfo#packages()} filled from
         * {@code ModuleDescriptor.packages()}), or {@code null} for a non-modular artifact.
         */
        JavaModuleInfo moduleInfo() {
            return moduleInfo;
        }

        /** {@code true} if the descriptor was derived for an automatic module (plain JAR). */
        boolean isAutomatic() {
            return automatic;
        }

        /** All packages containing at least one class file. */
        Set<String> packages() {
            return packages;
        }
    }

    /**
     * Scan one artifact.
     *
     * @param file the artifact's JAR file or classes directory
     * @return the scanned module view, never {@code null}
     * @throws IOException if the artifact cannot be read at all
     */
    static ScannedArtifact scan(File file) throws IOException {
        // A directory without module-info.class must not go through ModuleFinder: the finder would
        // treat it as a *directory of modules* and scan its entries, yielding nonsense.
        boolean tryFinder = file.isFile() || new File(file, "module-info.class").isFile();
        if (tryFinder && isModuleApiAvailable()) {
            try {
                ScannedArtifact scanned = scanWithModuleFinder(file);
                if (scanned != null) {
                    return scanned;
                }
            } catch (InvocationTargetException e) {
                // e.g. FindException for a JAR whose file name yields no valid automatic-module
                // name — legal on the classpath, so degrade to a non-modular scan.
            } catch (ReflectiveOperationException e) {
                throw new IOException("Could not read module information of " + file, e);
            }
        }
        return new ScannedArtifact(null, false, scanPackages(file));
    }

    private static boolean isModuleApiAvailable() {
        try {
            Class.forName("java.lang.module.ModuleFinder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Read the artifact through {@code ModuleFinder.of(path)}; {@code null} if it finds no module. */
    private static ScannedArtifact scanWithModuleFinder(File file) throws ReflectiveOperationException {
        Class<?> finderType = Class.forName("java.lang.module.ModuleFinder");
        Object pathArray = Array.newInstance(Path.class, 1);
        Array.set(pathArray, 0, file.toPath());
        // wrap: a bare Path[] argument would be unpacked by Method.invoke as the whole varargs list
        Object finder = finderType.getMethod("of", Path[].class).invoke(null, new Object[] {pathArray});
        Set<?> references = (Set<?>) finderType.getMethod("findAll").invoke(finder);
        if (references.size() != 1) {
            return null;
        }
        Object reference = references.iterator().next();
        Object descriptor = Class.forName("java.lang.module.ModuleReference")
                .getMethod("descriptor")
                .invoke(reference);
        boolean automatic = (Boolean) Class.forName("java.lang.module.ModuleDescriptor")
                .getMethod("isAutomatic")
                .invoke(descriptor);
        JavaModuleInfo moduleInfo = JavaModuleInfoReader.fromDescriptor(descriptor);
        return new ScannedArtifact(moduleInfo, automatic, moduleInfo.packages());
    }

    /** Plain scan: every package that holds at least one {@code .class} file. */
    private static Set<String> scanPackages(File file) throws IOException {
        Set<String> packages = new TreeSet<String>();
        if (file.isDirectory()) {
            collectPackages(file, "", packages);
        } else if (file.isFile()) {
            try (JarFile jar = new JarFile(file)) {
                for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    int slash = name.lastIndexOf('/');
                    if (slash > 0 && name.endsWith(".class") && !name.startsWith("META-INF/")) {
                        packages.add(name.substring(0, slash).replace('/', '.'));
                    }
                }
            }
        }
        return packages;
    }

    private static void collectPackages(File directory, String packageName, Set<String> packages) {
        File[] entries = directory.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (entry.isDirectory()) {
                String childPackage = packageName.isEmpty() ? entry.getName() : packageName + "." + entry.getName();
                collectPackages(entry, childPackage, packages);
            } else if (!packageName.isEmpty() && entry.getName().endsWith(".class")) {
                packages.add(packageName);
            }
        }
    }
}
