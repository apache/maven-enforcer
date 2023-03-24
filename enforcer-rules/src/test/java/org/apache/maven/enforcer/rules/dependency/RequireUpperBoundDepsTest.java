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
package org.apache.maven.enforcer.rules.dependency;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.DependencyNodeBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequireUpperBoundDepsTest {

    @Mock
    private ResolverUtil resolverUtil;

    @InjectMocks
    private RequireUpperBoundDeps rule;

    @Test
    void testRule() throws Exception {

        rule.setLog(mock(EnforcerLogger.class));

        when(resolverUtil.resolveTransitiveDependenciesVerbose(anyList()))
                .thenReturn(new DependencyNodeBuilder()
                        .withType(DependencyNodeBuilder.Type.POM)
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("childA")
                                .withVersion("1.0.0")
                                .build())
                        .withChildNode(new DependencyNodeBuilder()
                                .withArtifactId("childA")
                                .withVersion("2.0.0")
                                .build())
                        .build());

        assertThatCode(rule::execute)
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageContaining("default-group:childA:1.0.0:classifier")
                .hasMessageContaining("default-group:childA:2.0.0:classifier");
    }
}
