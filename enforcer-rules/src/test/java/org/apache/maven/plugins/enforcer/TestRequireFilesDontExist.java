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

import java.io.File;
import java.io.IOException;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test the "require files don't exist" rule.
 * 
 * @author <a href="brianf@apache.org">Brian Fox</a>
 */
public class TestRequireFilesDontExist
{
    @TempDir
    public File temporaryFolder;

    private final RequireFilesDontExist rule = new RequireFilesDontExist();

    @Test
    public void testFileExists()
        throws IOException
    {
        File f = File.createTempFile( "junit", null, temporaryFolder );

        rule.setFiles( new File[] { f } );

        try
        {
            rule.execute( EnforcerTestUtils.getHelper() );
            fail( "Expected an Exception." );
        }
        catch ( EnforcerRuleException e )
        {
            assertNotNull( e.getMessage() );
        }
        f.delete();
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

    @Test
    public void testEmptyFileList()
    {
        rule.setFiles( new File[] {} );
        assertEquals( 0, rule.getFiles().length );
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
    public void testEmptyFileListAllowNull()
    {
        rule.setFiles( new File[] {} );
        assertEquals( 0, rule.getFiles().length );
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

    @Test
    public void testFileDoesNotExist()
        throws EnforcerRuleException, IOException
    {
        File f = File.createTempFile( "junit", null, temporaryFolder );
        f.delete();

        assertFalse( f.exists() );

        rule.setFiles( new File[] { f } );

        rule.execute( EnforcerTestUtils.getHelper() );
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
