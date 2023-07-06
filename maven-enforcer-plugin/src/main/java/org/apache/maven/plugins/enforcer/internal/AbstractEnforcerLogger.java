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

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.plugin.logging.Log;

/**
 * Base EnforcerLogger implementation
 *
 * @author Slawomir Jaranowski
 * @since 3.2.0
 */
public abstract class AbstractEnforcerLogger implements EnforcerLogger {

    protected final Log log;

    protected AbstractEnforcerLogger(Log log) {
        this.log = Objects.requireNonNull(log, "log must be not null");
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(CharSequence message) {
        log.debug(message);
    }

    @Override
    public void debug(Supplier<CharSequence> messageSupplier) {
        if (log.isDebugEnabled()) {
            log.debug(messageSupplier.get());
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void info(CharSequence message) {
        log.info(message);
    }

    @Override
    public void info(Supplier<CharSequence> messageSupplier) {
        if (log.isInfoEnabled()) {
            log.info(messageSupplier.get());
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public void warn(CharSequence message) {
        log.warn(message);
    }

    @Override
    public void warn(Supplier<CharSequence> messageSupplier) {
        if (log.isWarnEnabled()) {
            log.warn(messageSupplier.get());
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void error(CharSequence message) {
        log.error(message);
    }

    @Override
    public void error(Supplier<CharSequence> messageSupplier) {
        if (log.isErrorEnabled()) {
            log.error(messageSupplier.get());
        }
    }
}
