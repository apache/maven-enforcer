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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.Test;

/**
 * Exhaustively check the OS mojo.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class TestRequireOS
{

    /**
     * Test os.
     */
    @Test
    public void testOS()
    {
        Log log = new SystemStreamLog();

        RequireOS rule = new RequireOS( new OperatingSystemProfileActivator() );

        rule.displayOSInfo( log, true );

        Iterator<String> iter = Os.getValidFamilies().iterator();
        String validFamily;
        String invalidFamily = null;
        while ( iter.hasNext() )
        {
            String fam = iter.next();
            if ( !Os.isFamily( fam ) )
            {
                invalidFamily = fam;
                break;
            }
        }

        validFamily = Os.OS_FAMILY;

        log.info( "Testing Mojo Using Valid Family: " + validFamily + " Invalid Family: " + invalidFamily );

        rule.setFamily( validFamily );
        assertTrue( rule.isAllowed() );

        rule.setFamily( invalidFamily );
        assertFalse( rule.isAllowed() );

        rule.setFamily( "!" + invalidFamily );
        assertTrue( rule.isAllowed() );

        rule.setFamily( null );
        rule.setArch( Os.OS_ARCH );
        assertTrue( rule.isAllowed() );

        rule.setArch( "somecrazyarch" );
        assertFalse( rule.isAllowed() );

        rule.setArch( "!somecrazyarch" );
        assertTrue( rule.isAllowed() );

        rule.setArch( null );

        rule.setName( Os.OS_NAME );
        assertTrue( rule.isAllowed() );

        rule.setName( "somecrazyname" );
        assertFalse( rule.isAllowed() );

        rule.setName( "!somecrazyname" );
        assertTrue( rule.isAllowed() );

        rule.setName( null );

        rule.setVersion( Os.OS_VERSION );
        assertTrue( rule.isAllowed() );

        rule.setVersion( "somecrazyversion" );
        assertFalse( rule.isAllowed() );

        rule.setVersion( "!somecrazyversion" );
        assertTrue( rule.isAllowed() );
    }

    @Test
    public void testInvalidFamily()
    {
        RequireOS rule = new RequireOS();

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();
        helper.getContainer().addComponent( new OperatingSystemProfileActivator(), "os" );

        rule.setFamily( "junk" );
        try
        {
            rule.execute( helper );
            fail( "Expected MojoExecution Exception because of invalid family type" );
        }
        catch ( EnforcerRuleException e )
        {
            assertThat( e.getMessage(), startsWith( "Invalid Family type used. Valid family types are: " ) );
        }
    }

    @Test
    public void testId()
    {
        RequireOS rule = new RequireOS();
        rule.getCacheId();
    }

}
