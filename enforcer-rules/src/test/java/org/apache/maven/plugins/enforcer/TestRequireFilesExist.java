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

import junit.framework.TestCase;

/**
 * Test the "require files exist" rule.
 * 
 * @author <a href="brett@apache.org">Brett Porter</a>
 */
public class TestRequireFilesExist
    extends TestCase
{
    RequireFilesExist rule = new RequireFilesExist();

    public void testFileExists()
        throws EnforcerRuleException, IOException
    {
        File f = File.createTempFile( "enforcer", "tmp" );
        f.deleteOnExit();

        rule.files = new File[] { f };

        rule.execute( EnforcerTestUtils.getHelper() );

        f.delete();
    }

    public void testFileDoesNotExist()
        throws EnforcerRuleException, IOException
    {
        File f = File.createTempFile( "enforcer", "tmp" );
        f.delete();

        rule.files = new File[] { f };

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
