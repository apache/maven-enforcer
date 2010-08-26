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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.StrictPatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * This rule checks that no snapshots are included.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class RequireReleaseDeps
    extends AbstractBanDependencies
{

    /**
     * Allows this rule to execute only when this project is a release.
     *
     * @parameter
     */
    public boolean onlyWhenRelease = false;

    /**
     * Allows this rule to fail when the parent is defined as a snapshot.
     *
     * @parameter
     */
    public boolean failWhenParentIsSnapshot = true;

    /**
     * Dependencies to ignore when checking for release versions.  For example, inter-module dependencies 
     * can be excluded from the check and therefore allowed to contain snapshot versions.
     */
    public List excludes = null;

    /**
     * Dependencies to include when checking for release versions.  If any of the included dependencies
     * have snapshot versions, the rule will fail.
     */
    public List includes = null;

    /**
     * Override parent to allow optional ignore of this rule.
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        boolean callSuper;
        MavenProject project = null;
        if ( onlyWhenRelease )
        {
            // get the project
            project = getProject( helper );

            // only call super if this project is a release
            callSuper = !project.getArtifact().isSnapshot();
        }
        else
        {
            callSuper = true;
        }
        if ( callSuper )
        {
            super.execute( helper );
            if ( failWhenParentIsSnapshot )
            {
                if ( project == null )
                {
                    project = getProject( helper );
                }
                Artifact parentArtifact = project.getParentArtifact();
                if ( parentArtifact != null && parentArtifact.isSnapshot() )
                {
                    throw new EnforcerRuleException( "Parent Cannot be a snapshot: " + parentArtifact.getId() );
                }
            }
        }
    }

    /**
     * @param helper
     * @return
     * @throws EnforcerRuleException
     */
    private MavenProject getProject( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        try
        {
            return (MavenProject) helper.evaluate( "${project}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenProject: ", eee );
        }
    }

    /**
     * Checks the set of dependencies to see if any snapshots are included
     *
     * @param dependencies the dependencies
     * @param log the log
     * @return the sets the
     * @throws EnforcerRuleException the enforcer rule exception
     */
    protected Set checkDependencies( Set dependencies, Log log )
        throws EnforcerRuleException
    {
        Set foundSnapshots = new HashSet();

        Set filteredDependencies = this.filterArtifacts( dependencies );
        
        Iterator DependencyIter = filteredDependencies.iterator();
        while ( DependencyIter.hasNext() )
        {
            Artifact artifact = (Artifact) DependencyIter.next();

            if ( artifact.isSnapshot() )
            {
                foundSnapshots.add( artifact );
            }
        }

        return foundSnapshots;
    }
    
    /*
     * Filter the dependency artifacts according to the includes and excludes
     * If includes and excludes are both null, the original set is returned.
     * 
     * @param dependencies the list of dependencies to filter
     * @return the resulting set of dependencies
     */
    public Set filterArtifacts( Set dependencies )
    {
        if ( includes == null && excludes == null )
        {
            return dependencies;
        }
        
        AndArtifactFilter filter = new AndArtifactFilter( );
        if ( includes != null )
        {
            filter.add( new StrictPatternIncludesArtifactFilter( includes ) );
        }
        if ( excludes != null )
        {
            filter.add( new StrictPatternExcludesArtifactFilter( excludes ) );
        }
        
        Set result = new HashSet();
        Iterator iter = dependencies.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( filter.include( artifact ) )
            {
                result.add( artifact );
            }
        }
        return result;
    }

    public boolean isOnlyWhenRelease()
    {
        return onlyWhenRelease;
    }

    public void setOnlyWhenRelease( boolean onlyWhenRelease )
    {
        this.onlyWhenRelease = onlyWhenRelease;
    }

    public boolean isFailWhenParentIsSnapshot()
    {
        return failWhenParentIsSnapshot;
    }

    public void setFailWhenParentIsSnapshot( boolean failWhenParentIsSnapshot )
    {
        this.failWhenParentIsSnapshot = failWhenParentIsSnapshot;
    }
}
