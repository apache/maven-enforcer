package org.apache.maven.plugins.enforcer.utils;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * The Class EnforcerRuleUtils.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class EnforcerRuleUtils
{

    /** The factory. */
    ArtifactFactory factory;

    /** The resolver. */
    ArtifactResolver resolver;

    /** The local. */
    ArtifactRepository local;

    /** The remote repositories. */
    List<ArtifactRepository> remoteRepositories;

    /** The log. */
    Log log;

    /** The project. */
    MavenProject project;

    private EnforcerRuleHelper helper;

    /**
     * Instantiates a new enforcer rule utils.
     *
     * @param theFactory the the factory
     * @param theResolver the the resolver
     * @param theLocal the the local
     * @param theRemoteRepositories the the remote repositories
     * @param project the project
     * @param theLog the the log
     */
    public EnforcerRuleUtils( ArtifactFactory theFactory, ArtifactResolver theResolver, ArtifactRepository theLocal,
                              List<ArtifactRepository> theRemoteRepositories, MavenProject project, Log theLog )
    {
        super();
        this.factory = theFactory;
        this.resolver = theResolver;
        this.local = theLocal;
        this.remoteRepositories = theRemoteRepositories;
        this.log = theLog;
        this.project = project;
    }

    /**
     * Instantiates a new enforcer rule utils.
     *
     * @param helper the helper
     */
    public EnforcerRuleUtils( EnforcerRuleHelper helper )
    {

        this.helper = helper;
        // get the various expressions out of the
        // helper.
        try
        {
            factory = (ArtifactFactory) helper.getComponent( ArtifactFactory.class );
            resolver = (ArtifactResolver) helper.getComponent( ArtifactResolver.class );
            local = (ArtifactRepository) helper.evaluate( "${localRepository}" );
            project = (MavenProject) helper.evaluate( "${project}" );
            remoteRepositories = project.getRemoteArtifactRepositories();
        }
        catch ( ComponentLookupException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( ExpressionEvaluationException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void resolve( Plugin plugin )
    {
        try
        {
            plugin.setGroupId( (String) helper.evaluate( plugin.getGroupId() ) );
            plugin.setArtifactId( (String) helper.evaluate( plugin.getArtifactId() ) );
            plugin.setVersion( (String) helper.evaluate( plugin.getVersion() ) );
        }
        catch ( ExpressionEvaluationException e )
        {
            // this should have gone already before
        }
    }

    private void resolve( ReportPlugin plugin )
    {
        try
        {
            plugin.setGroupId( (String) helper.evaluate( plugin.getGroupId() ) );
            plugin.setArtifactId( (String) helper.evaluate( plugin.getArtifactId() ) );
            plugin.setVersion( (String) helper.evaluate( plugin.getVersion() ) );
        }
        catch ( ExpressionEvaluationException e )
        {
            // this should have gone already before
        }
    }

    public List<Plugin> resolvePlugins( List<Plugin> plugins )
    {
        for ( Plugin plugin : plugins )
        {
            resolve( plugin );
        }
        return plugins;
    }

    public List<ReportPlugin> resolveReportPlugins( List<ReportPlugin> reportPlugins )
    {
        for ( ReportPlugin plugin : reportPlugins )
        {
            resolve( plugin );
        }
        return reportPlugins;
    }

}
