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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;

import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The Class TestMavenVersion.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class TestMavenVersion
{

    /**
     * Test rule.
     *
     * @throws EnforcerRuleException the enforcer rule exception
     */
    @Test
    public void testRule()
        throws EnforcerRuleException
    {

        RequireMavenVersion rule = new RequireMavenVersion();
        rule.setVersion( "2.0.5" );

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();

        // test the singular version
        rule.execute( helper );

        // exclude this version
        rule.setVersion( "(2.0.5" );

        try
        {
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            // expected to catch this.
        }

        // this shouldn't crash
        rule.setVersion( "2.0.5_01" );
        rule.execute( helper );

    }

    /**
     * Test few more cases
     *
     * @throws EnforcerRuleException the enforcer rule exception
     */
    @Test
    public void checkRequireVersionMatrix()
        throws EnforcerRuleException, ExpressionEvaluationException
    {
        RequireMavenVersion rule = new RequireMavenVersion();

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();
        MavenSession mavenSession = (MavenSession) helper.evaluate( "${session}" );
        Properties systemProperties = mavenSession.getSystemProperties();

        systemProperties.setProperty( "maven.version", "3.6.1" );
        rule.setVersion( "3.6.0" );
        rule.execute( helper );

        systemProperties.setProperty( "maven.version", "3.6.2" );
        rule.setVersion( "3.6.0" );
        rule.execute( helper );
        rule.setVersion( "3.6.1" );
        rule.execute( helper );
        rule.setVersion( "3.6.2" );
        rule.execute( helper );
        rule.setVersion( "3.6.3" );
        try
        {
            rule.execute( helper );
            fail( "Expected an exception." );
        }
        catch ( EnforcerRuleException e )
        {
            // expected to catch this.
        }
    }

    /**
     * Test id.
     */
    @Test
    public void testId()
    {
        RequireMavenVersion rule = new RequireMavenVersion();
        rule.getCacheId();
    }
}
