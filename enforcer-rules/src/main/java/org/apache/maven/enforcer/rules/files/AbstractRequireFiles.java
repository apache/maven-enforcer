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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;

/**
 * Contains the common code to compare an array of files against a requirement.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
abstract class AbstractRequireFiles extends AbstractStandardEnforcerRule {

    /** List of files to check. */
    private List<File> files = Collections.emptyList();

    /** if null file handles should be allowed. If they are allowed, it means treat it as a success. */
    private boolean allowNulls = false;

    /** Allow that a single one of the files can make the rule to pass. */
    private boolean satisfyAny;

    // check the file for the specific condition
    /**
     * Check one file.
     *
     * @param file the file
     * @return <code>true</code> if successful
     */
    abstract boolean checkFile(File file);

    // return standard error message
    /**
     * Gets the error msg.
     *
     * @return the error msg
     */
    abstract String getErrorMsg();

    @Override
    public void execute() throws EnforcerRuleException {

        if (!allowNulls && files.isEmpty()) {
            throw new EnforcerRuleError("The file list is empty and Null files are disabled.");
        }

        List<File> failures = new ArrayList<>();
        for (File file : files) {
            if (!allowNulls && file == null) {
                failures.add(file);
            } else if (!checkFile(file)) {
                failures.add(file);
            }
        }

        if (satisfyAny) {
            int passed = files.size() - failures.size();
            if (passed == 0) {
                fail(failures);
            }
        }
        // if anything was found, log it with the optional message.
        else if (!failures.isEmpty()) {
            fail(failures);
        }
    }

    private void fail(List<File> failures) throws EnforcerRuleException {
        String message = getMessage();

        StringBuilder buf = new StringBuilder();
        if (message != null) {
            buf.append(message + System.lineSeparator());
        }
        buf.append(getErrorMsg());

        for (File file : failures) {
            if (file != null) {
                buf.append(file.getAbsolutePath() + System.lineSeparator());
            } else {
                buf.append("(an empty filename was given and allowNulls is false)" + System.lineSeparator());
            }
        }

        throw new EnforcerRuleException(buf.toString());
    }

    @Override
    public String getCacheId() {
        return Integer.toString(files.hashCode());
    }

    void setFilesList(List<File> files) {
        this.files = files;
    }

    // method using for testing purpose ...

    List<File> getFiles() {
        return files;
    }

    void setAllowNulls(boolean allowNulls) {
        this.allowNulls = allowNulls;
    }

    void setSatisfyAny(boolean satisfyAny) {
        this.satisfyAny = satisfyAny;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[message=%s, files=%s, allowNulls=%b, satisfyAny=%b]",
                getClass().getSimpleName(), getMessage(), files, allowNulls, satisfyAny);
    }
}
