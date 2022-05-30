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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * This rule checks that no snapshots are included.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class RequireReleaseDeps
    extends AbstractBanDependencies
{

    /**
     * Allows this rule to execute only when this project is a release.
     *
     * @see {@link #setOnlyWhenRelease(boolean)}
     * @see {@link #isOnlyWhenRelease()}

     */
    private boolean onlyWhenRelease = false;

    /**
     * Allows this rule to fail when the parent is defined as a snapshot.
     *
     * @see {@link #setFailWhenParentIsSnapshot(boolean)}
     * @see {@link #isFailWhenParentIsSnapshot()}
     */
    private boolean failWhenParentIsSnapshot = true;

    /**
     * Dependencies to ignore when checking for release versions.  For example, inter-module dependencies 
     * can be excluded from the check and therefore allowed to contain snapshot versions.
     * 
     * @see {@link #setExcludes(List)}
     * @see {@link #getExcludes()}
     */
    private List<String> excludes = null;

    /**
     * Dependencies to include when checking for release versions.  If any of the included dependencies
     * have snapshot versions, the rule will fail.
     * 
     * @see {@link #setIncludes(List)}
     * @see {@link #getIncludes()}
     */
    private List<String> includes = null;

    // Override parent to allow optional ignore of this rule.
    @Override
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

                if ( parentArtifact != null )
                {
                    Set<Artifact> artifacts = filterArtifacts( Collections.singleton( parentArtifact ) );
                    parentArtifact = Optional.ofNullable( artifacts )
                        .flatMap( s -> s.stream().findFirst() )
                        .orElse( null );
                }

                if ( parentArtifact != null && parentArtifact.isSnapshot() )
                {
                    throw new EnforcerRuleException( "Parent Cannot be a snapshot: " + parentArtifact.getId() );
                }
            }
        }
    }

    /**
     * @param helper
     * @return The evaluated {@link MavenProject}.
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

    @Override
    protected Set<Artifact> checkDependencies( Set<Artifact> dependencies, Log log )
        throws EnforcerRuleException
    {
        Set<Artifact> foundSnapshots = new HashSet<>();

        Set<Artifact> filteredDependencies = filterArtifacts( dependencies );

        if ( filteredDependencies != null )
        {
            for ( Artifact artifact : filteredDependencies )
            {
                if ( artifact.isSnapshot() )
                {
                    foundSnapshots.add( artifact );
                }
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
    protected Set<Artifact> filterArtifacts( Set<Artifact> dependencies ) throws EnforcerRuleException 
    {
        if ( includes == null && excludes == null )
        {
            return dependencies;
        }
        
        Set<Artifact> included;
        if ( includes != null )
        {
            included = ArtifactUtils.checkDependencies( dependencies, includes ); 
        }
        else
        {
            included = new HashSet<>( dependencies );
        }

        // anything specifically included should be removed
        // from the ban list.
        if ( included != null )
        {
            Set<Artifact> excluded = ArtifactUtils.checkDependencies( dependencies, excludes );

            if ( excluded != null )
            {
                included.removeAll( excluded );
            }
        }
        return included;
    }

    public final boolean isOnlyWhenRelease()
    {
        return onlyWhenRelease;
    }

    public final void setOnlyWhenRelease( boolean onlyWhenRelease )
    {
        this.onlyWhenRelease = onlyWhenRelease;
    }

    public final boolean isFailWhenParentIsSnapshot()
    {
        return failWhenParentIsSnapshot;
    }

    public final void setFailWhenParentIsSnapshot( boolean failWhenParentIsSnapshot )
    {
        this.failWhenParentIsSnapshot = failWhenParentIsSnapshot;
    }
    
    public final void setExcludes( List<String> excludes )
    {
        this.excludes = excludes;
    }
    
    public final List<String> getExcludes()
    {
        return excludes;
    }
    
    public void setIncludes( List<String> includes )
    {
        this.includes = includes;
    }
    
    public List<String> getIncludes()
    {
        return includes;
    }
}
