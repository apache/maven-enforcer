package org.apache.maven.plugins.enforcer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerRule2;

/**
 * The Class AbstractStandardEnforcerRule.
 */
public abstract class AbstractStandardEnforcerRule
    implements EnforcerRule2
{

    /**
     * Specify a friendly message if the rule fails.
     *
     * @see {@link #setMessage(String)}
     * @see {@link #getMessage()}
     */
    private String message;

    private EnforcerLevel level = EnforcerLevel.ERROR;

    public final void setMessage( String message )
    {
        this.message = message;
    }

    public final String getMessage()
    {
        return message;
    }

    @Override
    public EnforcerLevel getLevel()
    {
        return level;
    }

    public void setLevel( EnforcerLevel level )
    {
        this.level = level;
    }

}
