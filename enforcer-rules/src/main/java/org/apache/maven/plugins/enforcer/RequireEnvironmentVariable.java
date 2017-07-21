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

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;

/**
 * This rule checks that certain environment variable is set.
 *
 * @author <a href='mailto:marvin[at]marvinformatics[dot]com'>Marvin Froeder</a>
 */
public class RequireEnvironmentVariable
    extends AbstractPropertyEnforcerRule
{

    /**
     * Specify the required variable.
     */
    private String variableName = null;

    /**
     * @param variableName the variable name
     * 
     * @see #setVariableName(String)
     * @see #getVariableName()
     */
    public final void setVariableName( String variableName )
    {
        this.variableName = variableName;
    }
    
    public final String getVariableName()
    {
        return variableName;
    }

    @Override
    public String resolveValue( EnforcerRuleHelper helper )
    {
        String envValue = System.getenv( variableName );
        return envValue;
    }

    @Override
    public boolean isCacheable()
    {
        // environment variables won't change while maven is on the run
        return true;
    }

    @Override
    public boolean isResultValid( EnforcerRule cachedRule )
    {
        // this rule shall always have the same result, since environment
        // variables are set before maven is launched
        return true;
    }

    @Override
    public String getCacheId()
    {
        return variableName;
    }

    @Override
    public String getPropertyName()
    {
        return variableName;
    }

    @Override
    public String getName()
    {
        return "Environment variable";
    }
}
