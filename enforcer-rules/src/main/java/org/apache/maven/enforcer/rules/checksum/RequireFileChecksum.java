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

import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;

/**
 * Rule to validate a binary file to match the specified checksum.
 *
 * @author Edward Samson
 * @author Lyubomyr Shaydariv
 * @see RequireTextFileChecksum
 */
@Named("requireFileChecksum")
public class RequireFileChecksum extends AbstractStandardEnforcerRule {

    private File file;

    private String checksum;

    private String type;

    private String nonexistentFileMessage;

    @Override
    public void execute() throws EnforcerRuleException {
        if (this.file == null) {
            throw new EnforcerRuleError("Input file unspecified");
        }

        if (this.type == null) {
            throw new EnforcerRuleError("Hash type unspecified");
        }

        if (this.checksum == null) {
            throw new EnforcerRuleError("Checksum unspecified");
        }

        if (!this.file.exists()) {
            String message = nonexistentFileMessage;
            if (message == null) {
                message = "File does not exist: " + this.file.getAbsolutePath();
            }
            throw new EnforcerRuleException(message);
        }

        if (this.file.isDirectory()) {
            throw new EnforcerRuleError("Cannot calculate the checksum of directory: " + this.file.getAbsolutePath());
        }

        if (!this.file.canRead()) {
            throw new EnforcerRuleError("Cannot read file: " + this.file.getAbsolutePath());
        }

        String checksum = calculateChecksum();

        if (!checksum.equalsIgnoreCase(this.checksum)) {
            String exceptionMessage = getMessage();
            if (exceptionMessage == null) {
                exceptionMessage =
                        this.type + " hash of " + this.file + " was " + checksum + " but expected " + this.checksum;
            }
            throw new EnforcerRuleException(exceptionMessage);
        }
    }

    /**
     * The file to check.
     *
     * @param file file
     */
    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    /**
     * The expected checksum value.
     *
     * @param checksum checksum
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getChecksum() {
        return checksum;
    }

    /**
     * The checksum algorithm to use. Possible values: "md5", "sha1", "sha256", "sha384", "sha512".
     *
     * @param type algorithm
     */
    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /**
     * The friendly message to use when the file does not exist.
     *
     * @param nonexistentFileMessage message
     */
    public void setNonexistentFileMessage(String nonexistentFileMessage) {
        this.nonexistentFileMessage = nonexistentFileMessage;
    }

    public String getNonexistentFileMessage() {
        return nonexistentFileMessage;
    }

    protected String calculateChecksum() throws EnforcerRuleException {
        try (InputStream inputStream = Files.newInputStream(this.file.toPath())) {
            return calculateChecksum(inputStream);
        } catch (IOException e) {
            throw new EnforcerRuleError("Unable to calculate checksum", e);
        }
    }

    protected String calculateChecksum(InputStream inputStream) throws IOException, EnforcerRuleException {
        String result;
        if ("md5".equals(this.type)) {
            result = DigestUtils.md5Hex(inputStream);
        } else if ("sha1".equals(this.type)) {
            result = DigestUtils.sha1Hex(inputStream);
        } else if ("sha256".equals(this.type)) {
            result = DigestUtils.sha256Hex(inputStream);
        } else if ("sha384".equals(this.type)) {
            result = DigestUtils.sha384Hex(inputStream);
        } else if ("sha512".equals(this.type)) {
            result = DigestUtils.sha512Hex(inputStream);
        } else {
            throw new EnforcerRuleError("Unsupported hash type: " + this.type);
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "RequireFileChecksum[message=%s, file=%s, checksum=%s, type=%s, nonexistentFileMessage=%s]",
                getMessage(), file, checksum, type, nonexistentFileMessage);
    }
}
