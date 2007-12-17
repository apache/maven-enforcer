package org.apache.maven.plugins.enforcer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.enforcer.utils.EnforcerRuleUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class RequireNoRepositories
    extends AbstractStandardEnforcerRule
{

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        EnforcerRuleUtils utils = new EnforcerRuleUtils( helper );

        MavenProject project;
        try
        {
            project = (MavenProject) helper.evaluate( "${project}" );

            List models =
                utils.getModelsRecursively( project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                            new File( project.getBasedir(), "pom.xml" ) );

            List badModels = checkModels( models );

            // if anything was found, log it then append the
            // optional message.
            if ( !badModels.isEmpty() )
            {
                StringBuffer newMsg = new StringBuffer();
                newMsg.append( "Some poms have repositories defined:\n" );
                Iterator iter = badModels.iterator();
                while ( iter.hasNext() )
                {
                    Model model = (Model) iter.next();
                    newMsg.append( model.getGroupId() + ":" + model.getArtifactId() + " version:" + model.getVersion() +
                        "\n" );
                }
                if ( StringUtils.isNotEmpty( message ) )
                {
                    newMsg.append( message );
                }

                throw new EnforcerRuleException( newMsg.toString() );
            }

        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( IOException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
        catch ( XmlPullParserException e )
        {
            throw new EnforcerRuleException( e.getLocalizedMessage() );
        }
    }

    private List checkModels( List models )
    {
        List badModels = new ArrayList();

        Iterator iter = models.iterator();
        while ( iter.hasNext() )
        {
            Model model = (Model) iter.next();
            List repos = model.getRepositories();
            if ( repos != null && !repos.isEmpty() )
            {
                badModels.add( model );
            }
        }
        return badModels;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#getCacheId()
     */
    public String getCacheId()
    {
        return "0";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isCacheable()
     */
    public boolean isCacheable()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isResultValid(org.apache.maven.enforcer.rule.api.EnforcerRule)
     */
    public boolean isResultValid( EnforcerRule theCachedRule )
    {
        return false;
    }
}
