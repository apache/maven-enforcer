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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The Class TestRequireJavaVersion.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class TestRequireJavaVersion
{

    /**
     * Test fix jdk version.
     */
    @Test
    public void testFixJDKVersion()
    {
        // test that we only take the first 3 versions for
        // comparison
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.5.0_11" ) ).isEqualTo( "1.5.0-11" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.5.1" ) ).isEqualTo( "1.5.1" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.5.2-1.b11" ) ).isEqualTo( "1.5.2-1" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.5.3_11" ) ).isEqualTo( "1.5.3-11" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.5.4.5_11" ) ).isEqualTo( "1.5.4-5" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.5.5.6_11.2" ) ).isEqualTo( "1.5.5-6" );

        // test for non-standard versions
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1-5-0-11" ) ).isEqualTo( "1.5.0-11" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1-_5-_0-_11" ) ).isEqualTo( "1.5.0-11" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1_5_0_11" ) ).isEqualTo( "1.5.0-11" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.5.0-07" ) ).isEqualTo( "1.5.0-7" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.5.0-b7" ) ).isEqualTo( "1.5.0-7" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.5.0-;7" ) ).isEqualTo( "1.5.0-7" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.6.0-dp" ) ).isEqualTo( "1.6.0" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.6.0-dp2" ) ).isEqualTo( "1.6.0-2" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "1.8.0_73" ) ).isEqualTo( "1.8.0-73" );
        assertThat( RequireJavaVersion.normalizeJDKVersion( "9" ) ).isEqualTo( "9" );

        assertThat( RequireJavaVersion.normalizeJDKVersion( "17" ) ).isEqualTo( "17" );

    }

    /**
     * Test rule.
     *
     * @throws EnforcerRuleException the enforcer rule exception
     */
    @Test
    public void settingsTheJavaVersionAsNormalizedVersionShouldNotFail()
        throws EnforcerRuleException
    {
        String normalizedJDKVersion = RequireJavaVersion.normalizeJDKVersion( SystemUtils.JAVA_VERSION );

        RequireJavaVersion rule = new RequireJavaVersion();
        rule.setVersion( normalizedJDKVersion );

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();

        // test the singular version
        rule.execute( helper );
        // intentionally no assertThat(...) because we don't expect and exception.
    }

    @Test
    public void excludingTheCurrentJavaVersionViaRangeThisShouldFailWithException()
    {
        assertThrows( EnforcerRuleException.class, () -> {
            String thisVersion = RequireJavaVersion.normalizeJDKVersion( SystemUtils.JAVA_VERSION );

            RequireJavaVersion rule = new RequireJavaVersion();
            // exclude this version
            rule.setVersion( "(" + thisVersion );

            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();
            rule.execute( helper );
            // intentionally no assertThat(...) because we expect and exception.
        } );
        // intentionally no assertThat(...) because we expect and exception.
    }

    @Test
    @Disabled
    // TODO: Think about the intention of this test? What should it prove?
    public void thisShouldNotCrash()
        throws EnforcerRuleException
    {
        RequireJavaVersion rule = new RequireJavaVersion();
        rule.setVersion( SystemUtils.JAVA_VERSION );

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();
        rule.execute( helper );
        // intentionally no assertThat(...) because we don't expect and exception.
    }

    /**
     * Test id.
     */
    @Test
    public void testId()
    {
        RequireJavaVersion rule = new RequireJavaVersion();
        assertThat( rule.getCacheId() ).isEqualTo( "0" );
    }
}
