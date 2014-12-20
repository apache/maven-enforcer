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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.junit.Test;

/**
 * This class is intended to test the {@link BanDistributionManagement} rule.
 *
 * @author <a href="mailto:khmarbaise@apache.org">Karl Heinz Marbaise</a>
 */
public class BanDistributionManagementTest
{
    private MavenProject project;

    private EnforcerRuleHelper helper;

    @Test
    public void shouldNotFailWithoutDistributionManagement()
        throws Exception
    {
        BanDistributionManagement rule = setupProjectWithoutDistributionManagement();
        rule.execute( helper );
        // intentionally no assert cause in case of an exception the test will be red.
    }

    /**
     * <pre>
     * &lt;distributionManagement&gt;
     *   &lt;repository&gt;
     *    ...
     *   &lt;/repository&gt;
     * &lt;/distributionManagement&gt;
     * </pre>
     * 
     * @throws Exception
     */
    @Test( expected = EnforcerRuleException.class )
    public void shouldThrowExceptionIfDistributionManagementIsDefinedWithRepository()
        throws Exception
    {
        BanDistributionManagement rule =
            setupProjectWithDistributionManagement( new DeploymentRepository(), null, null );
        rule.execute( helper );
        // intentionally no assert cause we expect an exception.
    }

    /**
     * <pre>
     * &lt;distributionManagement&gt;
     *   &lt;repository&gt;
     *    ...
     *   &lt;/repository&gt;
     *   &lt;snapshotRepository&gt;
     *    ...
     *   &lt;/snapshotRepository&gt;
     * &lt;/distributionManagement&gt;
     * </pre>
     */
    @Test( expected = EnforcerRuleException.class )
    public void shouldThrowExceptionIfDistributionManagementIsDefinedWithRepositorySnapshotRepository()
        throws Exception
    {
        BanDistributionManagement rule =
            setupProjectWithDistributionManagement( new DeploymentRepository(), new DeploymentRepository(), null );
        rule.execute( helper );
        // intentionally no assert cause we expect an exception.
    }

    /**
     * <pre>
     * &lt;distributionManagement&gt;
     *   &lt;repository&gt;
     *    ...
     *   &lt;/repository&gt;
     *   &lt;snapshotRepository&gt;
     *    ...
     *   &lt;/snapshotRepository&gt;
     *   &lt;site&gt;
     *    ...
     *   &lt;/site&gt;
     * &lt;/distributionManagement&gt;
     * </pre>
     */
    @Test( expected = EnforcerRuleException.class )
    public void shouldThrowExceptionIfDistributionManagementIsDefinedWithRepositorySnapshotRepositorySite()
        throws Exception
    {
        BanDistributionManagement rule =
            setupProjectWithDistributionManagement( new DeploymentRepository(), new DeploymentRepository(), new Site() );
        rule.execute( helper );
        // intentionally no assert cause we expect an exception.
    }

    /**
     * <pre>
     * &lt;distributionManagement&gt;
     *   &lt;repository&gt;
     *    ...
     *   &lt;/repository&gt;
     * &lt;/distributionManagement&gt;
     * </pre>
     */
    @Test
    public void shouldAllowDistributionManagementHavingRepository()
        throws Exception
    {
        BanDistributionManagement rule =
            setupProjectWithDistributionManagement( new DeploymentRepository(), null, null );
        rule.setAllowRepository( true );
        rule.execute( helper );
        // intentionally no assert cause in case of an exception the test will be red.
    }

    /**
     * <pre>
     * &lt;distributionManagement&gt;
     *   &lt;repository&gt;
     *    ...
     *   &lt;/repository&gt;
     *   &lt;snapshotRepository&gt;
     *    ...
     *   &lt;/snapshotRepository&gt;
     * &lt;/distributionManagement&gt;
     * </pre>
     */
    @Test
    public void shouldAllowDistributionManagementHavingRepositorySnapshotRepository()
        throws Exception
    {
        BanDistributionManagement rule =
            setupProjectWithDistributionManagement( new DeploymentRepository(), new DeploymentRepository(), null );
        rule.setAllowRepository( true );
        rule.setAllowSnapshotRepository( true );
        rule.execute( helper );
        // intentionally no assert cause in case of an exception the test will be red.
    }

    /**
     * <pre>
     * &lt;distributionManagement&gt;
     *   &lt;repository&gt;
     *    ...
     *   &lt;/repository&gt;
     *   &lt;snapshotRepository&gt;
     *    ...
     *   &lt;/snapshotRepository&gt;
     *   &lt;site&gt;
     *    ...
     *   &lt;/site&gt;
     * &lt;/distributionManagement&gt;
     * </pre>
     */
    @Test
    public void shouldAllowDistributionManagementHavingRepositorySnapshotRepositorySite()
        throws Exception
    {
        BanDistributionManagement rule =
            setupProjectWithDistributionManagement( new DeploymentRepository(), new DeploymentRepository(), new Site() );
        rule.setAllowRepository( true );
        rule.setAllowSnapshotRepository( true );
        rule.setAllowSite( true );
        rule.execute( helper );
        // intentionally no assert cause in case of an exception the test will be red.
    }

    @Test( expected = EnforcerRuleException.class )
    public void shouldThrowExceptionCauseParentProjectHasDistributionManagement()
        throws Exception
    {
        BanDistributionManagement rule =
            setupProjectWithParentDistributionManagement( new DeploymentRepository(), null, null );

        rule.setIgnoreParent( false );

        rule.execute( helper );
        // intentionally no assert cause we expect an exception.
    }

    @Test
    public void shouldThrowExceptionCauseParentProjectHasDistributionManagementSnapshotRepository()
        throws Exception
    {
        BanDistributionManagement rule =
            setupProjectWithParentDistributionManagement( null, new DeploymentRepository(), null );

        rule.setIgnoreParent( false );
        rule.setAllowSnapshotRepository( true );

        rule.execute( helper );
    }

    private BanDistributionManagement setupProjectWithParentDistributionManagement( DeploymentRepository repository,
                                                                                    DeploymentRepository snapshotRepository,
                                                                                    Site site )
        throws ExpressionEvaluationException
    {
        project = mock( MavenProject.class );
        when( project.getPackaging() ).thenReturn( "jar" );
        when( project.getDistributionManagement() ).thenReturn( null );

        MavenProject parentProject = mock( MavenProject.class );

        DistributionManagement dmParent = mock( DistributionManagement.class );
        when( dmParent.getRepository() ).thenReturn( repository );
        when( dmParent.getSnapshotRepository() ).thenReturn( snapshotRepository );
        when( dmParent.getSite() ).thenReturn( site );

        when( parentProject.getDistributionManagement() ).thenReturn( dmParent );

        when( project.getParent() ).thenReturn( parentProject );

        helper = mock( EnforcerRuleHelper.class );
        when( helper.evaluate( "${project}" ) ).thenReturn( project );
        BanDistributionManagement rule = new BanDistributionManagement();

        when( helper.getLog() ).thenReturn( mock( Log.class ) );

        return rule;
    }

    private BanDistributionManagement setupProjectWithoutDistributionManagement()
        throws ExpressionEvaluationException
    {
        project = mock( MavenProject.class );
        when( project.getPackaging() ).thenReturn( "jar" );
        when( project.getDistributionManagement() ).thenReturn( null );

        helper = mock( EnforcerRuleHelper.class );
        when( helper.evaluate( "${project}" ) ).thenReturn( project );
        BanDistributionManagement rule = new BanDistributionManagement();

        when( helper.getLog() ).thenReturn( mock( Log.class ) );

        return rule;
    }

    private BanDistributionManagement setupProjectWithDistributionManagement( DeploymentRepository repository,
                                                                              DeploymentRepository snapshotRepository,
                                                                              Site site )
        throws ExpressionEvaluationException
    {
        project = mock( MavenProject.class );
        when( project.getPackaging() ).thenReturn( "jar" );
        DistributionManagement dm = mock( DistributionManagement.class );
        when( dm.getRepository() ).thenReturn( repository );
        when( dm.getSnapshotRepository() ).thenReturn( snapshotRepository );
        when( dm.getSite() ).thenReturn( site );

        when( project.getDistributionManagement() ).thenReturn( dm );

        helper = mock( EnforcerRuleHelper.class );
        when( helper.evaluate( "${project}" ) ).thenReturn( project );
        BanDistributionManagement rule = new BanDistributionManagement();

        when( helper.getLog() ).thenReturn( mock( Log.class ) );

        return rule;
    }
}
