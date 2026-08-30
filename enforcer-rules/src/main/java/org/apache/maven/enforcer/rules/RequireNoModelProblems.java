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

import javax.inject.Inject;
import javax.inject.Named;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Requires all reactor models to be free of model-building problems.
 */
@Named("requireNoModelProblems")
public final class RequireNoModelProblems extends AbstractStandardEnforcerRule {

    private static final String MAVEN_4_SESSION_ACCESSOR = "getSession";
    private static final String MAVEN_4_COLLECTOR_ACCESSOR = "getModelProblemCollector";
    private static final String MAVEN_3_PROBLEMS_ACCESSOR = "getModelProblems";

    private final Object session;

    @Inject
    public RequireNoModelProblems(MavenSession session) {
        this((Object) session);
    }

    RequireNoModelProblems(Object session) {
        this.session = Objects.requireNonNull(session);
    }

    @Override
    public void execute() throws EnforcerRuleException {
        Optional<List<ModelProblemData>> discoveredProblems = getMaven4ModelProblems();
        if (!discoveredProblems.isPresent()) {
            discoveredProblems = getMaven3ModelProblems();
        }
        if (!discoveredProblems.isPresent()) {
            getLog().warn("This Maven version does not expose retained model problems; "
                    + "the requireNoModelProblems rule is skipped.");
            return;
        }

        List<ModelProblemData> problems = discoveredProblems.get();
        if (problems.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder();
        if (StringUtils.isNotEmpty(getMessage())) {
            message.append(getMessage());
        } else {
            message.append("Model problems were detected:");
        }

        for (ModelProblemData problem : problems) {
            appendProblem(message, problem);
        }

        throw new EnforcerRuleException(message.toString());
    }

    private Optional<List<ModelProblemData>> getMaven4ModelProblems() throws EnforcerRuleException {
        Method sessionAccessor = findAccessor(session, MAVEN_4_SESSION_ACCESSOR);
        if (sessionAccessor == null) {
            return Optional.empty();
        }

        Object apiSession = invoke(sessionAccessor, session, "MavenSession#getSession()");
        if (apiSession == null) {
            return Optional.empty();
        }

        Method collectorAccessor = findAccessor(apiSession, MAVEN_4_COLLECTOR_ACCESSOR);
        if (collectorAccessor == null) {
            return Optional.empty();
        }

        Object collector = invoke(collectorAccessor, apiSession, "Session#getModelProblemCollector()");
        if (collector == null) {
            throw new EnforcerRuleException("Session#getModelProblemCollector() returned null");
        }

        Object overflow = invoke(
                requireAccessor(collector, "problemsOverflow", "Maven 4 model problem collector"),
                collector,
                "ProblemCollector#problemsOverflow()");
        if (!(overflow instanceof Boolean)) {
            throw new EnforcerRuleException("ProblemCollector#problemsOverflow() returned an unsupported value");
        }

        Object result = invoke(
                requireAccessor(collector, "problems", "Maven 4 model problem collector"),
                collector,
                "ProblemCollector#problems()");
        if (!(result instanceof Stream)) {
            throw new EnforcerRuleException("ProblemCollector#problems() returned an unsupported value");
        }

        List<ModelProblemData> problems = new ArrayList<>();
        if ((Boolean) overflow) {
            problems.add(new ModelProblemData(
                    "WARNING",
                    "Too many model problems reported (listed problems are just a subset of reported problems)",
                    ""));
        }
        try (Stream<?> stream = (Stream<?>) result) {
            Iterator<?> iterator = stream.iterator();
            while (iterator.hasNext()) {
                problems.add(toMaven4ModelProblem(iterator.next()));
            }
        }
        return Optional.of(problems);
    }

    private Optional<List<ModelProblemData>> getMaven3ModelProblems() throws EnforcerRuleException {
        Method accessor = findAccessor(session, MAVEN_3_PROBLEMS_ACCESSOR);
        if (accessor == null) {
            return Optional.empty();
        }

        Object result = invoke(accessor, session, "MavenSession#getModelProblems()");
        if (!(result instanceof List)) {
            throw new EnforcerRuleException("MavenSession#getModelProblems() returned an unsupported value");
        }

        List<ModelProblemData> problems = new ArrayList<>();
        for (Object problem : (List<?>) result) {
            if (!(problem instanceof ModelProblem)) {
                throw new EnforcerRuleException(
                        "MavenSession#getModelProblems() returned an unsupported problem value");
            }
            ModelProblem modelProblem = (ModelProblem) problem;
            problems.add(new ModelProblemData(
                    modelProblem.getSeverity().toString(),
                    modelProblem.getMessage(),
                    ModelProblemUtils.formatLocation(modelProblem, null)));
        }
        return Optional.of(problems);
    }

    private ModelProblemData toMaven4ModelProblem(Object problem) throws EnforcerRuleException {
        if (problem == null) {
            throw new EnforcerRuleException("ProblemCollector#problems() returned a null problem");
        }

        Object severity = invoke(
                requireAccessor(problem, "getSeverity", "Maven 4 model problem"),
                problem,
                "ModelProblem#getSeverity()");
        if (severity == null) {
            throw new EnforcerRuleException("ModelProblem#getSeverity() returned null");
        }

        String message = getString(problem, "getMessage");
        String modelId = getString(problem, "getModelId");
        String source = getString(problem, "getSource");
        int lineNumber = getInt(problem, "getLineNumber");
        int columnNumber = getInt(problem, "getColumnNumber");
        return new ModelProblemData(
                severity.toString(), message, formatLocation(modelId, source, lineNumber, columnNumber));
    }

    private String getString(Object target, String accessorName) throws EnforcerRuleException {
        Object value = invoke(
                requireAccessor(target, accessorName, "Maven 4 model problem"),
                target,
                "ModelProblem#" + accessorName + "()");
        if (!(value instanceof String)) {
            throw new EnforcerRuleException("ModelProblem#" + accessorName + "() returned an unsupported value");
        }
        return (String) value;
    }

    private int getInt(Object target, String accessorName) throws EnforcerRuleException {
        Object value = invoke(
                requireAccessor(target, accessorName, "Maven 4 model problem"),
                target,
                "ModelProblem#" + accessorName + "()");
        if (!(value instanceof Number)) {
            throw new EnforcerRuleException("ModelProblem#" + accessorName + "() returned an unsupported value");
        }
        return ((Number) value).intValue();
    }

    private Method findAccessor(Object target, String accessorName) {
        try {
            return target.getClass().getMethod(accessorName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Method requireAccessor(Object target, String accessorName, String targetDescription)
            throws EnforcerRuleException {
        Method accessor = findAccessor(target, accessorName);
        if (accessor == null) {
            throw new EnforcerRuleException(targetDescription + " does not provide " + accessorName + "()");
        }
        return accessor;
    }

    private Object invoke(Method accessor, Object target, String description) throws EnforcerRuleException {
        try {
            return accessor.invoke(target);
        } catch (IllegalAccessException e) {
            throw new EnforcerRuleException("Could not access " + description, e);
        } catch (InvocationTargetException e) {
            throw new EnforcerRuleException(description + " failed", e.getCause());
        }
    }

    private static String formatLocation(String modelId, String source, int lineNumber, int columnNumber) {
        StringBuilder location = new StringBuilder();
        location.append(modelId);
        if (!source.isEmpty()) {
            if (location.length() > 0) {
                location.append(", ");
            }
            location.append(source);
        }
        if (lineNumber > 0) {
            if (location.length() > 0) {
                location.append(", ");
            }
            location.append("line ").append(lineNumber);
        }
        if (columnNumber > 0) {
            if (location.length() > 0) {
                location.append(", ");
            }
            location.append("column ").append(columnNumber);
        }
        return location.toString();
    }

    private void appendProblem(StringBuilder message, ModelProblemData problem) {
        message.append(System.lineSeparator())
                .append("- [")
                .append(problem.severity)
                .append("] ")
                .append(problem.message);
        if (StringUtils.isNotEmpty(problem.location)) {
            message.append(" @ ").append(problem.location);
        }
    }

    private static final class ModelProblemData {

        private final String severity;
        private final String message;
        private final String location;

        private ModelProblemData(String severity, String message, String location) {
            this.severity = severity;
            this.message = message;
            this.location = location;
        }
    }

    @Override
    public String getCacheId() {
        return String.valueOf(toString().hashCode());
    }

    @Override
    public String toString() {
        return String.format("RequireNoModelProblems[message=%s]", getMessage());
    }
}
