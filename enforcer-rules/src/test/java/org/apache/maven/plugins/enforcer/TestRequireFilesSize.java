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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.testing.ArtifactStubFactory;

import junit.framework.TestCase;

/**
 * Test the "require files exist" rule.
 *
 * @author <a href="brianf@apache.org">Brian Fox</a>
 */
public class TestRequireFilesSize
    extends TestCase
{
    RequireFilesSize rule = new RequireFilesSize();

    public void testFileExists()
        throws EnforcerRuleException, IOException
    {
        File f = File.createTempFile( "enforcer", "tmp" );
        f.deleteOnExit();

        rule.setFiles( new File[] { f } );

        rule.execute( EnforcerTestUtils.getHelper() );

        f.delete();
    }

    public void testEmptyFile()
        throws EnforcerRuleException, IOException
    {
        rule.setFiles( new File[] { null } );
        try
        {
            rule.execute( EnforcerTestUtils.getHelper() );
            fail( "Should get exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertTrue( true );
        }
    }

    public void testEmptyFileAllowNull()
        throws EnforcerRuleException, IOException
    {
        rule.setFiles( new File[] { null } );
        rule.setAllowNulls( true );
        try
        {
            rule.execute( EnforcerTestUtils.getHelper() );
        }
        catch ( EnforcerRuleException e )
        {
            fail( "Unexpected Exception:" + e.getLocalizedMessage() );
        }
    }

    public void testEmptyFileList()
        throws EnforcerRuleException, IOException
    {
        rule.setFiles( new File[] {} );

        assertEquals( 0, rule.getFiles().length );

        MockProject project = new MockProject();
        File f = File.createTempFile( "enforcer", "tmp" );
        f.deleteOnExit();
        ArtifactStubFactory factory = new ArtifactStubFactory();
        Artifact a = factory.getReleaseArtifact();
        a.setFile( f );

        project.setArtifact( a );

        // sanity check the mockProject
        assertSame( f, project.getArtifact().getFile() );

        rule.execute( EnforcerTestUtils.getHelper( project ) );

    }

    public void testFileDoesNotExist()
        throws EnforcerRuleException, IOException
    {
        File f = File.createTempFile( "enforcer", "tmp" );
        f.delete();
        assertTrue( !f.exists() );
        rule.setFiles( new File[] { f } );

        try
        {
            rule.execute( EnforcerTestUtils.getHelper() );
            fail( "Should get exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertTrue( true );
        }
    }

    public void testFileTooSmall()
        throws EnforcerRuleException, IOException
    {
        File f = File.createTempFile( "enforcer", "tmp" );
        f.deleteOnExit();
        rule.setFiles( new File[] { f } );
        rule.setMinsize( 10 );
        try
        {
            rule.execute( EnforcerTestUtils.getHelper() );
            fail( "Should get exception" );
        }
        catch ( EnforcerRuleException e )
        {
            assertTrue( true );
        }
    }

    public void testFileTooBig()
        throws EnforcerRuleException, IOException
    {
        File f = File.createTempFile( "enforcer", "tmp" );
        f.deleteOnExit();
        try
        {
            // Create file
            FileWriter fstream = new FileWriter( f );
            BufferedWriter out = new BufferedWriter( fstream );
            out.write( "123456789101112131415" );
            // Close the output stream
            out.close();
            fstream.close();
        }
        catch ( Exception e )
        {// Catch exception if any
            System.err.println( "Error: " + e.getMessage() );
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
            assertTrue( true );
        }
    }

    /**
     * Test id.
     */
    public void testId()
    {
        rule.getCacheId();
    }

}
