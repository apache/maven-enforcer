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

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * This is the interface that all helpers will use. This
 * provides access to the log, session and components to the
 * rules.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @deprecated Please see
 *         <a href="https://maven.apache.org/enforcer/enforcer-api/writing-a-custom-rule.html">Writing a custom rule</a>
 */
@Deprecated
public interface EnforcerRuleHelper extends ExpressionEvaluator {

    /**
     * Gets the log.
     *
     * @return the log
     */
    @Nonnull
    Log getLog();

    /**
     * Gets the component.
     *
     * @param clazz the clazz
     * @param <T> a class type
     * @return the component
     * @throws ComponentLookupException the component lookup exception
     */
    @Nonnull
    <T> T getComponent(Class<T> clazz) throws ComponentLookupException;

    /**
     * Gets the component.
     *
     * @param componentKey the component key
     * @return the component
     * @throws ComponentLookupException the component lookup exception
     */
    @Nonnull
    Object getComponent(String componentKey) throws ComponentLookupException;

    /**
     * Gets the component.
     *
     * @param role     the role
     * @param roleHint the role hint
     * @return the component
     * @throws ComponentLookupException the component lookup exception
     */
    Object getComponent(String role, String roleHint) throws ComponentLookupException;

    /**
     * Gets the component.
     *
     * @param clazz    the clazz
     * @param roleHint the role hint
     * @param <T> a class type
     * @return the component
     * @throws ComponentLookupException the component lookup exception
     */
    <T> T getComponent(Class<T> clazz, String roleHint) throws ComponentLookupException;

    /**
     * Gets the component map.
     *
     * @param role the role
     * @return the component map
     * @throws ComponentLookupException the component lookup exception
     */
    Map<String, ?> getComponentMap(String role) throws ComponentLookupException;

    /**
     * Gets the component list.
     *
     * @param role the role
     * @return the component list
     * @throws ComponentLookupException the component lookup exception
     */
    List<?> getComponentList(String role) throws ComponentLookupException;

    /**
     * Gets the container.
     *
     * @return the container
     */
    PlexusContainer getContainer();

    /**
     * Gets a cached value, or uses the provided producer to compute it.
     *
     * @param key      a key to identify the value stored
     * @param producer a supplier for the value if it's not already present
     * @return a previously-cached or freshly-computed value
     */
    Object getCache(String key, Supplier<?> producer);
}
