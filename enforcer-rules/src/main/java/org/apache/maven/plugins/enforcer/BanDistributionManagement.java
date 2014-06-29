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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.DistributionManagementCheck;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * This rule will check if a pom contains a <code>distributionManagement</code> part. This should be by best practice
 * only defined once. It could happen that you like to check the parent as well. This can be activated by using the
 * <code>ignoreParent</code> which is by default turned off (<code>true</code>) which means not to check the parent.
 * 
 * @author Karl Heinz Marbaise
 * @since 1.4
 */
public class BanDistributionManagement
    extends AbstractNonCacheableEnforcerRule
{

    /**
     * If we turn on the <code>ignoreParent</code> the parent will be ignored.
     */
    private boolean ignoreParent = true;

    /**
     * Allow using a repository entry in the distributionManagement area.
     */
    private boolean allowRepository = false;

    /**
     * Allow snapshotRepository entry in the distributionManagement area.
     */
    private boolean allowSnapshotRepository = false;

    /**
     * Allow site entry in the distributionManagement area.
     */
    private boolean allowSite = false;

    /**
     * Execute checking only in root of project.
     */
    private boolean executeRootOnly = false;

    private Log logger;

    /**
     * {@inheritDoc}
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        logger = helper.getLog();

        try
        {
            MavenProject project = (MavenProject) helper.evaluate( "${project}" );

            if ( !project.isExecutionRoot() && !executeRootOnly )
            {

                logger.debug( "distributionManagement: " + project.getDistributionManagement() );

                DistributionManagementCheck check = new DistributionManagementCheck( project );

                check.execute( isAllowRepository(), isAllowSnapshotRepository(), isAllowSite() );

                if ( !isIgnoreParent() && ( project.getParent() != null ) )
                {
                    DistributionManagementCheck checkParent = new DistributionManagementCheck( project.getParent() );
                    checkParent.execute( isAllowRepository(), isAllowSnapshotRepository(), isAllowSite() );
                }

            }
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( e.getMessage(), e );
        }
    }

    public boolean isIgnoreParent()
    {
        return ignoreParent;
    }

    public void setIgnoreParent( boolean ignoreParent )
    {
        this.ignoreParent = ignoreParent;
    }

    public boolean isAllowRepository()
    {
        return allowRepository;
    }

    public void setAllowRepository( boolean allowRepository )
    {
        this.allowRepository = allowRepository;
    }

    public boolean isAllowSnapshotRepository()
    {
        return allowSnapshotRepository;
    }

    public void setAllowSnapshotRepository( boolean allowSnapshotRepository )
    {
        this.allowSnapshotRepository = allowSnapshotRepository;
    }

    public boolean isAllowSite()
    {
        return allowSite;
    }

    public void setAllowSite( boolean allowSite )
    {
        this.allowSite = allowSite;
    }

}
