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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

// TODO: Auto-generated Javadoc
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
    @SuppressWarnings( "unchecked" )
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

    /**
     * Gets the pom model for this file.
     *
     * @param pom the pom
     *
     * @return the model
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws XmlPullParserException the xml pull parser exception
     */
    private Model readModel ( File pom )
        throws IOException, XmlPullParserException
    {
        Reader reader = ReaderFactory.newXmlReader( pom );
        MavenXpp3Reader xpp3 = new MavenXpp3Reader();
        Model model = null;
        try
        {
            model = xpp3.read( reader );
        }
        finally
        {
            reader.close();
            reader = null;
        }
        return model;
    }

    /**
     * This method gets the model for the defined artifact.
     * Looks first in the filesystem, then tries to get it
     * from the repo.
     *
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @param pom the pom
     *
     * @return the pom model
     *
     * @throws ArtifactResolutionException the artifact resolution exception
     * @throws ArtifactNotFoundException the artifact not found exception
     * @throws XmlPullParserException the xml pull parser exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private Model getPomModel ( String groupId, String artifactId, String version, File pom )
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        Model model = null;

        // do we want to look in the reactor like the
        // project builder? Would require @aggregator goal
        // which causes problems in maven core right now
        // because we also need dependency resolution in
        // other
        // rules. (MNG-2277)

        // look in the location specified by pom first.
        boolean found = false;
        try
        {
            model = readModel( pom );

            // i found a model, lets make sure it's the one
            // I want
            found = checkIfModelMatches( groupId, artifactId, version, model );
        }
        catch ( IOException e )
        {
            // nothing here, but lets look in the repo
            // before giving up.
        }
        catch ( XmlPullParserException e )
        {
            // nothing here, but lets look in the repo
            // before giving up.
        }

        // i didn't find it in the local file system, go
        // look in the repo
        if ( !found )
        {
            Artifact pomArtifact = factory.createArtifact( groupId, artifactId, version, null, "pom" );
            resolver.resolve( pomArtifact, remoteRepositories, local );
            model = readModel( pomArtifact.getFile() );
        }

        return model;
    }

    /**
     * This method loops through all the parents, getting
     * each pom model and then its parent.
     *
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @param pom the pom
     *
     * @return the models recursively
     *
     * @throws ArtifactResolutionException the artifact resolution exception
     * @throws ArtifactNotFoundException the artifact not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws XmlPullParserException the xml pull parser exception
     */
    public List<Model> getModelsRecursively ( String groupId, String artifactId, String version, File pom )
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        List<Model> models = null;
        Model model = getPomModel( groupId, artifactId, version, pom );

        Parent parent = model.getParent();

        // recurse into the parent
        if ( parent != null )
        {
            // get the relative path
            String relativePath = parent.getRelativePath();
            if ( StringUtils.isEmpty( relativePath ) )
            {
                relativePath = "../pom.xml";
            }
            // calculate the recursive path
            File parentPom = new File( pom.getParent(), relativePath );

            // if relative path is a directory, append pom.xml
            if ( parentPom.isDirectory() )
            {
                parentPom = new File( parentPom, "pom.xml" );
            }

            models = getModelsRecursively( parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), parentPom );
        }
        else
        {
            // only create it here since I'm not at the top
            models = new ArrayList<Model>();
        }
        models.add( model );

        return models;
    }

    /**
     * Make sure the model is the one I'm expecting.
     *
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @param model Model being checked.
     *
     * @return true, if check if model matches
     */
    protected boolean checkIfModelMatches ( String groupId, String artifactId, String version, Model model )
    {
        // try these first.
        String modelGroup = model.getGroupId();
        String modelVersion = model.getVersion();

        try
        {
            if ( StringUtils.isEmpty( modelGroup ) )
            {
                modelGroup = model.getParent().getGroupId();
            }
            else
            {
                // MENFORCER-30, handle cases where the value is a property like ${project.parent.groupId}
                modelGroup = (String) helper.evaluate( modelGroup );
            }

            if ( StringUtils.isEmpty( modelVersion ) )
            {
                modelVersion = model.getParent().getVersion();
            }
            else
            {
                // MENFORCER-30, handle cases where the value is a property like ${project.parent.version}
                modelVersion = (String) helper.evaluate( modelVersion );
            }
        }
        catch ( NullPointerException e )
        {
            // this is probably bad. I don't have a valid
            // group or version and I can't find a
            // parent????
            // lets see if it's what we're looking for
            // anyway.
        }
        catch ( ExpressionEvaluationException e )
        {
            // as above
        }
        return ( StringUtils.equals( groupId, modelGroup ) && StringUtils.equals( version, modelVersion ) && StringUtils
            .equals( artifactId, model.getArtifactId() ) );
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
