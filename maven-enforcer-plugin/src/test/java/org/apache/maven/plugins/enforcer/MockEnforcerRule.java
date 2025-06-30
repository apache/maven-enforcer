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
package org.apache.maven.plugins.enforcer;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class MockEnforcerRule implements EnforcerRule {

    private boolean failRule = false;

    private String cacheId = "";

    private boolean isCacheable = false;

    private boolean isResultValid = false;

    private boolean executed = false;

    public MockEnforcerRule(boolean fail) {
        this.failRule = fail;
    }

    public MockEnforcerRule(boolean fail, String cacheId, boolean isCacheable, boolean isResultValid) {
        this.failRule = fail;
        this.isCacheable = isCacheable;
        this.isResultValid = isResultValid;
        this.cacheId = cacheId;
    }

    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        executed = true;
        if (isFailRule()) {
            throw new EnforcerRuleException(" this condition is not allowed.");
        }
    }

    /**
     * @return the failRule
     */
    public boolean isFailRule() {
        return this.failRule;
    }

    /**
     * @param theFailRule the failRule to set
     */
    public void setFailRule(boolean theFailRule) {
        this.failRule = theFailRule;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#getCacheId()
     */
    public String getCacheId() {
        return cacheId;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isCacheable()
     */
    public boolean isCacheable() {
        return isCacheable;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.maven.enforcer.rule.api.EnforcerRule#isResultValid(org.apache.maven.enforcer.rule.api.EnforcerRule)
     */
    public boolean isResultValid(EnforcerRule theCachedRule) {
        return isResultValid;
    }

    /**
     * Checks if the rule got executed.
     *
     * @return {@code true} if executed, {@code false} otherwise.
     */
    public boolean isExecuted() {
        return executed;
    }
}
