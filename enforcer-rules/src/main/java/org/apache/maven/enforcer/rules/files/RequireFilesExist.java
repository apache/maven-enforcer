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

import javax.inject.Named;

import java.io.File;
import java.io.IOException;

/**
 * The Class RequireFilesExist.
 */
@Named("requireFilesExist")
public final class RequireFilesExist extends AbstractRequireFiles {
    @Override
    boolean checkFile(File file) {
        // if we get here and the handle is null, treat it as a success
        return file == null ? true : file.exists() && osIndependentNameMatch(file, true);
    }

    @Override
    String getErrorMsg() {
        return "Some required files are missing:" + System.lineSeparator();
    }

    /**
     * OSes like Windows are case insensitive, so this method will compare the file path with the actual path. A simple
     * {@link File#exists()} is not enough for such OS.
     *
     * @param file the file to verify
     * @param defaultValue value to return in case an IO exception occurs, should never happen as the file already
     *            exists
     * @return
     */
    private boolean osIndependentNameMatch(File file, boolean defaultValue) {
        try {
            File absFile;
            if (!file.isAbsolute()) {
                absFile = new File(new File(".").getCanonicalFile(), file.getPath());
            } else {
                absFile = file;
            }

            return absFile.toURI().equals(absFile.getCanonicalFile().toURI());
        } catch (IOException e) {
            return defaultValue;
        }
    }
}
