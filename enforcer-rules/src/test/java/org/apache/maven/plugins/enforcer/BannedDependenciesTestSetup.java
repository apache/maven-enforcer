package org.apache.maven.plugins.enforcer;

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
    private List<String> includes;

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
                return isSearchTransitive() ? project.getArtifacts() : project.getDependencyArtifacts();
            }
        };
        return rule;
    }


}

