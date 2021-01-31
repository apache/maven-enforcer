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

import java.util.List;

/**
 * This rule checks that the Java vendor is allowed.
 *
 * @author Tim Sijstermans
 * @since 3.0.0
 */
public class RequireJavaVendor extends AbstractNonCacheableEnforcerRule
{
    /**
     * Specify the banned vendors. This should be an exact match of the System Property
     * java.vendor, which you can also see with mvn --version. <br>
     * The rule will fail if vendor name matches any exclude, unless it also matches an
     * include rule.
     *
     * Some examples are:
     * <ul>
     * <li><code>AdoptOpenJDK</code> prohibits vendor name AdoptOpenJDK </li>
     * <li><code>Amazon</code> prohibits vendor name Amazon </li>
     * </ul>
     *
     * @see #setExcludes(List)
     * @see #getExcludes()
     */
    private List<String> excludes = null;

    /**
     * Specify the allowed vendor names. This should be an exact match of the System Property
     * java.vendor, which you can also see with mvn --version. <br>
     * Includes override the exclude rules.
     *
     * @see #setIncludes(List)
     * @see #getIncludes()
     */
    private List<String> includes = null;

    @Override
    public void execute( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        if ( excludes != null )
        {
            if ( excludes.contains( SystemUtils.JAVA_VENDOR ) )
            {
                if ( includes != null )
                {
                    if ( !includes.contains( SystemUtils.JAVA_VENDOR ) )
                    {
                        createException();
                    }
                    return;
                }
                createException();
            }
        }
    }

    /**
     * Gets the excludes.
     *
     * @return the excludes
     */
    public List<String> getExcludes()
    {
        return this.excludes;
    }

    /**
     * Specify the banned vendors. This should be an exact match of the System Property
     * java.vendor, which you can also see with mvn --version. <br>
     * The rule will fail if vendor name matches any exclude, unless it also matches an
     * include rule.
     *
     * @see #getExcludes()
     * @param theExcludes the excludes to set
     */
    public void setExcludes( List<String> theExcludes )
    {
        this.excludes = theExcludes;
    }

    /**
     * Gets the includes.
     *
     * @return the includes
     */
    public List<String> getIncludes()
    {
        return this.includes;
    }

    /**
     * Specify the allowed vendor names. This should be an exact match of the System Property
     * java.vendor, which you can also see with mvn --version. <br>
     * Includes override the exclude rules.
     * *
     * @see #setIncludes(List)
     * @param theIncludes the includes to set
     */
    public void setIncludes( List<String> theIncludes )
    {
        this.includes = theIncludes;
    }

    private void createException() throws EnforcerRuleException
    {
        String message = getMessage();
        String error = "Vendor " + SystemUtils.JAVA_VENDOR + " is in a list of banned vendors: " + excludes;
        StringBuilder sb = new StringBuilder();
        if ( message != null )
        {
            sb.append( message ).append( "\n" );
        }

        sb.append( error );

        throw new EnforcerRuleException( sb.toString() );
    }
}
