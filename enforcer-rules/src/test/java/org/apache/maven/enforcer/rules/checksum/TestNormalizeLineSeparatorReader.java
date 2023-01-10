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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.apache.maven.enforcer.rules.checksum.NormalizeLineSeparatorReader.LineSeparator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestNormalizeLineSeparatorReader {
    private static final String UNIX_MULTILINE_STRING = "line1\nline2\n\n";

    private static final String WINDOWS_MULTILINE_STRING = "line1\r\nline2\r\n\r\n";

    @Test
    void testUnixToWindows() throws IOException {
        try (Reader reader =
                new NormalizeLineSeparatorReader(new StringReader(UNIX_MULTILINE_STRING), LineSeparator.WINDOWS)) {
            assertEquals(WINDOWS_MULTILINE_STRING, IOUtils.toString(reader));
        }
    }

    @Test
    void testUnixToUnix() throws IOException {
        try (Reader reader =
                new NormalizeLineSeparatorReader(new StringReader(UNIX_MULTILINE_STRING), LineSeparator.UNIX)) {
            assertEquals(UNIX_MULTILINE_STRING, IOUtils.toString(reader));
        }
    }

    @Test
    void testWindowsToUnix() throws IOException {
        try (Reader reader =
                new NormalizeLineSeparatorReader(new StringReader(WINDOWS_MULTILINE_STRING), LineSeparator.UNIX)) {
            assertEquals(UNIX_MULTILINE_STRING, IOUtils.toString(reader));
        }
    }

    @Test
    void testWindowsToWindows() throws IOException {
        try (Reader reader =
                new NormalizeLineSeparatorReader(new StringReader(WINDOWS_MULTILINE_STRING), LineSeparator.WINDOWS)) {
            assertEquals(WINDOWS_MULTILINE_STRING, IOUtils.toString(reader));
        }
    }
}
