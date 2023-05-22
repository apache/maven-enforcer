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

import bsh.EvalError;
import bsh.Interpreter;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

/**
 * Rule for Maven Enforcer using Beanshell to evaluate a conditional expression.
 *
 * @author hugonnem
 */
@Named("evaluateBeanshell")
public final class EvaluateBeanshell extends AbstractStandardEnforcerRule {

    /** Beanshell interpreter. */
    private final Interpreter interpreter = new Interpreter();

    /** The condition to be evaluated.
     * */
    private String condition;

    private final ExpressionEvaluator evaluator;

    @Inject
    public EvaluateBeanshell(ExpressionEvaluator evaluator) {
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

        try {
            getLog().debug("Echo condition : " + condition);
            // Evaluate condition within Plexus Container
            String script = (String) evaluator.evaluate(condition);
            getLog().debug("Echo script : " + script);
            if (!evaluateCondition(script)) {
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

    /**
     * Evaluate expression using Beanshell.
     *
     * @param script the expression to be evaluated
     * @return boolean the evaluation of the expression
     * @throws EnforcerRuleException if the script could not be evaluated
     */
    private boolean evaluateCondition(String script) throws EnforcerRuleException {
        Boolean evaluation;
        try {
            evaluation = (Boolean) interpreter.eval(script);
            getLog().debug("Echo evaluating : " + evaluation);
        } catch (EvalError ex) {
            throw new EnforcerRuleException("Couldn't evaluate condition: " + script, ex);
        }
        return evaluation;
    }

    @Override
    public String toString() {
        return String.format("EvaluateBeanshell[message=%s, condition=%s]", getMessage(), condition);
    }
}
