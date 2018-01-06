package org.apache.maven.plugins.enforcer.utils;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.enforcer.EnforcerExpressionEvaluator;
import org.apache.maven.plugins.enforcer.EnforcerTestUtils;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

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

import junit.framework.TestCase;

/**
 * The Class TestMockEnforcerExpressionEvaluator.
 */
public class TestMockEnforcerExpressionEvaluator
    extends TestCase
{

    /**
     * Test evaluate.
     */
    public void testEvaluate()
    {
        MavenSession session = EnforcerTestUtils.getMavenSession();

        EnforcerExpressionEvaluator ev =
            new MockEnforcerExpressionEvaluator( session );
        assertMatch( ev, "SNAPSHOT" );
        assertMatch( ev, "RELEASE" );
        assertMatch( ev, "SNAPSHOT" );
        assertMatch( ev, "LATEST" );
        assertMatch( ev, "1.0" );
    }

    /**
     * Assert match.
     *
     * @param ev the ev
     * @param exp the exp
     */
    public void assertMatch( EnforcerExpressionEvaluator ev, String exp )
    {
        // the mock enforcer should return the name of the expression as the value.
        try
        {
            assertEquals( exp, ev.evaluate( "${" + exp + "}" ) );
        }
        catch ( ExpressionEvaluationException e )
        {
            fail( e.getLocalizedMessage() );
        }
    }
}
