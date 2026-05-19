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

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.util.Collections;
import java.util.Objects;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

/**
 * Rule to validate the main artifact is within certain size constraints.
 *
 * @author brianf
 * @author Roman Stumm
 */
@Named("requireFilesSize")
public final class RequireFilesSize extends AbstractRequireFiles {

    private static final long MAXSIZE = 10000;

    /** the max size allowed. */
    private long maxsize = MAXSIZE;

    /** the min size allowed. */
    private long minsize = 0;

    /** the mode for computing the size when the files are directories. */
    private boolean recursive = false;

    /** The error msg. */
    private String errorMsg;

    private final MavenProject project;

    @Inject
    public RequireFilesSize(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    @Override
    public void execute() throws EnforcerRuleException {
        // if the file is already defined, use that. Otherwise, get the main artifact.
        if (getFiles().isEmpty()) {
            setFilesList(Collections.singletonList(project.getArtifact().getFile()));
            super.execute();
        } else {
            super.execute();
        }
    }

    @Override
    public String getCacheId() {
        // non cached rule - return null
        return null;
    }

    @Override
    String perFileMessage(File file) {
        return "";
    }

    @Override
    boolean checkFile(File file) {
        if (file == null) {
            // if we get here and it's null, treat it as a success.
            return true;
        }

        // check the file now
        if (file.exists()) {
            long length = computeLength(file);
            if (length < minsize) {
                this.errorMsg = (file + " size (" + length + ") too small. Minimum is " + minsize + ".");
                return false;
            } else if (length > maxsize) {
                this.errorMsg = (file + " size (" + length + ") too large. Maximum is " + maxsize + ".");
                return false;
            } else {

                getLog().debug(() -> file
                        + " size ("
                        + length
                        + ") is OK ("
                        + (minsize == maxsize || minsize == 0
                                ? ("maximum " + maxsize)
                                : ("between " + minsize + " and " + maxsize))
                        + " bytes).");

                return true;
            }
        } else {
            this.errorMsg = (file + " does not exist.");
            return false;
        }
    }

    private long computeLength(File file) {
        File[] files = file.listFiles();
        long length = file.length();
        if (files == null || !recursive) {
            return length;
        }
        for (File child : files) {
            length += computeLength(child);
        }
        return length;
    }

    @Override
    String getErrorMsg() {
        return this.errorMsg;
    }

    public void setMaxsize(long maxsize) {
        this.maxsize = maxsize;
    }

    public void setMinsize(long minsize) {
        this.minsize = minsize;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }
}
