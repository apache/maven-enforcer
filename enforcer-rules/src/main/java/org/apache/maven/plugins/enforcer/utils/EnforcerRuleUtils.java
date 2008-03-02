package org.apache.maven.plugins.enforcer.utils;

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
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class EnforcerRuleUtils
{
    ArtifactFactory factory;

    ArtifactResolver resolver;

    ArtifactRepository local;

    List remoteRepositories;

    Log log;

    MavenProject project;

    public EnforcerRuleUtils( ArtifactFactory theFactory, ArtifactResolver theResolver, ArtifactRepository theLocal,
                              List theRemoteRepositories, MavenProject project, Log theLog )
    {
        super();
        this.factory = theFactory;
        this.resolver = theResolver;
        this.local = theLocal;
        this.remoteRepositories = theRemoteRepositories;
        this.log = theLog;
        this.project = project;
    }

    public EnforcerRuleUtils( EnforcerRuleHelper helper )
    {
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
     * @param pom
     * @return
     * @throws IOException
     * @throws XmlPullParserException
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
     * @param factory
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws XmlPullParserException
     * @throws IOException
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
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws IOException
     * @throws XmlPullParserException
     */
    public List getModelsRecursively ( String groupId, String artifactId, String version, File pom )
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        List models = null;
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

            models = getModelsRecursively( parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), parentPom );
        }
        else
        {
            // only create it here since I'm not at the top
            models = new ArrayList();
        }
        models.add( model );

        return models;
    }

    /**
     * Make sure the model is the one I'm expecting.
     * 
     * @param groupId
     * @param artifactId
     * @param version
     * @param model Model being checked.
     * @return
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

            if ( StringUtils.isEmpty( modelVersion ) )
            {
                modelVersion = model.getParent().getVersion();
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
        return ( StringUtils.equals( groupId, modelGroup ) && StringUtils.equals( version, modelVersion ) && StringUtils
            .equals( artifactId, model.getArtifactId() ) );
    }
}
