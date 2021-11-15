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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test the "require files exist" rule.
 * 
 * @author <a href="brett@apache.org">Brett Porter</a>
 */
public class TestRequireFilesExist
{
    @TempDir
    public File temporaryFolder;

    private final RequireFilesExist rule = new RequireFilesExist();

    @Test
    public void testFileExists()
        throws Exception
    {
        File f = File.createTempFile( "junit", null, temporaryFolder );

        rule.setFiles( new File[] { f.getCanonicalFile() } );

        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileOsIndependentExists()
    {
        rule.setFiles( new File[] { new File( "POM.xml" ) } );

        EnforcerRuleException e =
            assertThrows( EnforcerRuleException.class, () -> rule.execute( EnforcerTestUtils.getHelper() ) );

        assertNotNull( e.getMessage() );
    }

    @Test
    public void testEmptyFile()
    {
        rule.setFiles( new File[] { null } );

        EnforcerRuleException e =
            assertThrows( EnforcerRuleException.class, () -> rule.execute( EnforcerTestUtils.getHelper() ) );

        assertNotNull( e.getMessage() );
    }

    @Test
    public void testEmptyFileAllowNull()
        throws Exception
    {
        rule.setFiles( new File[] { null } );
        rule.setAllowNulls( true );
        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testEmptyFileList()
    {
        rule.setFiles( new File[] {} );
        assertEquals( 0, rule.getFiles().length );

        EnforcerRuleException e =
            assertThrows( EnforcerRuleException.class, () -> rule.execute( EnforcerTestUtils.getHelper() ) );

        assertNotNull( e.getMessage() );

    }

    @Test
    public void testEmptyFileListAllowNull()
        throws Exception
    {
        rule.setFiles( new File[] {} );
        assertEquals( 0, rule.getFiles().length );
        rule.setAllowNulls( true );
        rule.execute( EnforcerTestUtils.getHelper() );
    }

    @Test
    public void testFileDoesNotExist()
        throws Exception
    {
        File f = File.createTempFile( "junit", null, temporaryFolder );
        f.delete();

        assertFalse( f.exists() );
        rule.setFiles( new File[] { f } );

        EnforcerRuleException e =
            assertThrows( EnforcerRuleException.class, () -> rule.execute( EnforcerTestUtils.getHelper() ) );

        assertNotNull( e.getMessage() );

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
