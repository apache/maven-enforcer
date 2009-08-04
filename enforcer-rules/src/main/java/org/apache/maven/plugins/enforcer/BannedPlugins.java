package org.apache.maven.plugins.enforcer;

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * This rule checks that lists of plguins are not included.
 *
 * @author <a href="mailto:velo.br@gmail.com">Marvin Froeder</a>
 */
public class BannedPlugins
    extends BannedDependencies
{

    protected Set getDependenciesToCheck( MavenProject project )
    {
        return project.getPluginArtifacts();
    }

    protected CharSequence getErrorMessage( Artifact artifact )
    {
        return "Found Banned Plugin: " + artifact.getId() + "\n";
    }

}
