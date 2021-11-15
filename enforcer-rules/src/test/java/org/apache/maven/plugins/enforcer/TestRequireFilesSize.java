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

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test the "require files exist" rule.
 *
 * @author <a href="brianf@apache.org">Brian Fox</a>
 */
public class TestRequireFilesSize
{
    @TempDir
    public File temporaryFolder;

    private final RequireFilesSize rule = new RequireFilesSize();

    @Test
    public void testFileExists()
        throws EnforcerRuleException, IOException
    {
        File f = File.createTempFile( "junit", null, temporaryFolder );

        rule.setFiles( new File[] { f } );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testEmptyFile()
    {
        rule.setFiles( new File[] { null } );
        try
        {
            rule.execute( EnforcerTestUtils.getHelper() );
            fail( "Should get exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertNotNull( e.getMessage() );
        }
    }

    @Test
    public void testEmptyFileAllowNull()
        throws EnforcerRuleException
    {
        rule.setFiles( new File[] { null } );
        rule.setAllowNulls( true );
        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testEmptyFileList()
        throws EnforcerRuleException, IOException
    {
        rule.setFiles( new File[] {} );

        assertEquals( 0, rule.getFiles().length );

        MockProject project = new MockProject();
        File f = File.createTempFile( "junit", null, temporaryFolder );

        ArtifactStubFactory factory = new ArtifactStubFactory();
        Artifact a = factory.getReleaseArtifact();
        a.setFile( f );

        project.setArtifact( a );

        // sanity check the mockProject
        assertSame( f, project.getArtifact().getFile() );

        rule.execute( EnforcerTestUtils.getHelper( project ) );

    }

    @Test
    public void testFileDoesNotExist()
        throws IOException
    {
        File f = File.createTempFile( "junit", null, temporaryFolder );
        f.delete();
        assertFalse( f.exists() );
        rule.setFiles( new File[] { f } );

        try
        {
            rule.execute( EnforcerTestUtils.getHelper() );
            fail( "Should get exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertNotNull( e.getMessage() );
        }
    }

    @Test
    public void testFileTooSmall()
        throws IOException
    {
        File f = File.createTempFile( "junit", null, temporaryFolder );
        rule.setFiles( new File[] { f } );
        rule.setMinsize( 10 );
        try
        {
            rule.execute( EnforcerTestUtils.getHelper() );
            fail( "Should get exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertNotNull( e.getMessage() );
        }
    }

    @Test
    public void testFileTooBig()
        throws IOException
    {
        File f = File.createTempFile( "junit", null, temporaryFolder );
        try ( BufferedWriter out = new BufferedWriter( new FileWriter( f ) ) )
        {
            out.write( "123456789101112131415" );
        }

        rule.setFiles( new File[] { f } );
        rule.setMaxsize( 10 );
        assertTrue( f.length() > 10 );
        try
        {
            rule.execute( EnforcerTestUtils.getHelper() );
            fail( "Should get exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertNotNull( e.getMessage() );
        }
    }

    /**
     * Test id.
     */
    @Test
    public void testId()
    {
        rule.getCacheId();
    }

}
