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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.AbstractStandardEnforcerRule;

/**
 * Abstract enforcer rule that give a foundation to validate properties from multiple sources.
 *
 * @author Paul Gier
 * @author <a href='mailto:marvin[at]marvinformatics[dot]com'>Marvin Froeder</a>
 */
abstract class AbstractPropertyEnforcerRule extends AbstractStandardEnforcerRule {

    /**
     * Match the property value to a given regular expression. Defaults to <code>null</code> (any value is ok).
     *
     * @see {@link #setRegex(String)}
     * @see {@link #getRegex()}
     */
    private String regex = null;

    /**
     * Specify a warning message if the regular expression is not matched.
     *
     * @see {@link #setRegexMessage(String)}
     * @see {@link #getRegexMessage()}
     */
    private String regexMessage = null;

    /**
     * Set the property value to a given regular expression. Defaults to <code>null</code> (any value is ok).
     *
     * @param regex The regular expression
     */
    public final void setRegex(String regex) {
        this.regex = regex;
    }

    /**
     * Get the property value to a given regular expression. Defaults to <code>null</code> (any value is ok).
     *
     * @return the regular expression
     */
    public final String getRegex() {
        return regex;
    }

    /**
     * Set a warning message if the regular expression is not matched.
     *
     * @param regexMessage the regex message
     */
    public final void setRegexMessage(String regexMessage) {
        this.regexMessage = regexMessage;
    }

    /**
     * Get a warning message if the regular expression is not matched.
     *
     * @return the regex message
     */
    public final String getRegexMessage() {
        return regexMessage;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        Object propValue = resolveValue();

        // Check that the property is not null or empty string
        if (propValue == null) {
            String message = getMessage();
            if (message == null) {
                message = getName() + " \"" + getPropertyName() + "\" is required for this build.";
            }
            throw new EnforcerRuleException(message);
        }
        // If there is a regex, check that the property matches it
        if (regex != null && !propValue.toString().matches(regex)) {
            if (regexMessage == null) {
                regexMessage = getName() + " \"" + getPropertyName() + "\" evaluates to \"" + propValue + "\".  "
                        + "This does not match the regular expression \"" + regex + "\"";
            }
            throw new EnforcerRuleException(regexMessage);
        }
    }

    /**
     * How the property that is being evaluated is called
     * @return kind of property
     */
    protected abstract String getName();

    /**
     * The name of the property currently being evaluated, this is used for default message purposes only
     *
     * @return the name of the property
     */
    protected abstract String getPropertyName();

    /**
     * Resolves the property value
     *
     * @return a resolve value
     * @throws EnforcerRuleException in case of problems
     */
    protected abstract Object resolveValue() throws EnforcerRuleException;
}
