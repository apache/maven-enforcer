package org.apache.maven.plugins.enforcer.utils;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;

/**
 * @author Brian Fox
 *
 */
public class PluginWrapper
{
    private final String groupId;

    private final String artifactId;

    private final String version;

    private final InputLocationTracker locationTracker;

    public static List<PluginWrapper> addAll( List<?> plugins )
    {
        List<PluginWrapper> results = null;

        if ( !plugins.isEmpty() )
        {
            results = new ArrayList<>( plugins.size() );
            for ( Object o : plugins )
            {
                if ( o instanceof Plugin )
                {
                    results.add( new PluginWrapper( (Plugin) o ) );
                }
                else
                {
                    if ( o instanceof ReportPlugin )
                    {
                        results.add( new PluginWrapper( (ReportPlugin) o ) );
                    }
                }

            }
        }
        return results;
    }

    private PluginWrapper( Plugin plugin )
    {
        this.groupId = plugin.getGroupId();
        this.artifactId = plugin.getArtifactId();
        this.version = plugin.getVersion();
        this.locationTracker = plugin;
    }

    private PluginWrapper( ReportPlugin plugin )
    {
        this.groupId = plugin.getGroupId();
        this.artifactId = plugin.getArtifactId();
        this.version = plugin.getVersion();
        this.locationTracker = plugin;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getSource()
    {
        InputLocation inputLocation = locationTracker.getLocation( "version" );
        
        if ( inputLocation == null )
        {
            // most likely super-pom or default-lifecycle-bindings in Maven 3.6.0 or before (MNG-6593 / MNG-6600)
            return "unknown";
        }
        else
        {
            return inputLocation.getSource().getLocation();
        }
    }
}
