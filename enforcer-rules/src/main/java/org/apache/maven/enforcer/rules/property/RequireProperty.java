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
package org.apache.maven.enforcer.rules.property;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Objects;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.ExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * This rule checks that certain properties are set.
 *
 * @author Paul Gier
 */
@Named("requireProperty")
public final class RequireProperty extends AbstractPropertyEnforcerRule {

    /**
     * Specify the required property.
     */
    private String property = null;

    private final ExpressionEvaluator evaluator;

    @Inject
    public RequireProperty(ExpressionEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    @Override
    public Object resolveValue() throws EnforcerRuleException {
        try {
            return evaluator.evaluate("${" + property + "}");
        } catch (ExpressionEvaluationException e) {
            throw new EnforcerRuleException(e);
        }
    }

    @Override
    public String getPropertyName() {
        return property;
    }

    @Override
    public String getName() {
        return "Property";
    }

    @Override
    public String toString() {
        return String.format(
                "RequireProperty[message=%s, property=%s, regex=%s, regexMessage=%s]",
                getMessage(), property, getRegex(), getRegexMessage());
    }
}
