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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface to be implemented by any rules executed by the enforcer.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @deprecated Please see
 *         <a href="https://maven.apache.org/enforcer/enforcer-api/writing-a-custom-rule.html">Writing a custom rule</a>
 */
@Deprecated
public interface EnforcerRule extends EnforcerRuleBase {

    /**
     * This is the interface into the rule. This method should throw an exception
     * containing a reason message if the rule fails the check. The plugin will
     * then decide based on the fail flag if it should stop or just log the
     * message as a warning.
     *
     * @param helper The helper provides access to the log, MavenSession and has
     *               helpers to get common components. It is also able to lookup components
     *               by class name.
     * @throws EnforcerRuleException the enforcer rule exception
     */
    void execute(@Nonnull EnforcerRuleHelper helper) throws EnforcerRuleException;

    /**
     * This method tells the enforcer if the rule results may be cached. If the result is true,
     * the results will be remembered for future executions in the same build (ie children). Subsequent
     * iterations of the rule will be queried to see if they are also cacheable. This will allow the rule to be
     * uncached further down the tree if needed.
     *
     * @return <code>true</code> if rule is cacheable
     */
    boolean isCacheable();

    /**
     * If the rule is cacheable and the same id is found in the cache, the stored results are passed to this method to
     * allow double checking of the results. Most of the time this can be done by generating unique ids, but sometimes
     * the results of objects returned by the helper need to be queried. You may for example, store certain objects in
     * your rule and then query them later.
     *
     * @param cachedRule the last cached instance of the rule. This is to be used by the rule to
     *                   potentially determine if the results are still valid (ie if the configuration has been
     *                   overridden)
     * @return <code>true</code> if the stored results are valid for the same id.
     */
    boolean isResultValid(@Nonnull EnforcerRule cachedRule);

    /**
     * If the rule is to be cached, this id is used as part of the key. This can allow rules to take parameters
     * that allow multiple results of the same rule to be cached.
     *
     * @return id to be used by the enforcer to determine uniqueness of cache results. The ids only need to be unique
     *         within a given rule implementation as the full key will be [classname]-[id]
     */
    @Nullable
    String getCacheId();
}
