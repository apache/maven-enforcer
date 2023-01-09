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
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the "require files exist" rule.
 *
 * @author <a href="brett@apache.org">Brett Porter</a>
 */
class TestRequireFilesExist {
    @TempDir
    public File temporaryFolder;

    private final RequireFilesExist rule = new RequireFilesExist();

    @Test
    void testFileExists() throws Exception {
        File f = File.createTempFile("junit", null, temporaryFolder);

        rule.setFilesList(Collections.singletonList(f.getCanonicalFile()));

        rule.execute();
    }

    @Test
    void testFileOsIndependentExists() {
        rule.setFilesList(Collections.singletonList(new File("POM.xml")));

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, () -> rule.execute());

        assertNotNull(e.getMessage());
    }

    @Test
    void testEmptyFile() {
        rule.setFilesList(Collections.singletonList(null));

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, () -> rule.execute());

        assertNotNull(e.getMessage());
    }

    @Test
    void testEmptyFileAllowNull() throws Exception {
        rule.setFilesList(Collections.singletonList(null));
        rule.setAllowNulls(true);
        rule.execute();
    }

    @Test
    void testEmptyFileList() {
        rule.setFilesList(Collections.emptyList());
        assertTrue(rule.getFiles().isEmpty());

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, () -> rule.execute());

        assertNotNull(e.getMessage());
    }

    @Test
    void testEmptyFileListAllowNull() throws Exception {
        rule.setFilesList(Collections.emptyList());
        assertTrue(rule.getFiles().isEmpty());
        rule.setAllowNulls(true);
        rule.execute();
    }

    @Test
    void testFileDoesNotExist() throws Exception {
        File f = File.createTempFile("junit", null, temporaryFolder);
        f.delete();

        assertFalse(f.exists());
        rule.setFilesList(Collections.singletonList(f));

        EnforcerRuleException e = assertThrows(EnforcerRuleException.class, () -> rule.execute());

        assertNotNull(e.getMessage());
    }

    @Test
    void testFileExistsSatisfyAny() throws EnforcerRuleException, IOException {
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
