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
import java.util.stream.Stream;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelProblem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequireNoModelProblemsTest {

    @Test
    void skipsUnsupportedMavenWithWarning() {
        EnforcerLogger logger = mock(EnforcerLogger.class);
        RequireNoModelProblems rule = newRule(new Object(), logger);

        assertDoesNotThrow(rule::execute);

        verify(logger).warn(contains("does not expose retained model problems"));
    }

    @Test
    void passesWhenMaven3HasNoModelProblems() {
        RequireNoModelProblems rule = newRule(new Maven3Session(Collections.emptyList()), mock(EnforcerLogger.class));

        assertDoesNotThrow(rule::execute);
    }

    @Test
    void reportsMaven3ModelProblems() {
        RequireNoModelProblems rule =
                newRule(new Maven3Session(Collections.singletonList(newMaven3Problem())), mock(EnforcerLogger.class));
        rule.setMessage("Fix these model problems");

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, rule::execute);

        assertTrue(exception.getMessage().contains("Fix these model problems"));
        assertTrue(exception.getMessage().contains("[WARNING] missing plugin version"));
        assertTrue(exception.getMessage().contains("org.example:project:1, pom.xml, line 12, column 4"));
    }

    @Test
    void rejectsUnsupportedMaven3ProblemValue() {
        RequireNoModelProblems rule = newRule(new InvalidMaven3Session(), mock(EnforcerLogger.class));

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, rule::execute);

        assertTrue(exception.getMessage().contains("unsupported problem value"));
    }

    @Test
    void reportsMaven3AccessorFailure() {
        RequireNoModelProblems rule = newRule(new FailingMaven3Session(), mock(EnforcerLogger.class));

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, rule::execute);

        assertTrue(exception.getMessage().contains("MavenSession#getModelProblems() failed"));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("broken Maven 3 accessor", exception.getCause().getMessage());
    }

    @Test
    void passesWhenMaven4HasNoModelProblems() {
        RequireNoModelProblems rule =
                newRule(newMaven4Session(false, Collections.emptyList()), mock(EnforcerLogger.class));

        assertDoesNotThrow(rule::execute);
    }

    @Test
    void reportsMaven4ModelProblems() {
        RequireNoModelProblems rule = newRule(
                newMaven4Session(false, Collections.singletonList(new NativeModelProblem())),
                mock(EnforcerLogger.class));

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, rule::execute);

        assertTrue(exception.getMessage().contains("[WARNING] missing plugin version"));
        assertTrue(exception.getMessage().contains("org.example:project:1, pom.xml, line 12, column 4"));
    }

    @Test
    void reportsMaven4CollectorOverflow() {
        RequireNoModelProblems rule =
                newRule(newMaven4Session(true, Collections.emptyList()), mock(EnforcerLogger.class));

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, rule::execute);

        assertTrue(exception.getMessage().contains("listed problems are just a subset"));
    }

    @Test
    void prefersMaven4ProblemCollectorWhenBothPathsExist() {
        Maven4ApiSession apiSession = new Maven4ApiSession(new Maven4ProblemCollector(false, Collections.emptyList()));
        DualPathSession session = new DualPathSession(apiSession, Collections.singletonList(newMaven3Problem()));
        RequireNoModelProblems rule = newRule(session, mock(EnforcerLogger.class));

        assertDoesNotThrow(rule::execute);
    }

    @Test
    void rejectsUnsupportedMaven4ProblemValue() {
        RequireNoModelProblems rule =
                newRule(newMaven4Session(false, Collections.singletonList(new Object())), mock(EnforcerLogger.class));

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, rule::execute);

        assertTrue(exception.getMessage().contains("does not provide getSeverity()"));
    }

    @Test
    void reportsMaven4AccessorFailure() {
        RequireNoModelProblems rule =
                newRule(new Maven4Session(new FailingMaven4ApiSession()), mock(EnforcerLogger.class));

        EnforcerRuleException exception = assertThrows(EnforcerRuleException.class, rule::execute);

        assertTrue(exception.getMessage().contains("Session#getModelProblemCollector() failed"));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("broken Maven 4 accessor", exception.getCause().getMessage());
    }

    @Test
    void cacheIdIncludesConfiguration() {
        RequireNoModelProblems rule = newRule(new Object(), mock(EnforcerLogger.class));
        String defaultCacheId = rule.getCacheId();

        rule.setMessage("custom message");

        assertNotEquals(defaultCacheId, rule.getCacheId());
    }

    private RequireNoModelProblems newRule(Object session, EnforcerLogger logger) {
        RequireNoModelProblems rule = new RequireNoModelProblems(session);
        rule.setLog(logger);
        return rule;
    }

    private static Maven4Session newMaven4Session(boolean overflow, List<?> problems) {
        return new Maven4Session(new Maven4ApiSession(new Maven4ProblemCollector(overflow, problems)));
    }

    private static ModelProblem newMaven3Problem() {
        return new DefaultModelProblem(
                "missing plugin version",
                ModelProblem.Severity.WARNING,
                ModelProblem.Version.BASE,
                "pom.xml",
                12,
                4,
                "org.example:project:1",
                null);
    }

    public static class Maven3Session {

        private final List<?> modelProblems;

        Maven3Session(List<?> modelProblems) {
            this.modelProblems = modelProblems;
        }

        public List<?> getModelProblems() {
            return modelProblems;
        }
    }

    public static final class FailingMaven3Session extends Maven3Session {

        FailingMaven3Session() {
            super(Collections.emptyList());
        }

        @Override
        public List<?> getModelProblems() {
            throw new IllegalStateException("broken Maven 3 accessor");
        }
    }

    public static final class InvalidMaven3Session extends Maven3Session {

        InvalidMaven3Session() {
            super(Collections.singletonList("not a model problem"));
        }
    }

    public static class Maven4Session {

        private final Object apiSession;

        Maven4Session(Object apiSession) {
            this.apiSession = apiSession;
        }

        public Object getSession() {
            return apiSession;
        }
    }

    public static final class DualPathSession extends Maven4Session {

        private final List<?> modelProblems;

        DualPathSession(Object apiSession, List<?> modelProblems) {
            super(apiSession);
            this.modelProblems = modelProblems;
        }

        public List<?> getModelProblems() {
            return modelProblems;
        }
    }

    public static class Maven4ApiSession {

        private final Object problemCollector;

        Maven4ApiSession(Object problemCollector) {
            this.problemCollector = problemCollector;
        }

        public Object getModelProblemCollector() {
            return problemCollector;
        }
    }

    public static final class FailingMaven4ApiSession extends Maven4ApiSession {

        FailingMaven4ApiSession() {
            super(null);
        }

        @Override
        public Object getModelProblemCollector() {
            throw new IllegalStateException("broken Maven 4 accessor");
        }
    }

    public static final class Maven4ProblemCollector {

        private final boolean overflow;
        private final List<?> problems;

        Maven4ProblemCollector(boolean overflow, List<?> problems) {
            this.overflow = overflow;
            this.problems = problems;
        }

        public boolean problemsOverflow() {
            return overflow;
        }

        public Stream<?> problems() {
            return problems.stream();
        }
    }

    public static final class NativeModelProblem {

        public NativeSeverity getSeverity() {
            return NativeSeverity.WARNING;
        }

        public String getMessage() {
            return "missing plugin version";
        }

        public String getModelId() {
            return "org.example:project:1";
        }

        public String getSource() {
            return "pom.xml";
        }

        public int getLineNumber() {
            return 12;
        }

        public int getColumnNumber() {
            return 4;
        }
    }

    public enum NativeSeverity {
        WARNING
    }
}
