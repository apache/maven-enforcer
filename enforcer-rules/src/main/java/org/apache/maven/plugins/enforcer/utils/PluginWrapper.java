package org.apache.maven.plugins.enforcer.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;

public class PluginWrapper
{
    private String groupId;

    private String artifactId;

    private String version;

    private String source;

    public static List addAll( List plugins, String source )
    {
        List results = null;

        if ( !plugins.isEmpty() )
        {
            results = new ArrayList( plugins.size() );
            Iterator iter = plugins.iterator();
            while ( iter.hasNext() )
            {
                Object o = iter.next();
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
