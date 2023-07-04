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

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Entry point for custom {@code Enforcer Rule} which provide additional rules configuration.
 * <p>
 * Provided configuration will be added to current rules list by {@code Enforcer Mojo}
 *
 * @author Slawomir Jaranowski
 * @since 3.2.1
 */
public abstract class AbstractEnforcerRuleConfigProvider extends AbstractEnforcerRuleBase {

    /**
     * Produce rule configuration.
     * <p>
     * Returned configuration must contain rules configuration as in example:
     * <pre>
     *     &lt;rules&gt;
     *         &lt;ruleName/&gt;
     *         &lt;ruleName&gt;
     *             &lt;ruleConfig&gt;config value&lt;/ruleConfig&gt;
     *         &lt;/ruleName&gt;
     *     &lt;/rules&gt;
     * </pre>
     *
     * @return a rules configuration
     * @throws EnforcerRuleError the error during executing
     */
    public abstract Xpp3Dom getRulesConfig() throws EnforcerRuleError;
}
