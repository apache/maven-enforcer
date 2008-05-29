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
package org.apache.maven.enforcer.rule.api;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Inteface to be implemented by any rules executed by the enforcer.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public interface EnforcerRule
{
    
    /**
     * This is the interface into the rule. This method should throw an exception
     * containing a reason message if the rule fails the check. The plugin will
     * then decide based on the fail flag if it should stop or just log the
     * message as a warning.
     * 
     * @param helper The helper provides access to the log, MavenSession and has
     * helpers to get at common components. It is also able to look
     * up components by class name.
     * 
     * @throws MojoExecutionException
     * @throws EnforcerRuleException the enforcer rule exception
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException;
    
    /**
     * This method tells the enforcer if the rule results may be cached. If the result is true,
     * the results will be remembered for future executions in the same build (ie children). Subsequent
     * iterations of the rule will be queried to see if they are also cacheable. This will allow the rule to be
     * uncached further down the tree if needed.
     * 
     * @return true, if checks if is cacheable
     */
    public boolean isCacheable();
    
    /**
     * Checks if is result valid.
     * 
     * @param cachedRule the last cached instance of the rule. This is to be used by the rule to
     * potentially determine if the results are still valid (ie if the configuration has been overridden)
     * 
     * @return true if the stored results are valid for the same id.
     */
    public boolean isResultValid(EnforcerRule cachedRule);
    
    /**
     * If the rule is to be cached, this id is used as part of the key. This can allow rules to take parameters
     * that allow multiple results of the same rule to be cached.
     * 
     * @return id to be used by the enforcer to determine uniqueness of cache results. The ids only need to be unique
     * within a given rule implementation as the full key will be [classname]-[id]
     */
    public String getCacheId();
    

}
