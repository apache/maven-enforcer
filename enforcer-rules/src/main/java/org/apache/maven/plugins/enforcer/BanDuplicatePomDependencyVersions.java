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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Since Maven 3 'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique.
 * Early versions of Maven 3 already warn, this rule can force to break a build for this reason. 
 * 
 * @author Robert Scholte
 * @since 1.3
 *
 */
public class BanDuplicatePomDependencyVersions
    extends AbstractNonCacheableEnforcerRule
{

    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        // get the project
        MavenProject project;
        try
        {
            project = (MavenProject) helper.evaluate( "${project}" );
        }
        catch ( ExpressionEvaluationException eee )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenProject: ", eee );
        }
        
        
        // re-read model, because M3 uses optimized model
        MavenXpp3Reader modelReader = new MavenXpp3Reader();
        FileReader pomReader = null;
        Model model;
        try
        {
            pomReader = new FileReader( project.getFile() );

            model = modelReader.read( pomReader );
        }
        catch ( FileNotFoundException e )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenProject: ", e );
        }
        catch ( IOException e )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenProject: ", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new EnforcerRuleException( "Unable to retrieve the MavenProject: ", e );
        }
        finally
        {
            IOUtil.close( pomReader );
        }

        // @todo reuse ModelValidator when possible
        
//        Object modelValidator = null;
//        try
//        {
//            modelValidator = helper.getComponent( "org.apache.maven.model.validation.ModelValidator" );
//        }
//        catch ( ComponentLookupException e1 )
//        {
//            // noop
//        }


//        if( modelValidator == null )
//        {
            maven2Validation( helper, model );            
//        }
//        else
//        {
//        }
    }

    private void maven2Validation( EnforcerRuleHelper helper, Model model )
        throws EnforcerRuleException
    {
        Map<String, Integer> depMap = new HashMap<String, Integer>();
        Set<String> duplicateDeps = new HashSet<String>();

        @SuppressWarnings( "unchecked" )
        List<Dependency> dependencies = model.getDependencies();
        for ( Dependency dependency : dependencies )
        {
            String key = dependency.getManagementKey();

            helper.getLog().debug( "verify " + key );

            int times = 0;
            if ( depMap.containsKey( key ) )
            {
                times = depMap.get( key );
                duplicateDeps.add( key );
            }
            depMap.put( key, times + 1 );
        }

        if ( !duplicateDeps.isEmpty() )
        {
            StringBuilder message = new StringBuilder();
            message.append( "Found " ).append( duplicateDeps.size() ).append( " duplicate " );
            message.append( duplicateDeps.size() == 1 ? "dependency" : "dependencies" ).append( " in this project:\n" );
            for ( String key : duplicateDeps )
            {
                message.append( " - " ).append( key ).append( " ( " ).append( depMap.get( key ) ).append( " times )\n" );
            }
            throw new EnforcerRuleException( message.toString() );
        }
    }

}
