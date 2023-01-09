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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * Test the "require files exist" rule.
 *
 * @author <a href="brianf@apache.org">Brian Fox</a>
 */
@ExtendWith(MockitoExtension.class)
class TestRequireFilesSize {
    @TempDir
    public File temporaryFolder;

    @Mock
    private MavenProject project;

    @Mock
    private EnforcerLogger log;

    @InjectMocks
    private RequireFilesSize rule;

    @BeforeEach
    void setup() {
        rule.setLog(log);
    }

    @Test
    void testFileExists() throws EnforcerRuleException, IOException {
        File f = File.createTempFile("junit", null, temporaryFolder);

        rule.setFilesList(Collections.singletonList(f));

        rule.execute();
    }

    @Test
    void testEmptyFile() {
        rule.setFilesList(Collections.singletonList(null));
        try {
            rule.execute();
            fail("Should get exception");
        } catch (EnforcerRuleException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void testEmptyFileAllowNull() throws EnforcerRuleException {
        rule.setFilesList(Collections.singletonList(null));
        rule.setAllowNulls(true);
        rule.execute();
    }

    @Test
    void testEmptyFileList() throws EnforcerRuleException, IOException {
        rule.setFilesList(Collections.emptyList());

        assertTrue(rule.getFiles().isEmpty());

        File f = File.createTempFile("junit", null, temporaryFolder);

        ArtifactStubFactory factory = new ArtifactStubFactory();
        Artifact a = factory.getReleaseArtifact();
        a.setFile(f);

        when(project.getArtifact()).thenReturn(a);

        // sanity check the mockProject
        assertSame(f, project.getArtifact().getFile());

        rule.execute();
    }

    @Test
    void testFileDoesNotExist() throws IOException {
        File f = File.createTempFile("junit", null, temporaryFolder);
        f.delete();
        assertFalse(f.exists());
        rule.setFilesList(Collections.singletonList(f));

        try {
            rule.execute();
            fail("Should get exception");
        } catch (EnforcerRuleException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void testFileTooSmall() throws IOException {
        File f = File.createTempFile("junit", null, temporaryFolder);
        rule.setFilesList(Collections.singletonList(f));
        rule.setMinsize(10);
        try {
            rule.execute();
            fail("Should get exception");
        } catch (EnforcerRuleException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void testFileTooBig() throws IOException {
        File f = File.createTempFile("junit", null, temporaryFolder);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(f))) {
            out.write("123456789101112131415");
        }

        rule.setFilesList(Collections.singletonList(f));
        rule.setMaxsize(10);
        assertTrue(f.length() > 10);
        try {
            rule.execute();
            fail("Should get exception");
        } catch (EnforcerRuleException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void testRequireFilesSizeSatisfyAny() throws EnforcerRuleException, IOException {
        File f = File.createTempFile("junit", null, temporaryFolder);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(f))) {
            out.write("123456789101112131415");
        }
        assertTrue(f.length() > 10);

        File g = File.createTempFile("junit", null, temporaryFolder);

        rule.setFilesList(Arrays.asList(f, g));
        rule.setMaxsize(10);
        rule.setSatisfyAny(true);

        rule.execute();
    }

    /**
     * Test id.
     */
    @Test
    void testId() {
        assertNull(rule.getCacheId());
    }
}
