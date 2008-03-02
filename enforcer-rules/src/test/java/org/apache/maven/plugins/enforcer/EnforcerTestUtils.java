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

import java.util.Date;
import java.util.Properties;

import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.enforcer.utils.MockEnforcerExpressionEvaluator;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class EnforcerTestUtils
{
    public static MavenSession getMavenSession()
    {
        return new MavenSession( new MockPlexusContainer(), null, null, null, null, null, null, new Properties(),
                                 new Date() );
    }

    public static EnforcerRuleHelper getHelper()
    {
        return getHelper( new MockProject(), false );
    }

    public static EnforcerRuleHelper getHelper( boolean mockExpression )
    {
        return getHelper( new MockProject(), mockExpression );
    }

    public static EnforcerRuleHelper getHelper( MavenProject project )
    {
        return getHelper( project, false );
    }
    
    public static EnforcerRuleHelper getHelper( MavenProject project, boolean mockExpression )
    {
        MavenSession session = getMavenSession();
        ExpressionEvaluator eval;
        if ( mockExpression )
        {
            eval = new MockEnforcerExpressionEvaluator( session, new MockPathTranslator(), project );
        }
        else
        {
            eval = new EnforcerExpressionEvaluator( session, new MockPathTranslator(), project );
        }
        return new DefaultEnforcementRuleHelper( session, eval, new SystemStreamLog(), null );
    }

    public static Plugin newPlugin( String groupId, String artifactId, String version )
    {
        Plugin plugin = new Plugin();
        plugin.setArtifactId( artifactId );
        plugin.setGroupId( groupId );
        plugin.setVersion( version );
        return plugin;
    }
}
