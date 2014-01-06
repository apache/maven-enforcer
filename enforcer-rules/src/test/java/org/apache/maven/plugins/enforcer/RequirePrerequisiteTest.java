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

import static org.mockito.Mockito.*;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

public class RequirePrerequisiteTest
{
    @Test( expected = EnforcerRuleException.class )
    public void testNoPrerequisite()
        throws Exception
    {
        RequirePrerequisite rule = new RequirePrerequisite();

        MavenProject project = mock( MavenProject.class );

        EnforcerRuleHelper helper = mock( EnforcerRuleHelper.class );
        when( helper.evaluate( "${project}" ) ).thenReturn( project );

        rule.execute( helper );
    }
    
    @Test
    public void testNoSpecifiedPrerequisite()
        throws Exception
    {
        RequirePrerequisite rule = new RequirePrerequisite();

        MavenProject project = mock( MavenProject.class );
        when( project.getPrerequisites() ).thenReturn( new Prerequisites() );

        EnforcerRuleHelper helper = mock( EnforcerRuleHelper.class );
        when( helper.evaluate( "${project}" ) ).thenReturn( project );

        rule.execute( helper );
    }

    @Test( expected = EnforcerRuleException.class )
    public void testLowerMavenPrerequisite()
        throws Exception
    {
        RequirePrerequisite rule = new RequirePrerequisite();
        rule.setMavenVersion( "3.0" );

        MavenProject project = mock( MavenProject.class );
        when( project.getPrerequisites() ).thenReturn( new Prerequisites() );

        EnforcerRuleHelper helper = mock( EnforcerRuleHelper.class );
        when( helper.evaluate( "${project}" ) ).thenReturn( project );

        rule.execute( helper );
    }

    @Test( expected = EnforcerRuleException.class )
    public void testLowerMavenRangePrerequisite()
        throws Exception
    {
        RequirePrerequisite rule = new RequirePrerequisite();
        rule.setMavenVersion( "[3.0,)" );

        MavenProject project = mock( MavenProject.class );
        when( project.getPrerequisites() ).thenReturn( new Prerequisites() );

        EnforcerRuleHelper helper = mock( EnforcerRuleHelper.class );
        when( helper.evaluate( "${project}" ) ).thenReturn( project );

        rule.execute( helper );
    }

    @Test
    public void testValidPrerequisite()
                    throws Exception
    {
        RequirePrerequisite rule = new RequirePrerequisite();
        rule.setMavenVersion( "2.2.1" );

        MavenProject project = mock( MavenProject.class );
        Prerequisites prerequisites = new Prerequisites();
        prerequisites.setMaven( "3.0" );
        when( project.getPrerequisites() ).thenReturn( prerequisites );

        EnforcerRuleHelper helper = mock( EnforcerRuleHelper.class );
        when( helper.evaluate( "${project}" ) ).thenReturn( project );

        rule.execute( helper );
    }
}
