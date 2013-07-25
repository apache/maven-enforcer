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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * This goal executes the defined enforcer-rules once per
 * module.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
@Mojo( name = "enforce", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true )
public class EnforceMojo
    extends AbstractEnforceMojo
{
    /**
     * Flag to fail the build if a version check fails.
     */
    @Parameter(property = "enforcer.fail", defaultValue = "true")
    private boolean fail = true;

    /**
     * Fail on the first rule that doesn't pass
     */
    @Parameter(property = "enforcer.failFast", defaultValue = "false")
    private boolean failFast = false;

    /**
     * Array of objects that implement the EnforcerRule
     * interface to execute.
     */
    @Parameter( required = true )
    private EnforcerRule[] rules;

    /**
     * @param theFail the fail to set
     */
    public void setFail ( boolean theFail )
    {
        this.fail = theFail;
    }

    /**
     * @return the rules
     */
    @Override
    public EnforcerRule[] getRules ()
    {
        return this.rules;
    }

    /**
     * @param theRules the rules to set
     */
    @Override
    public void setRules ( EnforcerRule[] theRules )
    {
        this.rules = theRules;
    }

    /**
     * @param theFailFast the failFast to set
     */
    @Override
    public void setFailFast ( boolean theFailFast )
    {
        this.failFast = theFailFast;
    }

    @Override
    public boolean isFailFast() {
        return failFast;
    }

    @Override
    public boolean isFail() {
        return fail;
    }

}
