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

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * This rule checks that lists of plugins are not included.
 *
 * @author <a href="mailto:velo.br@gmail.com">Marvin Froeder</a>
 */
public class BannedPlugins
    extends BannedDependencies
{
    @Override
    protected Set<Artifact> getDependenciesToCheck( MavenProject project )
    {
        return project.getPluginArtifacts();
    }

    @Override
    protected CharSequence getErrorMessage( Artifact artifact )
    {
        return "Found Banned Plugin: " + artifact.getId() + System.lineSeparator();
    }

}
