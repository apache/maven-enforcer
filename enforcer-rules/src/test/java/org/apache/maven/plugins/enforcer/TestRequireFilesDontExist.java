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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test the "require files don't exist" rule.
 * 
 * @author <a href="brianf@apache.org">Brian Fox</a>
 */
public class TestRequireFilesDontExist
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private RequireFilesDontExist rule = new RequireFilesDontExist();

    @Test
    public void testFileExists()
        throws EnforcerRuleException, IOException
    {
        File f = temporaryFolder.newFile();

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
        throws EnforcerRuleException, IOException
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
        throws EnforcerRuleException, IOException
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
        File f = temporaryFolder.newFile();
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
