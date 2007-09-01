package org.apache.maven.enforcer.rule;

import java.io.File;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class MyCustomRule
    implements EnforcerRule
{
    /**
     * Simple param. This rule will fail if the value is true.
     */
    private boolean shouldIfail = false;

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        Log log = helper.getLog();

        try
        {
            // get the various expressions out of the helper.
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );
            MavenSession session = (MavenSession) helper.evaluate( "${session}" );
            String target = (String) helper.evaluate( "${project.build.directory}" );
            String artifactId = (String) helper.evaluate( "${project.artifactId}" );

            // retreive any component out of the session directly
            ArtifactResolver resolver = (ArtifactResolver) helper.getComponent( ArtifactResolver.class );
            RuntimeInformation rti = (RuntimeInformation) helper.getComponent( RuntimeInformation.class );

            log.info( "Retrieved Target Folder: " + target );
            log.info( "Retrieved ArtifactId: " +artifactId );
            log.info( "Retrieved Project: " + project );
            log.info( "Retrieved RuntimeInfo: " + rti );
            log.info( "Retrieved Session: " + session );
            log.info( "Retrieved Resolver: " + resolver );

            if ( this.shouldIfail )
            {
                throw new EnforcerRuleException( "Failing because my param said so." );
            }
        }
        catch ( ComponentLookupException e )
        {
            throw new EnforcerRuleException( "Unable to lookup a component " + e.getLocalizedMessage(), e );
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to lookup an expression " + e.getLocalizedMessage(), e );
        }
    }
}
