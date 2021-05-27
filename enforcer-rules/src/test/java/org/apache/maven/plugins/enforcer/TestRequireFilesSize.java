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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test the "require files exist" rule.
 *
 * @author <a href="brianf@apache.org">Brian Fox</a>
 */
public class TestRequireFilesSize
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private RequireFilesSize rule = new RequireFilesSize();

    @Test
    public void testFileExists()
        throws EnforcerRuleException, IOException
    {
        File f = temporaryFolder.newFile();

        rule.setFiles( new File[] { f } );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
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
            assertNotNull( e.getMessage() );
        }
    }

    @Test
    public void testEmptyFileAllowNull()
        throws EnforcerRuleException, IOException
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
        File f = temporaryFolder.newFile();

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
        throws EnforcerRuleException, IOException
    {
        File f = temporaryFolder.newFile();
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
        throws EnforcerRuleException, IOException
    {
        File f = temporaryFolder.newFile();
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
        File f = temporaryFolder.newFile();
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
