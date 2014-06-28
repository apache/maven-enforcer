package org.apache.maven.plugins.enforcer.utils;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.project.MavenProject;

public class DistributionManagementCheck
{
    private DistributionManagement distributionManagement;

    public DistributionManagementCheck( MavenProject project )
    {
        this.distributionManagement = project.getDistributionManagement();
    }

    public void execute( boolean isAllowRepository, boolean isAllowSnapshotRepository, boolean isAllowSite )
        throws EnforcerRuleException
    {
        if ( hasDistributionManagement() )
        {
            if ( !isAllowRepository && hasRepository() )
            {
                throw new EnforcerRuleException( "You have defined a repository in distributionManagement." );
            }
            else if ( !isAllowSnapshotRepository && hasSnapshotRepository() )
            {
                throw new EnforcerRuleException( "You have defined a snapshotRepository in distributionManagement." );
            }
            else if ( !isAllowSite && hasSite() )
            {
                throw new EnforcerRuleException( "You have defined a site in distributionManagement." );
            }
        }

    }

    private boolean hasRepository()
    {
        if ( getDistributionManagement().getRepository() == null )
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public DistributionManagement getDistributionManagement()
    {
        return distributionManagement;
    }

    public void setDistributionManagement( DistributionManagement distributionManagement )
    {
        this.distributionManagement = distributionManagement;
    }

    private boolean hasSnapshotRepository()
    {
        if ( getDistributionManagement().getSnapshotRepository() == null )
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    private boolean hasSite()
    {
        if ( getDistributionManagement().getSite() == null )
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    private boolean hasDistributionManagement()
    {
        if ( getDistributionManagement() == null )
        {
            return false;
        }
        else
        {
            return true;
        }
    }

}