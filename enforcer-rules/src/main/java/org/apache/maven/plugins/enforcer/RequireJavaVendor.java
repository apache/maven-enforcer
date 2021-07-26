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
 * Rule will fail is it matches any of the excludes or doesn't match any include in case it was set. 
 *
 * @author Tim Sijstermans
 * @since 3.0.0
 */
public class RequireJavaVendor extends AbstractNonCacheableEnforcerRule
{
    /**
     * Java vendors to include. If none is defined, all are included.
     * 
     */
    private List<String> includes;

    /**
     * Java vendors to exclude.
     */
    private List<String> excludes;

    @Override
    public void execute( EnforcerRuleHelper helper ) throws EnforcerRuleException
    {
        if ( excludes != null && excludes.contains( SystemUtils.JAVA_VENDOR ) )
        {
            String message = getMessage();
            if ( message == null ) 
            {
                message = String.format( "%s is an excluded Required Java Vendor", SystemUtils.JAVA_VENDOR );
            }
            throw new EnforcerRuleException( message );
        }
        else if ( includes != null && !includes.contains( SystemUtils.JAVA_VENDOR ) )
        {
            String message = getMessage();
            if ( message == null ) 
            {
                message = String.format( "%s is not an included Required Java Vendor", SystemUtils.JAVA_VENDOR );
            }
            throw new EnforcerRuleException( message );
        }
    }

    /**
     * Specify the banned vendors. This should be an exact match of the System Property
     * java.vendor, which you can also see with mvn --version. <br>
     * Excludes override the include rules.
     *
     * @param theExcludes the vendor to to exclude from the include list.
     */
    public void setExcludes( List<String> theExcludes )
    {
        this.excludes = theExcludes;
    }

    /**
     * Specify the allowed vendor names. This should be an exact match of the System Property
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
     * @param theIncludes the list of required vendors.
     * 
     * @see #setExcludes(List)
     */
    public void setIncludes( List<String> theIncludes )
    {
        this.includes = theIncludes;
    }
}
