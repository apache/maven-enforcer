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

import javax.annotation.PreDestroy;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for manage rules cache storage.
 *
 * @author Slawomir Jaranowski
 * @since 3.2.0
 */
@Named
@Singleton
public class EnforcerRuleCache {

    private final Logger logger = LoggerFactory.getLogger(EnforcerRuleCache.class);

    private final Map<Class<? extends AbstractEnforcerRule>, List<String>> cache = new HashMap<>();

    public boolean isCached(AbstractEnforcerRule rule) {

        String cacheId = rule.getCacheId();

        if (cacheId == null) {
            return false;
        }

        Class<? extends AbstractEnforcerRule> ruleClass = rule.getClass();
        logger.debug("Check cache for {} with id {}", ruleClass, cacheId);

        synchronized (this) {
            List<String> cacheIdList = cache.computeIfAbsent(ruleClass, k -> new ArrayList<>());
            if (cacheIdList.contains(cacheId)) {
                logger.debug("Already cached {} with id {}", ruleClass, cacheId);
                return true;
            }
            logger.debug("Add cache {} with id {}", ruleClass, cacheId);
            cacheIdList.add(cacheId);
        }

        return false;
    }

    @PreDestroy
    public void cleanup() {
        synchronized (this) {
            cache.clear();
        }
    }
}
