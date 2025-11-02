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
package org.apache.maven.enforcer.rules.checksum;

import java.io.File;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the "RequireFileChecksum" rule
 *
 * @author Lyubomyr Shaydariv
 */
class TestRequireFileChecksum {

    private final RequireFileChecksum rule = new RequireFileChecksum();

    @TempDir
    private File temporaryFolder;

    @Test
    void fileChecksumMd5() throws Exception {
        File f = File.createTempFile("junit", null, temporaryFolder);
        FileUtils.fileWrite(f, "message");

        rule.setFile(f);
        rule.setChecksum("78e731027d8fd50ed642340b7c9a63b3");
        rule.setType("md5");

        rule.execute();
    }

    @Test
    void fileChecksumMd5UpperCase() throws Exception {
        File f = File.createTempFile("junit", null, temporaryFolder);
        FileUtils.fileWrite(f, "message");

        rule.setFile(f);
        rule.setChecksum("78E731027D8FD50ED642340B7C9A63B3");
        rule.setType("md5");

        rule.execute();
    }

    @Test
    void fileChecksumMd5GivenFileDoesNotExistFailure() {
        File f = new File("nonExistent");
        rule.setFile(f);
        rule.setChecksum("78e731027d8fd50ed642340b7c9a63b3");
        rule.setType("md5");
        Throwable exception = assertThrows(EnforcerRuleException.class, () ->

            rule.execute());
        assertTrue(exception.getMessage().contains("File does not exist: " + f.getAbsolutePath()));
    }

    @Test
    void fileChecksumMd5GivenFileDoesNotExistFailureWithMessage() {
        File f = new File("nonExistent");
        String configuredMessage = "testMessageFileDoesNotExist";
        rule.setFile(f);
        rule.setChecksum("78e731027d8fd50ed642340b7c9a63b3");
        rule.setType("md5");
        rule.setNonexistentFileMessage(configuredMessage);
        Throwable exception = assertThrows(EnforcerRuleException.class, () ->

            rule.execute());
        assertTrue(exception.getMessage().contains(configuredMessage));
    }

    @Test
    void fileChecksumMd5GivenFileIsNotReadableFailure() throws Exception {
        File t = File.createTempFile("junit", null, temporaryFolder);
        File f = new File(t.getAbsolutePath()) {
            private static final long serialVersionUID = 6987790643999338089L;

            @Override
            public boolean canRead() {
                return false;
            }
        };
        rule.setFile(f);
        rule.setChecksum("78e731027d8fd50ed642340b7c9a63b3");
        rule.setType("md5");
        Throwable exception = assertThrows(EnforcerRuleException.class, () ->

            rule.execute());
        assertTrue(exception.getMessage().contains("Cannot read file: " + f.getAbsolutePath()));
    }

    @Test
    void fileChecksumMd5GivenFileIsADirectoryFailure() {
        File f = temporaryFolder;
        rule.setFile(f);
        rule.setChecksum("78e731027d8fd50ed642340b7c9a63b3");
        rule.setType("md5");
        Throwable exception = assertThrows(EnforcerRuleException.class, () ->

            rule.execute());
        assertTrue(
                exception.getMessage().contains("Cannot calculate the checksum of directory: " + f.getAbsolutePath()));
    }

    @Test
    void fileChecksumMd5NoFileSpecifiedFailure() {
        rule.setChecksum("78e731027d8fd50ed642340b7c9a63b3");
        rule.setType("md5");
        Throwable exception = assertThrows(EnforcerRuleException.class, () ->

            rule.execute());
        assertTrue(exception.getMessage().contains("Input file unspecified"));
    }

    @Test
    void fileChecksumMd5NoChecksumSpecifiedFailure() {
        File f = File.createTempFile("junit", null, temporaryFolder);
        rule.setFile(f);
        rule.setType("md5");
        Throwable exception = assertThrows(EnforcerRuleException.class, () ->

            rule.execute());
        assertTrue(exception.getMessage().contains("Checksum unspecified"));
    }

    @Test
    void fileChecksumMd5NoTypeSpecifiedFailure() {
        File f = File.createTempFile("junit", null, temporaryFolder);
        rule.setFile(f);
        rule.setChecksum("78e731027d8fd50ed642340b7c9a63b3");
        Throwable exception = assertThrows(EnforcerRuleException.class, () ->

            rule.execute());
        assertTrue(exception.getMessage().contains("Hash type unspecified"));
    }

    @Test
    void fileChecksumMd5ChecksumMismatchFailure() throws Exception {
        File f = File.createTempFile("junit", null, temporaryFolder);
        FileUtils.fileWrite(f, "message");
        rule.setFile(f);
        rule.setChecksum("ffeeddccbbaa99887766554433221100");
        rule.setType("md5");
        Throwable exception = assertThrows(EnforcerRuleException.class, () ->

            rule.execute());
        assertTrue(exception
                .getMessage()
                .contains("md5 hash of " + f.getAbsolutePath()
                        + " was 78e731027d8fd50ed642340b7c9a63b3 but expected ffeeddccbbaa99887766554433221100"));
    }

    @Test
    void fileChecksumMd5ChecksumMismatchFailureWithMessage() {
        String configuredMessage = "testMessage";
        File f = File.createTempFile("junit", null, temporaryFolder);
        FileUtils.fileWrite(f, "message");
        rule.setFile(f);
        rule.setChecksum("ffeeddccbbaa99887766554433221100");
        rule.setType("md5");
        rule.setMessage(configuredMessage);
        Throwable exception = assertThrows(EnforcerRuleException.class, () ->

            rule.execute());
        assertTrue(exception.getMessage().contains(configuredMessage));
    }

    @Test
    void fileChecksumSha1() throws Exception {
        File f = File.createTempFile("junit", null, temporaryFolder);
        FileUtils.fileWrite(f, "message");

        rule.setFile(f);
        rule.setChecksum("6f9b9af3cd6e8b8a73c2cdced37fe9f59226e27d");
        rule.setType("sha1");

        rule.execute();
    }

    @Test
    void fileChecksumSha256() throws Exception {
        File f = File.createTempFile("junit", null, temporaryFolder);
        FileUtils.fileWrite(f, "message");

        rule.setFile(f);
        rule.setChecksum("ab530a13e45914982b79f9b7e3fba994cfd1f3fb22f71cea1afbf02b460c6d1d");
        rule.setType("sha256");

        rule.execute();
    }

    @Test
    void fileChecksumSha384() throws Exception {
        File f = File.createTempFile("junit", null, temporaryFolder);
        FileUtils.fileWrite(f, "message");

        rule.setFile(f);
        rule.setChecksum(
                "353eb7516a27ef92e96d1a319712d84b902eaa828819e53a8b09af7028103a9978ba8feb6161e33c3619c5da4c4666a5");
        rule.setType("sha384");

        rule.execute();
    }

    @Test
    void fileChecksumSha512() throws Exception {
        File f = File.createTempFile("junit", null, temporaryFolder);
        FileUtils.fileWrite(f, "message");

        rule.setFile(f);
        rule.setChecksum(
                "f8daf57a3347cc4d6b9d575b31fe6077e2cb487f60a96233c08cb479dbf31538cc915ec6d48bdbaa96ddc1a16db4f4f96f37276cfcb3510b8246241770d5952c");
        rule.setType("sha512");

        rule.execute();
    }
}
