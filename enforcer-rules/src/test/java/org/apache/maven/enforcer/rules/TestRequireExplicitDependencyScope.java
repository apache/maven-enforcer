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

import java.util.Collections;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestRequireExplicitDependencyScope {

    @Mock
    private MavenProject project;

    private RequireExplicitDependencyScope rule;

    @BeforeEach
    void setUp() {
        rule = new RequireExplicitDependencyScope(project);
        rule.setLog(mock(EnforcerLogger.class));
    }

    @Test
    void shouldOutputCustomMessageWhenScopeMissing() {
        String customMessage = "Custom explicit scope message";
        rule.setMessage(customMessage);

        Dependency dep = new Dependency();
        dep.setGroupId("com.example");
        dep.setArtifactId("no-scope-dep");
        // scope is intentionally null

        Model model = mock(Model.class);
        when(model.getDependencies()).thenReturn(Collections.singletonList(dep));
        when(project.getOriginalModel()).thenReturn(model);

        assertThatThrownBy(() -> rule.execute())
                .isInstanceOf(EnforcerRuleException.class)
                .hasMessageStartingWith(customMessage)
                .hasMessageContaining("missing dependency scope");
    }
}
