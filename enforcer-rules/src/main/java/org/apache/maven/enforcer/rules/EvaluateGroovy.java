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

import java.util.Objects;

import groovy.lang.GroovyShell;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

/**
 * Rule for Maven Enforcer using Groovy to evaluate a conditional expression.
 * <p>
 * Groovy is not shipped with the plugin: add {@code org.apache.groovy:groovy} as a dependency of the
 * {@code maven-enforcer-plugin} declaration to use this rule.
 *
 * @since 3.6.4
 */
@Named("evaluateGroovy")
public final class EvaluateGroovy extends AbstractStandardEnforcerRule {

    /** The condition to be evaluated. */
    private String condition;

    private final ExpressionEvaluator evaluator;

    @Inject
    public EvaluateGroovy(ExpressionEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator);
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        if (condition == null || condition.trim().isEmpty()) {
            throw new EnforcerRuleError("The evaluateGroovy rule requires a condition");
        }
        try {
            Class.forName("groovy.lang.GroovyShell", false, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new EnforcerRuleError("The evaluateGroovy rule needs Groovy on the plugin classpath: add"
                    + " org.apache.groovy:groovy as a dependency of the maven-enforcer-plugin declaration");
        }

        try {
            getLog().debug("Echo condition : " + condition);
            // interpolate ${...} against the project before handing the script to Groovy
            String script = (String) evaluator.evaluate(condition);
            getLog().debug("Echo script : " + script);
            if (!GroovyEvaluation.evaluate(script)) {
                String message = getMessage();
                if (message == null || message.isEmpty()) {
                    message = "The expression \"" + condition + "\" is not true.";
                }
                throw new EnforcerRuleException(message);
            }
        } catch (ExpressionEvaluationException e) {
            throw new EnforcerRuleException("Unable to evaluate an expression '" + condition + "'", e);
        }
    }

    @Override
    public String toString() {
        return String.format("EvaluateGroovy[message=%s, condition=%s]", getMessage(), condition);
    }

    /**
     * Kept apart so that the rule class itself loads without Groovy on the classpath and can report that.
     */
    private static final class GroovyEvaluation {
        private GroovyEvaluation() {}

        static boolean evaluate(String script) throws EnforcerRuleException {
            Object result;
            try {
                result = new GroovyShell().evaluate(script);
            } catch (RuntimeException | LinkageError ex) {
                throw new EnforcerRuleException("Couldn't evaluate condition: " + script, ex);
            }
            if (!(result instanceof Boolean)) {
                throw new EnforcerRuleException("The condition must evaluate to a boolean but gave " + result + " ("
                        + (result == null ? "null" : result.getClass().getName()) + "): " + script);
            }
            return (Boolean) result;
        }
    }
}
