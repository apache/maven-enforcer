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

import static org.junit.jupiter.api.Assertions.fail;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link RequireEnvironmentVariable}}
 *
 * @author <a href='mailto:marvin[at]marvinformatics[dot]com'>Marvin Froeder</a>
 */
public class TestRequireEnvironmentVariable
{

    /**
     * Test rule.
     *
     */
    @Test
    public void testRule()
    {
        MockProject project = new MockProject();
        project.setProperty( "testProp", "This is a test." );
        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );

        RequireEnvironmentVariable rule = new RequireEnvironmentVariable();
        // this env variable should not be set
        rule.setVariableName( "JUNK" );

        try
        {
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            // expected to catch this.
        }

        // PATH shall be common to windows and linux
        rule.setVariableName( "PATH" );
        try
        {
            rule.execute( helper );
        }
        catch ( EnforcerRuleException e )
        {
            fail( "This should not throw an exception" );
        }
    }

    /**
     * Test rule with regex.
     *
     */
    @Test
    public void testRuleWithRegex()
    {
        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();

        RequireEnvironmentVariable rule = new RequireEnvironmentVariable();
        rule.setVariableName( "PATH" );
        // This expression should not match the property
        // value
        rule.setRegex( "[^abc]" );

        try
        {
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            // expected to catch this.
        }

        // can't really predict what a PATH will looks like, just enforce it ain't empty
        rule.setRegex( ".{1,}" );
        try
        {
            rule.execute( helper );
        }
        catch ( EnforcerRuleException e )
        {
            fail( "This should not throw an exception" );
        }
    }

}
