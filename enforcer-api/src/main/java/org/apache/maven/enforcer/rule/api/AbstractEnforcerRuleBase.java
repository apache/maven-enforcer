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
 * Base rule implementation for new API.
 * <p>
 * Used for internal purpose.
 *
 * @author Slawomir Jaranowski
 * @since 3.2.1
 */
abstract class AbstractEnforcerRuleBase implements EnforcerRuleBase {

    /**
     * EnforcerLogger instance
     */
    private EnforcerLogger log;

    /**
     * Used by {@code EnforcerMojo} to inject logger instance
     *
     * @param log an {@link EnforcerLogger} instance
     */
    @Override
    public void setLog(EnforcerLogger log) {
        this.log = log;
    }

    /**
     * Provide an {@link  EnforcerLogger} instance for Rule
     * <p>
     * <b>NOTICE</b> A logger is not available in constructors.
     *
     * @return an {@link EnforcerLogger} instance
     */
    public EnforcerLogger getLog() {
        return log;
    }
}
