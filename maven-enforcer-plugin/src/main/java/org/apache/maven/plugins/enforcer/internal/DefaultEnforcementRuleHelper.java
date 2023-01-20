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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Default implementation of the EnforcementRuleHelper interface. This is used to help retrieve information from the
 * session and provide useful elements like the log.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class DefaultEnforcementRuleHelper implements EnforcerRuleHelper {

    /** The log. */
    private Log log;

    /** The evaluator. */
    private ExpressionEvaluator evaluator;

    /** The container. */
    private PlexusContainer container;

    /** A cache. */
    private Map<String, Object> cache;

    /**
     * Instantiates a new default enforcement rule helper.
     *
     * @param session the session
     * @param evaluator the evaluator
     * @param log the log
     * @param container the container
     */
    public DefaultEnforcementRuleHelper(
            MavenSession session, ExpressionEvaluator evaluator, Log log, PlexusContainer container) {
        this.evaluator = evaluator;
        this.log = log;
        if (container != null) {
            this.container = container;
        } else {
            this.container = session.getContainer();
        }

        this.cache = new HashMap<>();
    }

    @Override
    public Log getLog() {
        return log;
    }

    @Override
    public File alignToBaseDirectory(File theFile) {
        return evaluator.alignToBaseDirectory(theFile);
    }

    @Override
    public Object evaluate(String theExpression) throws ExpressionEvaluationException {
        return evaluator.evaluate(theExpression);
    }

    @Override
    public <T> T getComponent(Class<T> clazz) throws ComponentLookupException {
        return container.lookup(clazz);
    }

    @Override
    public Object getComponent(String theComponentKey) throws ComponentLookupException {
        return container.lookup(theComponentKey);
    }

    @Override
    public Object getComponent(String theRole, String theRoleHint) throws ComponentLookupException {
        return container.lookup(theRole, theRoleHint);
    }

    @Override
    public List<Object> getComponentList(String theRole) throws ComponentLookupException {
        return container.lookupList(theRole);
    }

    @Override
    public Map<String, Object> getComponentMap(String theRole) throws ComponentLookupException {
        return container.lookupMap(theRole);
    }

    @Override
    public <T> T getComponent(Class<T> clazz, String roleHint) throws ComponentLookupException {
        return container.lookup(clazz, roleHint);
    }

    @Override
    public PlexusContainer getContainer() {
        return container;
    }

    @Override
    public Object getCache(String key, Supplier<?> producer) {
        return cache.computeIfAbsent(key, (x) -> producer.get());
    }
}
