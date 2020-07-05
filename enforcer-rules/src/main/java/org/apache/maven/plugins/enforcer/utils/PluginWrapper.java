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

import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;

/**
 * @author Brian Fox
 *
 */
public class PluginWrapper
{
    private String groupId;

    private String artifactId;

    private String version;

    private String source;

    public static List<PluginWrapper> addAll( List<?> plugins, String source )
    {
        List<PluginWrapper> results = null;

        if ( !plugins.isEmpty() )
        {
            results = new ArrayList<>( plugins.size() );
            for ( Object o : plugins )
            {
                if ( o instanceof Plugin )
                {
                    results.add( new PluginWrapper( (Plugin) o, source ) );
                }
                else
                {
                    if ( o instanceof ReportPlugin )
                    {
                        results.add( new PluginWrapper( (ReportPlugin) o, source ) );
                    }
                }

            }
        }
        return results;
    }

    public PluginWrapper( Plugin plugin, String source )
    {
        setGroupId( plugin.getGroupId() );
        setArtifactId( plugin.getArtifactId() );
        setVersion( plugin.getVersion() );
        setSource( source );
    }

    public PluginWrapper( ReportPlugin plugin, String source )
    {
        setGroupId( plugin.getGroupId() );
        setArtifactId( plugin.getArtifactId() );
        setVersion( plugin.getVersion() );
        setSource( source );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource( String source )
    {
        this.source = source;
    }
}
