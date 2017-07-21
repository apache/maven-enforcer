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
import org.apache.maven.plugin.logging.Log;

/**
 * Always pass. This rule is useful for testing the Enforcer configuration.
 * @author Ben Lidgey
 */
public class AlwaysPass
    extends AbstractNonCacheableEnforcerRule
{

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        final Log log = helper.getLog();
        
        String message = getMessage();
        
        StringBuffer buf = new StringBuffer();
        if ( message != null )
        {
            buf.append( message ).append( '\n' );
        }
        buf.append( "Always pass!" );
        log.info( buf.toString() );
    }

}
