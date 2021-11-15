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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RequirePrerequisiteTest
{
    private MavenProject project;

    private EnforcerRuleHelper helper;

    @BeforeEach
    public void before()
        throws ExpressionEvaluationException
    {
        project = mock( MavenProject.class );
        when( project.getPackaging() ).thenReturn( "maven-plugin" );

        helper = mock( EnforcerRuleHelper.class );
        when( helper.evaluate( "${project}" ) ).thenReturn( project );
    }

    @Test
    public void testNoPrerequisite()
    {
        assertThrows( EnforcerRuleException.class, () -> {
            RequirePrerequisite rule = new RequirePrerequisite();
            rule.execute( helper );
        } );
    }

    @Test
    public void testNoSpecifiedPrerequisite()
        throws Exception
    {
        when( project.getPrerequisites() ).thenReturn( new Prerequisites() );

        RequirePrerequisite rule = new RequirePrerequisite();
        rule.execute( helper );
    }

    @Test
    public void testLowerMavenPrerequisite()
    {
        assertThrows( EnforcerRuleException.class, () -> {
            when( project.getPrerequisites() ).thenReturn( new Prerequisites() );

            RequirePrerequisite rule = new RequirePrerequisite();
            rule.setMavenVersion( "3.0" );
            rule.execute( helper );
        } );
    }

    @Test
    public void testLowerMavenRangePrerequisite()
    {
        assertThrows( EnforcerRuleException.class, () -> {
            when( project.getPrerequisites() ).thenReturn( new Prerequisites() );

            RequirePrerequisite rule = new RequirePrerequisite();
            rule.setMavenVersion( "[3.0,)" );

            rule.execute( helper );
        } );
    }

    @Test
    public void testMavenRangesPrerequisite()
    {
        assertThrows( EnforcerRuleException.class, () -> {
            Prerequisites prerequisites = new Prerequisites();
            prerequisites.setMaven( "2.2.0" );
            when( project.getPrerequisites() ).thenReturn( prerequisites );

            RequirePrerequisite rule = new RequirePrerequisite();
            rule.setMavenVersion( "[2.0.6,2.1.0),(2.1.0,2.2.0),(2.2.0,)" );

            rule.execute( helper );
        } );
    }

    @Test
    public void testValidPrerequisite()
        throws Exception
    {
        Prerequisites prerequisites = new Prerequisites();
        prerequisites.setMaven( "3.0" );
        when( project.getPrerequisites() ).thenReturn( prerequisites );

        RequirePrerequisite rule = new RequirePrerequisite();
        rule.setMavenVersion( "2.2.1" );

        rule.execute( helper );
    }

    @Test
    public void testPomPackaging()
        throws Exception
    {
        when( project.getPackaging() ).thenReturn( "pom" );

        Log log = mock( Log.class );
        when( helper.getLog() ).thenReturn( log );

        RequirePrerequisite rule = new RequirePrerequisite();
        rule.execute( helper );

        verify( log ).debug( "Packaging is pom, skipping requirePrerequisite rule" );
    }

    @Test
    public void testMatchingPackagings()
    {
        assertThrows( EnforcerRuleException.class, () -> {
            when( project.getPackaging() ).thenReturn( "maven-plugin" );

            RequirePrerequisite rule = new RequirePrerequisite();
            rule.setPackagings( Collections.singletonList( "maven-plugin" ) );
            rule.execute( helper );
        } );
    }

    @Test
    public void testNotMatchingPackagings()
        throws Exception
    {
        when( project.getPackaging() ).thenReturn( "jar" );

        Log log = mock( Log.class );
        when( helper.getLog() ).thenReturn( log );

        RequirePrerequisite rule = new RequirePrerequisite();
        rule.setPackagings( Collections.singletonList( "maven-plugin" ) );
        rule.execute( helper );

        verify( log ).debug( "Packaging is jar, skipping requirePrerequisite rule" );
    }

}
