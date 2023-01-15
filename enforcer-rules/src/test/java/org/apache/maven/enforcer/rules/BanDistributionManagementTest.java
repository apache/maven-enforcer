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
package org.apache.maven.enforcer.rules;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Site;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is intended to test the {@link BanDistributionManagement} rule.
 *
 * @author <a href="mailto:khmarbaise@apache.org">Karl Heinz Marbaise</a>
 */
@ExtendWith(MockitoExtension.class)
class BanDistributionManagementTest {

    @Mock
    private MavenProject project;

    @Mock
    private EnforcerLogger log;

    @InjectMocks
    private BanDistributionManagement rule;

    @BeforeEach
    void setup() {
        rule.setLog(log);
    }

    @Test
    void shouldNotFailWithoutDistributionManagement() throws Exception {
        setupProjectWithoutDistributionManagement();
        rule.execute();
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
     */
    @Test
    void shouldThrowExceptionIfDistributionManagementIsDefinedWithRepository() {
        setupProjectWithDistributionManagement(new DeploymentRepository(), null, null);
        assertThrows(EnforcerRuleException.class, () -> rule.execute());
    }

    /**
     * <pre>
     * &lt;distributionManagement&gt;
     *   &lt;snapshotRepository&gt;
     *    ...
     *   &lt;/snapshotRepository&gt;
     * &lt;/distributionManagement&gt;
     * </pre>
     */
    @Test
    void shouldThrowExceptionIfDistributionManagementIsDefinedWithRepositorySnapshotRepository() {
        setupProjectWithDistributionManagement(null, new DeploymentRepository(), null);

        assertThrows(EnforcerRuleException.class, () -> rule.execute());
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
    void shouldThrowExceptionIfDistributionManagementIsDefinedWithRepositorySnapshotRepositorySite() {
        setupProjectWithDistributionManagement(new DeploymentRepository(), null, null);

        assertThrows(EnforcerRuleException.class, () -> rule.execute());
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
     * @throws Exception if any occurs
     */
    @Test
    void shouldAllowDistributionManagementHavingRepository() throws Exception {
        setupProjectWithDistributionManagement(null, null, null);
        rule.setAllowRepository(true);
        rule.execute();
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
     *
     * @throws Exception if any occurs
     */
    @Test
    void shouldAllowDistributionManagementHavingRepositorySnapshotRepository() throws Exception {
        setupProjectWithDistributionManagement(null, null, null);

        rule.setAllowRepository(true);
        rule.setAllowSnapshotRepository(true);
        rule.execute();
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
     *
     * @throws Exception if any occurs
     */
    @Test
    void shouldAllowDistributionManagementHavingRepositorySnapshotRepositorySite() throws Exception {
        setupProjectWithDistributionManagement(null, null, null);
        rule.setAllowRepository(true);
        rule.setAllowSnapshotRepository(true);
        rule.setAllowSite(true);
        rule.execute();
        // intentionally no assert cause in case of an exception the test will be red.
    }

    private void setupProjectWithoutDistributionManagement() {
        setupProject(null);
    }

    private void setupProjectWithDistributionManagement(
            DeploymentRepository repository, DeploymentRepository snapshotRepository, Site site) {
        DistributionManagement dm = mock(DistributionManagement.class);
        if (repository != null) {
            when(dm.getRepository()).thenReturn(repository);
        }
        if (snapshotRepository != null) {
            when(dm.getSnapshotRepository()).thenReturn(snapshotRepository);
        }
        if (site != null) {
            when(dm.getSite()).thenReturn(site);
        }
        setupProject(dm);

        when(project.getParent()).thenReturn(mock(MavenProject.class));
        when(project.isExecutionRoot()).thenReturn(true);
    }

    private void setupProject(DistributionManagement distributionManagement) {
        //        when(project.getPackaging()).thenReturn("jar");
        Model mavenModel = mock(Model.class);
        when(project.getOriginalModel()).thenReturn(mavenModel);
        when(mavenModel.getDistributionManagement()).thenReturn(distributionManagement);
    }
}
