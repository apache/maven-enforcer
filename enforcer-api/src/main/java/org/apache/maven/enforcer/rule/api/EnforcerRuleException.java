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
 * An exception occurring during the execution of a rule.
 * <p>
 * Enforcer plugin takes decision based on configuration and {@code Enforcer Rule} level
 * whether build should pass or fail.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class EnforcerRuleException extends Exception {

    /** serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The source. */
    protected Object source;

    /** The long message. */
    protected String longMessage;

    /**
     * Gets the long message.
     *
     * @return the long message
     * @deprecated not used
     */
    @Deprecated
    public String getLongMessage() {
        return longMessage;
    }

    /**
     * Gets the source.
     *
     * @return the source
     * @deprecated not used
     */
    @Deprecated
    public Object getSource() {
        return source;
    }

    /**
     * Construct a new <code>EnforcerRuleException</code> exception providing
     * the source and a short and long message.
     *
     * @param source       the source
     * @param shortMessage the short message
     * @param longMessage  the long message
     * @deprecated {@code source} and {@code longMessage} are not used
     */
    @Deprecated
    public EnforcerRuleException(Object source, String shortMessage, String longMessage) {
        super(shortMessage);
        this.source = source;
        this.longMessage = longMessage;
    }

    /**
     * Construct a new <code>EnforcerRuleException</code> exception wrapping
     * an underlying <code>Exception</code> and providing a
     * <code>message</code>.
     *
     * @param message the message
     * @param cause   the cause
     */
    public EnforcerRuleException(String message, Exception cause) {
        super(message, cause);
    }

    /**
     * Construct a new <code>EnforcerRuleException</code> exception wrapping
     * an underlying <code>Throwable</code> and providing a
     * <code>message</code>.
     *
     * @param message the message
     * @param cause   the cause
     */
    public EnforcerRuleException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a new <code>EnforcerRuleException</code> exception providing
     * a <code>message</code>.
     *
     * @param message the message
     */
    public EnforcerRuleException(String message) {
        super(message);
    }

    /**
     * Construct a new <code>EnforcerRuleException</code> exception wrapping
     * an underlying <code>Throwable</code>.
     *
     * @param cause the cause
     */
    public EnforcerRuleException(Throwable cause) {
        super(cause);
    }
}
