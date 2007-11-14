package org.apache.maven.plugins.enforcer;

import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 * This rule checks that the current project is not a
 * snapshot
 */
public class RequireReleaseVersion
    implements EnforcerRule
{

    /**
     * Specify a friendly message if the rule fails.
     * 
     * @parameter
     */
    public String message = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
     */
    public void execute ( EnforcerRuleHelper theHelper )
        throws EnforcerRuleException
    {
        try
        {
            MavenProject project = (MavenProject) theHelper.evaluate( "${project}" );

            if ( project.getArtifact().isSnapshot() )
            {
                StringBuffer buf = new StringBuffer();
                if ( message != null )
                {
                    buf.append( message + "\n" );
                }
                buf.append( "This project cannot be a snapshot:" + project.getArtifact().getId() );
                throw new EnforcerRuleException( buf.toString() );
            }
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to retrieve the project.", e );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#getCacheId()
     */
    public String getCacheId ()
    {
        return "0";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isCacheable()
     */
    public boolean isCacheable ()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isResultValid(org.apache.maven.enforcer.rule.api.EnforcerRule)
     */
    public boolean isResultValid ( EnforcerRule theCachedRule )
    {
        return false;
    }

}
