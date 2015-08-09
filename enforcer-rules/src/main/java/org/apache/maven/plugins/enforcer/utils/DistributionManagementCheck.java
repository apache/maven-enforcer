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
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.project.MavenProject;

/**
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
public class DistributionManagementCheck
{
    private DistributionManagement distributionManagement;

    public DistributionManagementCheck( MavenProject project )
    {
        this.distributionManagement = project.getOriginalModel().getDistributionManagement();
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
        return getDistributionManagement().getRepository() != null;
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
        return getDistributionManagement().getSnapshotRepository() != null;
    }

    private boolean hasSite()
    {
        return getDistributionManagement().getSite() != null;
    }

    private boolean hasDistributionManagement()
    {
        return getDistributionManagement() != null;
    }
}
