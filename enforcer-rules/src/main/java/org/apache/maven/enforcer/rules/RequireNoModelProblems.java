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
import java.util.List;
import java.util.Objects;

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

    private static final String ACCESSOR = "getModelProblems";

    private final MavenSession session;

    @Inject
    public RequireNoModelProblems(MavenSession session) {
        this.session = Objects.requireNonNull(session);
    }

    @Override
    public void execute() throws EnforcerRuleException {
        Method accessor;
        try {
            accessor = session.getClass().getMethod(ACCESSOR);
        } catch (NoSuchMethodException e) {
            getLog().warn("MavenSession#getModelProblems() is unavailable; "
                    + "the requireNoModelProblems rule is skipped.");
            return;
        }

        Object result;
        try {
            result = accessor.invoke(session);
        } catch (IllegalAccessException e) {
            throw new EnforcerRuleException("Could not access MavenSession#getModelProblems()", e);
        } catch (InvocationTargetException e) {
            throw new EnforcerRuleException("MavenSession#getModelProblems() failed", e.getCause());
        }

        if (!(result instanceof List)) {
            throw new EnforcerRuleException("MavenSession#getModelProblems() returned an unsupported value");
        }

        List<?> problems = (List<?>) result;
        if (problems.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder();
        if (StringUtils.isNotEmpty(getMessage())) {
            message.append(getMessage());
        } else {
            message.append("Model problems were detected:");
        }

        for (Object problem : problems) {
            if (!(problem instanceof ModelProblem)) {
                throw new EnforcerRuleException(
                        "MavenSession#getModelProblems() returned an unsupported problem value");
            }
            appendProblem(message, (ModelProblem) problem);
        }

        throw new EnforcerRuleException(message.toString());
    }

    private void appendProblem(StringBuilder message, ModelProblem problem) {
        message.append(System.lineSeparator())
                .append("- [")
                .append(problem.getSeverity())
                .append("] ")
                .append(problem.getMessage());
        String location = ModelProblemUtils.formatLocation(problem, null);
        if (StringUtils.isNotEmpty(location)) {
            message.append(" @ ").append(location);
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
