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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * Test the "RequireFileChecksum" rule
 *
 * @author Lyubomyr Shaydariv
 */
public class TestRequireFileChecksum
{

    private RequireFileChecksum rule = new RequireFileChecksum();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testFileChecksumMd5()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "message" );

        rule.setFile( f );
        rule.setChecksum( "78e731027d8fd50ed642340b7c9a63b3" );
        rule.setType( "md5" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5UpperCase()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "message" );

        rule.setFile( f );
        rule.setChecksum( "78E731027D8FD50ED642340B7C9A63B3" );
        rule.setType( "md5" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5GivenFileDoesNotExistFailure()
        throws IOException, EnforcerRuleException
    {
        File f = new File( "nonExistent" );

        expectedException.expect( EnforcerRuleException.class );
        expectedException.expectMessage( "File does not exist: " + f.getAbsolutePath() );

        rule.setFile( f );
        rule.setChecksum( "78e731027d8fd50ed642340b7c9a63b3" );
        rule.setType( "md5" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5GivenFileDoesNotExistFailureWithMessage()
        throws IOException, EnforcerRuleException
    {
        File f = new File( "nonExistent" );
        String configuredMessage = "testMessageFileDoesNotExist";

        expectedException.expect( EnforcerRuleException.class );
        expectedException.expectMessage( configuredMessage );

        rule.setFile( f );
        rule.setChecksum( "78e731027d8fd50ed642340b7c9a63b3" );
        rule.setType( "md5" );
        rule.setNonexistentFileMessage( configuredMessage );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5GivenFileIsNotReadableFailure()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        f = new File( f.getAbsolutePath() )
        {
            private static final long serialVersionUID = 6987790643999338089L;

            @Override
            public boolean canRead()
            {
                return false;
            }
        };

        expectedException.expect( EnforcerRuleException.class );
        expectedException.expectMessage( "Cannot read file: " + f.getAbsolutePath() );

        rule.setFile( f );
        rule.setChecksum( "78e731027d8fd50ed642340b7c9a63b3" );
        rule.setType( "md5" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5GivenFileIsADirectoryFailure()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFolder();

        expectedException.expect( EnforcerRuleException.class );
        expectedException.expectMessage( "Cannot calculate the checksum of directory: " + f.getAbsolutePath() );

        rule.setFile( f );
        rule.setChecksum( "78e731027d8fd50ed642340b7c9a63b3" );
        rule.setType( "md5" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5NoFileSpecifiedFailure()
        throws IOException, EnforcerRuleException
    {
        expectedException.expect( EnforcerRuleException.class );
        expectedException.expectMessage( "Input file unspecified" );

        rule.setChecksum( "78e731027d8fd50ed642340b7c9a63b3" );
        rule.setType( "md5" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5NoChecksumSpecifiedFailure()
        throws IOException, EnforcerRuleException
    {
        expectedException.expect( EnforcerRuleException.class );
        expectedException.expectMessage( "Checksum unspecified" );

        File f = temporaryFolder.newFile();

        rule.setFile( f );
        rule.setType( "md5" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5NoTypeSpecifiedFailure()
        throws IOException, EnforcerRuleException
    {
        expectedException.expect( EnforcerRuleException.class );
        expectedException.expectMessage( "Hash type unspecified" );

        File f = temporaryFolder.newFile();

        rule.setFile( f );
        rule.setChecksum( "78e731027d8fd50ed642340b7c9a63b3" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5ChecksumMismatchFailure()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "message" );

        expectedException.expect( EnforcerRuleException.class );
        expectedException.expectMessage( "md5 hash of " + f.getAbsolutePath()
            + " was 78e731027d8fd50ed642340b7c9a63b3 but expected ffeeddccbbaa99887766554433221100" );

        rule.setFile( f );
        rule.setChecksum( "ffeeddccbbaa99887766554433221100" );
        rule.setType( "md5" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumMd5ChecksumMismatchFailureWithMessage()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "message" );
        String configuredMessage = "testMessage";

        expectedException.expect( EnforcerRuleException.class );
        expectedException.expectMessage( configuredMessage );

        rule.setFile( f );
        rule.setChecksum( "ffeeddccbbaa99887766554433221100" );
        rule.setType( "md5" );
        rule.setMessage( configuredMessage );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumSha1()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "message" );

        rule.setFile( f );
        rule.setChecksum( "6f9b9af3cd6e8b8a73c2cdced37fe9f59226e27d" );
        rule.setType( "sha1" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumSha256()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "message" );

        rule.setFile( f );
        rule.setChecksum( "ab530a13e45914982b79f9b7e3fba994cfd1f3fb22f71cea1afbf02b460c6d1d" );
        rule.setType( "sha256" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumSha384()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "message" );

        rule.setFile( f );
        rule.setChecksum( "353eb7516a27ef92e96d1a319712d84b902eaa828819e53a8b09af7028103a9978ba8feb6161e33c3619c5da4c4666a5" );
        rule.setType( "sha384" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileChecksumSha512()
        throws IOException, EnforcerRuleException
    {
        File f = temporaryFolder.newFile();
        FileUtils.fileWrite( f, "message" );

        rule.setFile( f );
        rule.setChecksum( "f8daf57a3347cc4d6b9d575b31fe6077e2cb487f60a96233c08cb479dbf31538cc915ec6d48bdbaa96ddc1a16db4f4f96f37276cfcb3510b8246241770d5952c" );
        rule.setType( "sha512" );

        rule.execute( EnforcerTestUtils.getHelper() );
    }
}
