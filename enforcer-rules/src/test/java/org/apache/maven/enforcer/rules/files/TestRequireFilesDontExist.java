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
package org.apache.maven.enforcer.rules.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Test the "require files don't exist" rule.
 *
 * @author <a href="brianf@apache.org">Brian Fox</a>
 */
class TestRequireFilesDontExist {
    @TempDir
    private File temporaryFolder;

    private final RequireFilesDontExist rule = new RequireFilesDontExist();

    @Test
    void testFileExists() throws IOException {
        File f = File.createTempFile("junit", null, temporaryFolder);

        rule.setFilesList(Collections.singletonList(f));

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);

        assertNotNull(e.getMessage());
        f.delete();
    }

    @Test
    void testFileOsIndependentDoesNotExist() throws EnforcerRuleException {
        // a file differing only in case is not the requested file, whatever the filesystem says
        rule.setFilesList(Collections.singletonList(new File("POM.xml")));

        rule.execute();
    }

    @Test
    void testEmptyFile() {
        rule.setFilesList(Collections.singletonList(null));

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);

        assertNotNull(e.getMessage());
    }

    @Test
    void testEmptyFileAllowNull() throws EnforcerRuleException {
        rule.setFilesList(Collections.singletonList(null));
        rule.setAllowNulls(true);
        rule.execute();
    }

    @Test
    void testEmptyFileList() {
        rule.setFilesList(Collections.emptyList());
        assertTrue(rule.getFiles().isEmpty());

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);

        assertNotNull(e.getMessage());
    }

    @Test
    void testEmptyFileListAllowNull() throws EnforcerRuleException {
        rule.setFilesList(Collections.emptyList());
        assertTrue(rule.getFiles().isEmpty());
        rule.setAllowNulls(true);
        rule.execute();
    }

    @Test
    void testDeletedFileDetected() throws EnforcerRuleException, IOException {
        File f = File.createTempFile("junit", null, temporaryFolder);
        rule.setFilesList(Collections.singletonList(f));

        // Check the file is detected as being present
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertNotNull(e.getMessage());

        f.delete();

        assumeFalse(f.exists());

        // Rule should now pass as the file was deleted
        rule.execute();
    }

    @Test
    void testSymbolicLinkDeletedDetected() throws Exception {
        File canonicalFile = File.createTempFile("canonical_", null, temporaryFolder);
        File linkFile = Files.createSymbolicLink(
                        Paths.get(temporaryFolder.getAbsolutePath(), "symbolic.link"),
                        Paths.get(canonicalFile.getAbsolutePath()))
                .toFile();

        rule.setFilesList(Collections.singletonList(linkFile));
        // Check the link is detected as being present
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertNotNull(e.getMessage());

        linkFile.delete();

        // Rule should now pass as the target was deleted
        rule.execute();
    }

    @Test
    void testSymbolicLinkTargetDeletedDetected() throws Exception {
        File canonicalFile = File.createTempFile("canonical_", null, temporaryFolder);
        File linkFile = Files.createSymbolicLink(
                        Paths.get(temporaryFolder.getAbsolutePath(), "symbolic.link"),
                        Paths.get(canonicalFile.getAbsolutePath()))
                .toFile();
        rule.setFilesList(Collections.singletonList(linkFile));

        // Check the target is detected as being present
        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, rule::execute);
        assertNotNull(e.getMessage());

        canonicalFile.delete();

        // Rule should now pass as the target was deleted
        rule.execute();
    }

    @Test
    void testDeletedFileDetectedSatisfyAny() throws EnforcerRuleException, IOException {
        File f = File.createTempFile("junit", null, temporaryFolder);
        f.delete();

        assertFalse(f.exists());

        File g = File.createTempFile("junit", null, temporaryFolder);

        assertTrue(g.exists());

        rule.setFilesList(Arrays.asList(f, g.getCanonicalFile()));
        rule.setSatisfyAny(true);

        rule.execute();
    }

    /**
     * Test id.
     */
    @Test
    void testId() {
        assertNotNull(rule.getCacheId());
    }
}
