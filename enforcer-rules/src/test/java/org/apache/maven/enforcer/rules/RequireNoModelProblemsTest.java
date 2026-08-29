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
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelProblem;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequireNoModelProblemsTest {

    @Test
    void skipsUnsupportedMavenWithWarning() {
        assumeFalse(
                hasModelProblemsAccessor(),
                "This test requires a Maven version without MavenSession#getModelProblems()");

        EnforcerLogger logger = mock(EnforcerLogger.class);
        RequireNoModelProblems rule = newRule(newSession(), logger);

        assertDoesNotThrow(rule::execute);

        verify(logger).warn(contains("getModelProblems() is unavailable"));
    }

    private static boolean hasModelProblemsAccessor() {
        try {
            MavenSession.class.getMethod("getModelProblems");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Test
    void passesWhenThereAreNoModelProblems() {
        RequireNoModelProblems rule =
                newRule(new ModelProblemsMavenSession(Collections.emptyList()), mock(EnforcerLogger.class));

        assertDoesNotThrow(rule::execute);
    }

    @Test
    void reportsRetainedModelProblems() {
        ModelProblem problem = new DefaultModelProblem(
                "missing plugin version",
                ModelProblem.Severity.WARNING,
                ModelProblem.Version.BASE,
                "pom.xml",
                12,
                4,
                "org.example:project:1",
                null);
        RequireNoModelProblems rule =
                newRule(new ModelProblemsMavenSession(Collections.singletonList(problem)), mock(EnforcerLogger.class));
        rule.setMessage("Fix these model problems");

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, rule::execute);

        assertTrue(exception.getMessage().contains("Fix these model problems"));
        assertTrue(exception.getMessage().contains("[WARNING] missing plugin version"));
        assertTrue(exception.getMessage().contains("org.example:project:1, pom.xml, line 12, column 4"));
    }

    @Test
    void rejectsUnsupportedProblemValue() {
        RequireNoModelProblems rule = newRule(new InvalidProblemMavenSession(), mock(EnforcerLogger.class));

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, rule::execute);

        assertTrue(exception.getMessage().contains("unsupported problem value"));
    }

    @Test
    void reportsAccessorFailure() {
        RequireNoModelProblems rule = newRule(new FailingModelProblemsMavenSession(), mock(EnforcerLogger.class));

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, rule::execute);

        assertTrue(exception.getMessage().contains("getModelProblems() failed"));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("broken accessor", exception.getCause().getMessage());
    }

    @Test
    void cacheIdIncludesConfiguration() {
        RequireNoModelProblems rule = newRule(newSession(), mock(EnforcerLogger.class));
        String defaultCacheId = rule.getCacheId();

        rule.setMessage("custom message");

        assertNotEquals(defaultCacheId, rule.getCacheId());
    }

    private RequireNoModelProblems newRule(MavenSession session, EnforcerLogger logger) {
        RequireNoModelProblems rule = new RequireNoModelProblems(session);
        rule.setLog(logger);
        return rule;
    }

    private static MavenSession newSession() {
        return new MavenSession(
                null,
                (RepositorySystemSession) null,
                new DefaultMavenExecutionRequest(),
                new DefaultMavenExecutionResult());
    }

    public static class ModelProblemsMavenSession extends MavenSession {

        private final List<ModelProblem> modelProblems;

        ModelProblemsMavenSession(List<ModelProblem> modelProblems) {
            super(
                    null,
                    (RepositorySystemSession) null,
                    new DefaultMavenExecutionRequest(),
                    new DefaultMavenExecutionResult());
            this.modelProblems = modelProblems;
        }

        public List<ModelProblem> getModelProblems() {
            return modelProblems;
        }
    }

    public static final class FailingModelProblemsMavenSession extends ModelProblemsMavenSession {

        FailingModelProblemsMavenSession() {
            super(Collections.emptyList());
        }

        @Override
        public List<ModelProblem> getModelProblems() {
            throw new IllegalStateException("broken accessor");
        }
    }

    public static final class InvalidProblemMavenSession extends MavenSession {

        InvalidProblemMavenSession() {
            super(
                    null,
                    (RepositorySystemSession) null,
                    new DefaultMavenExecutionRequest(),
                    new DefaultMavenExecutionResult());
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public List getModelProblems() {
            return Collections.singletonList("not a model problem");
        }
    }
}
