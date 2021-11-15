package org.apache.maven.plugins.enforcer.utils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.apache.maven.plugins.enforcer.utils.NormalizeLineSeparatorReader.LineSeparator;
import org.junit.jupiter.api.Test;

public class TestNormalizeLineSeparatorReader
{
    private final static String UNIX_MULTILINE_STRING = "line1\nline2\n\n";

    private final static String WINDOWS_MULTILINE_STRING = "line1\r\nline2\r\n\r\n";

    @Test
    public void testUnixToWindows()
        throws IOException
    {
        try ( Reader reader =
            new NormalizeLineSeparatorReader( new StringReader( UNIX_MULTILINE_STRING ), LineSeparator.WINDOWS ) )
        {
            assertEquals( WINDOWS_MULTILINE_STRING, IOUtils.toString( reader ) );
        }
    }

    @Test
    public void testUnixToUnix()
        throws IOException
    {
        try ( Reader reader =
            new NormalizeLineSeparatorReader( new StringReader( UNIX_MULTILINE_STRING ), LineSeparator.UNIX ) )
        {
            assertEquals( UNIX_MULTILINE_STRING, IOUtils.toString( reader ) );
        }
    }

    @Test
    public void testWindowsToUnix()
        throws IOException
    {
        try ( Reader reader =
            new NormalizeLineSeparatorReader( new StringReader( WINDOWS_MULTILINE_STRING ), LineSeparator.UNIX ) )
        {
            assertEquals( UNIX_MULTILINE_STRING, IOUtils.toString( reader ) );
        }
    }

    @Test
    public void testWindowsToWindows()
        throws IOException
    {
        try ( Reader reader =
            new NormalizeLineSeparatorReader( new StringReader( WINDOWS_MULTILINE_STRING ), LineSeparator.WINDOWS ) )
        {
            assertEquals( WINDOWS_MULTILINE_STRING, IOUtils.toString( reader ) );
        }
    }
}
