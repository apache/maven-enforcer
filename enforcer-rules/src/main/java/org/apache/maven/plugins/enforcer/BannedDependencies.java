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

import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.ArtifactUtils;

/**
 * This rule checks that lists of dependencies are not included.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class BannedDependencies
    extends AbstractBanDependencies
{

    /**
     * Specify the banned dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard 
     * by using '*' (ie group:*:1.0) <br>
     * The rule will fail if any dependency matches any exclude, unless it also matches 
     * an include rule.
     * 
     * @see {@link #setExcludes(List)}
     * @see {@link #getExcludes()}
     */
    private List<String> excludes = null;

    /**
     * Specify the allowed dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard 
     * by using '*' (ie group:*:1.0) <br>
     * Includes override the exclude rules. It is meant to allow wide exclusion rules 
     * with wildcards and still allow a
     * smaller set of includes. <br>
     * For example, to ban all xerces except xerces-api -> exclude "xerces", include "xerces:xerces-api"
     * 
     * @see {@link #setIncludes(List)}
     * @see {@link #getIncludes()}
     */
    private List<String> includes = null;

    @Override
    protected Set<Artifact> checkDependencies( Set<Artifact> theDependencies, Log log )
        throws EnforcerRuleException
    {

        Set<Artifact> excluded = ArtifactUtils.checkDependencies( theDependencies, excludes );

        // anything specifically included should be removed
        // from the ban list.
        if ( excluded != null )
        {
            Set<Artifact> included = ArtifactUtils.checkDependencies( theDependencies, includes );

            if ( included != null )
            {
                excluded.removeAll( included );
            }
        }
        return excluded;

    }

    /**
     * Gets the excludes.
     * 
     * @return the excludes
     */
    public List<String> getExcludes()
    {
        return this.excludes;
    }

    /**
     * Specify the banned dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard 
     * by using '*' (ie group:*:1.0) <br>
     * The rule will fail if any dependency matches any exclude, unless it also matches an 
     * include rule.
     * 
     * @see #getExcludes()
     * @param theExcludes the excludes to set
     */
    public void setExcludes( List<String> theExcludes )
    {
        this.excludes = theExcludes;
    }

    /**
     * Gets the includes.
     * 
     * @return the includes
     */
    public List<String> getIncludes()
    {
        return this.includes;
    }

    /**
     * Specify the allowed dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard 
     * by using '*' (ie group:*:1.0) <br>
     * Includes override the exclude rules. It is meant to allow wide exclusion rules with 
     * wildcards and still allow a
     * smaller set of includes. <br>
     * For example, to ban all xerces except xerces-api → exclude "xerces",
     * include "xerces:xerces-api"
     * 
     * @see #setIncludes(List)
     * @param theIncludes the includes to set
     */
    public void setIncludes( List<String> theIncludes )
    {
        this.includes = theIncludes;
    }

}
