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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

// CHECKSTYLE_OFF: LineLength
/**
 * This goal has been deprecated.
 * 
 * @deprecated
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
@Mojo( name = "enforce-once", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST )
public class EnforceOnceMojo
    extends EnforceMojo
{
    public void execute()
        throws MojoExecutionException
    {
        this.getLog().warn( "enforcer:enforce-once is deprecated. Use enforcer:enforce instead. See MENFORCER-11/MENFORCER-12 for more information." );
        super.execute();
    }
}
// CHECKSTYLE_ON: LineLength
