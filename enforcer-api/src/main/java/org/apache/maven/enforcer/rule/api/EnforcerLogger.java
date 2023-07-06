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

import java.util.function.Supplier;

/**
 * Logger used by enforcer rule.
 *
 * @author Slawomir Jaranowski
 * @since 3.2.1
 */
public interface EnforcerLogger {

    /**
     * Log message in {@code warn} or {@code error} level according to current rule {@link EnforcerLevel}.
     *
     * @param message a massage to log
     */
    void warnOrError(CharSequence message);

    /**
     * Log message in {@code warn} or {@code error} level according to current rule {@link EnforcerLevel}.
     * <p>
     * {@code messageSupplier} will be evaluate only when corresponding log level is enabled.
     *
     * @param messageSupplier a supplier for message to log
     */
    void warnOrError(Supplier<CharSequence> messageSupplier);

    /**
     * Is the logger instance enabled for the DEBUG level?
     *
     * @return {@code true} if this Logger is enabled for the DEBUG level, {@code false} otherwise.
     * @since 3.4.0
     */
    boolean isDebugEnabled();

    /**
     * Log message in {@code debug} level.
     *
     * @param message a massage to log
     */
    void debug(CharSequence message);

    /**
     * Log message in {@code debug} level.
     * <p>
     * {@code messageSupplier} will be evaluate only when corresponding log level is enabled.
     *
     * @param messageSupplier a supplier for message to log
     */
    void debug(Supplier<CharSequence> messageSupplier);

    /**
     * Is the logger instance enabled for the INFO level?
     *
     * @return {@code true} if this Logger is enabled for the INFO level, {@code false} otherwise.
     * @since 3.4.0
     */
    boolean isInfoEnabled();

    /**
     * Log message in {@code info} level.
     *
     * @param message a massage to log
     */
    void info(CharSequence message);

    /**
     * Log message in {@code info} level.
     * <p>
     * {@code messageSupplier} will be evaluate only when corresponding log level is enabled.
     *
     * @param messageSupplier a supplier for message to log
     */
    void info(Supplier<CharSequence> messageSupplier);

    /**
     * Is the logger instance enabled for the WARN level?
     *
     * @return {@code true} if this Logger is enabled for the WARN level, {@code false} otherwise.
     * @since 3.4.0
     */
    boolean isWarnEnabled();

    /**
     * Log message in {@code warn} level.
     *
     * @param message a massage to log
     */
    void warn(CharSequence message);

    /**
     * Log message in {@code warn} level.
     * <p>
     * {@code messageSupplier} will be evaluate only when corresponding log level is enabled.
     *
     * @param messageSupplier a supplier for message to log
     */
    void warn(Supplier<CharSequence> messageSupplier);

    /**
     * Is the logger instance enabled for the ERROR level?
     *
     * @return {@code true} if this Logger is enabled for the ERROR level, {@code false} otherwise.
     * @since 3.4.0
     */
    boolean isErrorEnabled();

    /**
     * Log message in {@code error} level.
     *
     * @param message a massage to log
     */
    void error(CharSequence message);

    /**
     * Log message in {@code error} level.
     * <p>
     * {@code messageSupplier} will be evaluate only when corresponding log level is enabled.
     *
     * @param messageSupplier a supplier for message to log
     */
    void error(Supplier<CharSequence> messageSupplier);
}
