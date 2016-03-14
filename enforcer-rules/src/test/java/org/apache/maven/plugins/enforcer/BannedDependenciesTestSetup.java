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

public class BannedDependenciesTestSetup
{
    public BannedDependenciesTestSetup()
        throws IOException
    {
        this.excludes = new ArrayList<String>();
        this.includes = new ArrayList<String>();
        this.excludesUrl = null;
        
        ArtifactStubFactory factory = new ArtifactStubFactory();

        MockProject project = new MockProject();
        project.setArtifacts( factory.getMixedArtifacts() );
        project.setDependencyArtifacts( factory.getScopedArtifacts() );
        
        MockProject project1 = new MockProject();
        project1.setArtifact( factory.createArtifact( "pg1", "pa1", "4.0",Artifact.SCOPE_COMPILE, "pom","") );
        project.setParent( project1 );
        project.setParentArtifact( factory.createArtifact( "pg1", "pa1", "4.0",Artifact.SCOPE_COMPILE, "pom","") );    
        
        this.helper = EnforcerTestUtils.getHelper( project );

        this.rule = newBannedDependenciesRule();
        this.rule.setMessage( null );

        this.rule.setExcludes( this.excludes );
        this.rule.setIncludes( this.includes );
        this.rule.setExcludesUrl( this.excludesUrl );
    }

    private List<String> excludes;
    private List<String> includes;
    private String excludesUrl;
    
    private BannedDependencies rule;

    private EnforcerRuleHelper helper;

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
    
    public void addExcludeUrlAndRunRule( String url )
      throws EnforcerRuleException
  {
      excludesUrl = url;
      rule.setExcludesUrl(excludesUrl);
      rule.execute( helper );
  
  }

    public void addIncludeExcludeAndRunRule (String incAdd, String excAdd) throws EnforcerRuleException {
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
    
    public String getExcludesUrl()
    {
        return excludesUrl;
    }

    public void setExcludesUrl( String excludesUrl )
    {
        this.excludesUrl = excludesUrl;
    }

    private BannedDependencies newBannedDependenciesRule()
    {
        BannedDependencies rule = new BannedDependencies()
        {
            @SuppressWarnings( "unchecked" )
            @Override
            protected Set<Artifact> getDependenciesToCheck( MavenProject project )
            {
                // the integration with dependencyGraphTree is verified with the integration tests
                // for unit-testing
                Set<Artifact> artifacts1 = project.getArtifacts();
                artifacts1.add(project.getParentArtifact());
                
                Set<Artifact> artifacts2 = project.getDependencyArtifacts();
                artifacts2.add(project.getParentArtifact());
                
                //return isSearchTransitive() ? project.getArtifacts() : project.getDependencyArtifacts();
                return isSearchTransitive() ? artifacts1 : artifacts2;
            }
        };
        return rule;
    }


}

