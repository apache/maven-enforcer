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
 * This goal executes the defined recommended enforcer-rules once per
 * module. In contrast to {@link EnforceMojo} it will never fail the
 * build, i.e. it will only warn.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @author Mirko Friedenhagen
 * @version $Id$
 */
@Mojo( name = "recommend", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true )
public class RecommendMojo
    extends AbstractEnforceMojo
{

    /**
     * Array of objects that implement the EnforcerRule
     * interface to execute.
     */
    @Parameter( required = true )
    private EnforcerRule[] recommendedRules;

    /**
     * @return the recommendedRules
     */
    @Override
    public EnforcerRule[] getRules ()
    {
        return this.recommendedRules;
    }

    /**
     * @param theRules the recommendedRules to set
     */
    @Override
    public void setRules ( EnforcerRule[] theRules )
    {
        this.recommendedRules = theRules;
    }

    /**
     * @param theFailFast the failFast to set
     */
    @Override
    public void setFailFast ( boolean theFailFast )
    {
        // intentionally blank
    }

    /**
     * Always return false, as this Mojo should never fail the build.
     * @return false
     */
    @Override
    public boolean isFail() {
        return false;
    }

    /**
     * Always return false, as this Mojo should never fail the build.
     * @return false
     */
    @Override
    public boolean isFailFast() {
        return false;
    }
}
