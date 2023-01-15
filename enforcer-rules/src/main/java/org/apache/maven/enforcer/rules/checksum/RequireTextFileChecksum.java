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

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.checksum.NormalizeLineSeparatorReader.LineSeparator;
import org.apache.maven.project.MavenProject;

/**
 * Rule to validate a text file to match the specified checksum.
 *
 * @author Konrad Windszus
 * @see RequireFileChecksum
 */
@Named("requireTextFileChecksum")
public final class RequireTextFileChecksum extends RequireFileChecksum {

    private NormalizeLineSeparatorReader.LineSeparator normalizeLineSeparatorTo = LineSeparator.UNIX;

    private Charset encoding;

    private final MavenProject project;

    @Inject
    public RequireTextFileChecksum(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    public void setNormalizeLineSeparatorTo(NormalizeLineSeparatorReader.LineSeparator normalizeLineSeparatorTo) {
        this.normalizeLineSeparatorTo = normalizeLineSeparatorTo;
    }

    public void setEncoding(String encoding) {
        this.encoding = Charset.forName(encoding);
    }

    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        // set defaults
        if (encoding == null) {
            // https://maven.apache.org/plugins/maven-resources-plugin/examples/encoding.html
            String projectEncoding = project.getProperties().getProperty("project.build.sourceEncoding", null);
            if (StringUtils.isBlank(projectEncoding)) {
                projectEncoding = System.getProperty("file.encoding");
                getLog().warn("File encoding has not been set, using platform encoding " + projectEncoding
                        + ". Build is platform dependent! - https://maven.apache.org/general.html#encoding-warning");
            }
            encoding = Charset.forName(projectEncoding);
        }
        super.execute();
    }

    @Override
    protected String calculateChecksum() throws EnforcerRuleException {
        try (Reader reader = new NormalizeLineSeparatorReader(
                        Files.newBufferedReader(getFile().toPath(), encoding), normalizeLineSeparatorTo);
                InputStream inputStream = new ReaderInputStream(reader, encoding)) {
            return super.calculateChecksum(inputStream);
        } catch (IOException e) {
            throw new EnforcerRuleError("Unable to calculate checksum (with normalized line separators)", e);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "RequireFileChecksum[message=%s, file=%s, checksum=%s, type=%s, encoding=%s, normalizeLineSeparatorTo=%s, nonexistentFileMessage=%s, level=%s]",
                getMessage(),
                getFile(),
                getChecksum(),
                getType(),
                encoding,
                normalizeLineSeparatorTo,
                getNonexistentFileMessage(),
                getLevel());
    }
}
