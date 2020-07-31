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

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;

/**
 * This rule checks that the Java vendor is allowed.
 *
 * @author Tim Sijstermans
 * @since 3.0.0
 */
public class RequireJavaVendor extends AbstractNonCacheableEnforcerRule
{
    private String name;

    @Override
    public void execute( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        if ( !SystemUtils.JAVA_VENDOR.equals( name ) )
        {
            String message = getMessage();
            String error = "Vendor " + SystemUtils.JAVA_VENDOR + " did not match required vendor " + name;
            StringBuilder sb = new StringBuilder();
            if ( message != null )
            {
                sb.append( message ).append( System.lineSeparator() );
            }

            sb.append( error );

            throw new EnforcerRuleException( sb.toString() );
        }
    }

    /**
     * Specify the required name. Some examples are:
     * Name should be an exact match of the System Property java.vendor, which you can also see with mvn --version
     *
     * <ul>
     * <li><code>AdoptOpenJDK</code> enforces vendor name AdoptOpenJDK </li>
     * <li><code>Amazon</code> enforces vendor name Amazon </li>
     * </ul>
     *
     * @param name the required name to set
     */
    public final void setName( String name )
    {
        this.name = name;
    }
}
