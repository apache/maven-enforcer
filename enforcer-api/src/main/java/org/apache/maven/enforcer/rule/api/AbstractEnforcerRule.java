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
package org.apache.maven.enforcer.rule.api;

/**
 * Entry point for custom {@code Enforcer Rule}.
 * <p>
 * Please see
 * <a href="https://maven.apache.org/enforcer/enforcer-api/writing-a-custom-rule.html">Writing a custom rule</a>
 *
 * @author Slawomir Jaranowski
 * @since 3.2.1
 */
public abstract class AbstractEnforcerRule extends AbstractEnforcerRuleBase {

    /**
     * Enforcer Rule execution level
     */
    private EnforcerLevel level = EnforcerLevel.ERROR;

    /**
     * Current Enforcer execution level
     *
     * @return an Enforcer execution level
     */
    @Override
    public EnforcerLevel getLevel() {
        return level;
    }

    /**
     * If the rule is to be cached during session scope, whole executing of Maven build,
     * this id is used as part of the key.
     * <p>
     * Rule of the same class and the same cache id will be executed once.
     *
     * @return id to be used by the Enforcer to determine uniqueness of cache results.
     *         Return {@code null} disable cache of rule executing.
     */
    public String getCacheId() {
        return null;
    }

    /**
     * This is the interface into the rule. This method should throw an exception
     * containing a reason message if the rule fails the check. The plugin will
     * then decide based on the fail flag and rule level if it should stop or just log the
     * message as a warning.
     *
     * @throws EnforcerRuleException the enforcer rule exception
     * @throws EnforcerRuleError     in order to brake a build immediately
     */
    public abstract void execute() throws EnforcerRuleException;
}
