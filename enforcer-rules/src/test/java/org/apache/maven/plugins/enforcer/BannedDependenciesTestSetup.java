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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;

public class BannedDependenciesTestSetup
{
    public BannedDependenciesTestSetup()
        throws IOException
    {
        this.excludes = new ArrayList<>();
        this.includes = new ArrayList<>();

        ArtifactStubFactory factory = new ArtifactStubFactory();

        MockProject project = new MockProject();
        project.setArtifacts( factory.getMixedArtifacts() );
        project.setDependencyArtifacts( factory.getScopedArtifacts() );

        this.helper = EnforcerTestUtils.getHelper( project );

        this.rule = newBannedDependenciesRule();
        this.rule.setMessage( null );

        this.rule.setExcludes( this.excludes );
        this.rule.setIncludes( this.includes );
    }

    private List<String> excludes;

    private final List<String> includes;

    private final BannedDependencies rule;

    private final EnforcerRuleHelper helper;

    public void setSearchTransitive( boolean searchTransitive )
    {
        rule.setSearchTransitive( searchTransitive );
    }

    public void addExcludeAndRunRule( String toAdd )
        throws EnforcerRuleException
    {
        excludes.add( toAdd );
        rule.execute( helper );
    }

    public void addIncludeExcludeAndRunRule( String incAdd, String excAdd )
        throws EnforcerRuleException
    {
        excludes.add( excAdd );
        includes.add( incAdd );
        rule.execute( helper );
    }

    public List<String> getExcludes()
    {
        return excludes;
    }

    public void setExcludes( List<String> excludes )
    {
        this.excludes = excludes;
    }

    private BannedDependencies newBannedDependenciesRule()
    {
        return new BannedDependencies()
        {
            @Override
            protected Set<Artifact> getDependenciesToCheck( ProjectBuildingRequest buildingRequest )
            {
                MavenProject project = buildingRequest.getProject();

                // the integration with dependencyGraphTree is verified with the integration tests
                // for unit-testing
                return isSearchTransitive() ? project.getArtifacts() : project.getDependencyArtifacts();
            }
        };
    }

}
