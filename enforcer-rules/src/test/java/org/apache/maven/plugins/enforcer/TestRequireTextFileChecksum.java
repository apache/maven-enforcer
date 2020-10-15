package org.apache.maven.plugins.enforcer;

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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugins.enforcer.utils.NormalizeLineSeparatorReader.LineSeparator;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * Test the "RequireTextFileChecksum" rule
 *
 */
public class TestRequireTextFileChecksum
{

    private RequireTextFileChecksum rule = new RequireTextFileChecksum();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void testFileChecksumMd5NormalizedFromUnixToWindows()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "line1\nline2\n" );

        rule.setFile( f );
        rule.setChecksum( "c6242222cf6ccdb15a43e0e5b1a08810" );
        rule.setType( "md5" );
        rule.setNormalizeLineSeparatorTo( LineSeparator.WINDOWS );
        rule.setEncoding( StandardCharsets.US_ASCII.name() );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5NormalizedFromWindowsToWindows()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "line1\r\nline2\r\n" );

        rule.setFile( f );
        rule.setChecksum( "c6242222cf6ccdb15a43e0e5b1a08810" );
        rule.setType( "md5" );
        rule.setNormalizeLineSeparatorTo( LineSeparator.WINDOWS );
        rule.setEncoding( StandardCharsets.US_ASCII.name() );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5NormalizedFromWindowsToUnix()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "line1\r\nline2\r\n" );

        rule.setFile( f );
        rule.setChecksum( "4fcc82a88ee38e0aa16c17f512c685c9" );
        rule.setType( "md5" );
        rule.setNormalizeLineSeparatorTo( LineSeparator.UNIX );
        rule.setEncoding( StandardCharsets.US_ASCII.name() );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5NormalizedFromUnixToUnix()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "line1\nline2\n" );

        rule.setFile( f );
        rule.setChecksum( "4fcc82a88ee38e0aa16c17f512c685c9" );
        rule.setType( "md5" );
        rule.setNormalizeLineSeparatorTo( LineSeparator.UNIX );
        rule.setEncoding( StandardCharsets.US_ASCII.name() );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5NormalizedWithMissingFileCharsetPaameter()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "line1\nline2\n" );

        expectedException.expect( EnforcerRuleException.class );
        expectedException.expectMessage( "No encoding set for '${project.build.sourceEncoding}'" );

        rule.setFile( f );
        rule.setChecksum( "4fcc82a88ee38e0aa16c17f512c685c9" );
        rule.setType( "md5" );
        rule.setNormalizeLineSeparatorTo( LineSeparator.UNIX );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

}
