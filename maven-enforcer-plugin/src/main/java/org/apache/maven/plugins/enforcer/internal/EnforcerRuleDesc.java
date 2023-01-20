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
package org.apache.maven.plugins.enforcer.internal;

import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerRuleBase;

/**
 * Description of rule to execute.
 *
 * @author Slawomir Jaranowski
 * @since 3.2.0
 */
public final class EnforcerRuleDesc {

    private final String name;

    private final EnforcerRuleBase rule;

    /**
     * Create a new Rule Description
     *
     * @param name  a rule name
     * @param rule  a rule instance
     */
    public EnforcerRuleDesc(String name, EnforcerRuleBase rule) {
        this.name = name;
        this.rule = rule;
    }

    public String getName() {
        return name;
    }

    public EnforcerRuleBase getRule() {
        return rule;
    }

    public EnforcerLevel getLevel() {
        return rule.getLevel();
    }

    @Override
    public String toString() {
        return String.format("EnforcerRuleDesc[name=%s, rule=%s, level=%s]", name, rule, getLevel());
    }
}
