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

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.junit.Test;

/**
 * The Class TestRequireJavaVersion.
 *
 * @author Tim Sijstermans
 */
public class TestRequireJavaVendor
{

    /**
     * Test Rule: Success case
     *
     * @throws EnforcerRuleException the enforcer rule exception
     */
    @Test
    public void settingTheRequiredJavaVendorToSystemVendorShouldNotFail() throws EnforcerRuleException
    {
        RequireJavaVendor underTest = new RequireJavaVendor();
        // Set the required vendor to the current system vendor
        underTest.setName( SystemUtils.JAVA_VENDOR );
        final EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();
        underTest.execute( helper );
        // No assert and no expected exception because this test should not fail
    }

    /**
     * Test rule: Failing case
     *
     * @throws EnforcerRuleException the enforcer rule exception
     */
    @Test( expected = EnforcerRuleException.class )
    public void excludingTheCurrentVendorShouldFail() throws EnforcerRuleException
    {

        RequireJavaVendor underTest = new RequireJavaVendor();
        // Set the required vendor to something nonsensical
        underTest.setName( "..." + SystemUtils.JAVA_VENDOR );
        final EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();
        underTest.execute( helper );
        // expect EnforcerRuleException to happen
    }
}