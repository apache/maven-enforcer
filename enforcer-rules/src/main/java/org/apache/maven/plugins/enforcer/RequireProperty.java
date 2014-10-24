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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * This rule checks that certain properties are set.
 * 
 * @author Paul Gier
 */
public class RequireProperty
    extends AbstractPropertyEnforcerRule
{

    /**
     * Specify the required property.
     * 
     * @see {@link #setProperty(String)}
     * @see {@link #getPropertyName()}
     */
    private String property = null;

    public final void setProperty( String property )
    {
        this.property = property;
    }

    @Override
    public Object resolveValue( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        Object propValue = null;
        try
        {
            propValue = helper.evaluate( "${" + property + "}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to evaluate property: " + property, eee );
        }
        return propValue;
    }

    protected String resolveValue()
    {
        return null;
    }

    @Override
    public String getPropertyName()
    {
        return property;
    }

    @Override
    public String getName()
    {
        return "Property";
    }
}
