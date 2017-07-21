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
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Since Maven 3 'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique. Early versions of Maven
 * 3 already warn, this rule can force to break a build for this reason.
 * 
 * @author Robert Scholte
 * @since 1.3
 */
public class BanDuplicatePomDependencyVersions
    extends AbstractNonCacheableEnforcerRule
{
    @Override
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

        // Object modelValidator = null;
        // try
        // {
        // modelValidator = helper.getComponent( "org.apache.maven.model.validation.ModelValidator" );
        // }
        // catch ( ComponentLookupException e1 )
        // {
        // // noop
        // }

        // if( modelValidator == null )
        // {
        maven2Validation( helper, model );
        // }
        // else
        // {
        // }
    }

    private void maven2Validation( EnforcerRuleHelper helper, Model model )
        throws EnforcerRuleException
    {
        List<Dependency> dependencies = model.getDependencies();
        Map<String, Integer> duplicateDependencies = validateDependencies( dependencies );

        int duplicates = duplicateDependencies.size();

        StringBuilder summary = new StringBuilder();
        messageBuilder( duplicateDependencies, "dependencies.dependency", summary );

        if ( model.getDependencyManagement() != null )
        {
            List<Dependency> managementDependencies = model.getDependencies();
            Map<String, Integer> duplicateManagementDependencies = validateDependencies( managementDependencies );
            duplicates += duplicateManagementDependencies.size();

            messageBuilder( duplicateManagementDependencies, "dependencyManagement.dependencies.dependency", summary );
        }

        List<Profile> profiles = model.getProfiles();
        for ( Profile profile : profiles )
        {
            List<Dependency> profileDependencies = profile.getDependencies();

            Map<String, Integer> duplicateProfileDependencies = validateDependencies( profileDependencies );

            duplicates += duplicateProfileDependencies.size();

            messageBuilder( duplicateProfileDependencies, "profiles.profile[" + profile.getId()
                + "].dependencies.dependency", summary );

            if ( model.getDependencyManagement() != null )
            {
                List<Dependency> profileManagementDependencies = profile.getDependencies();

                Map<String, Integer> duplicateProfileManagementDependencies =
                    validateDependencies( profileManagementDependencies );

                duplicates += duplicateProfileManagementDependencies.size();

                messageBuilder( duplicateProfileManagementDependencies, "profiles.profile[" + profile.getId()
                    + "].dependencyManagement.dependencies.dependency", summary );
            }
        }

        if ( summary.length() > 0 )
        {
            StringBuilder message = new StringBuilder();
            message.append( "Found " )
                .append( duplicates )
                .append( " duplicate dependency " );
            message.append( duplicateDependencies.size() == 1 ? "declaration" : "declarations" )
                .append( " in this project:\n" );
            message.append( summary );
            throw new EnforcerRuleException( message.toString() );
        }
    }

    private void messageBuilder( Map<String, Integer> duplicateDependencies, String prefix, StringBuilder message )
    {
        if ( !duplicateDependencies.isEmpty() )
        {
            for ( Map.Entry<String, Integer> entry : duplicateDependencies.entrySet() )
            {
                message.append( " - " )
                    .append( prefix )
                    .append( '[' )
                    .append( entry.getKey() )
                    .append( "] ( " )
                    .append( entry.getValue() )
                    .append( " times )\n" );
            }
        }
    }

    private Map<String, Integer> validateDependencies( List<Dependency> dependencies )
        throws EnforcerRuleException
    {
        Map<String, Integer> duplicateDeps = new HashMap<String, Integer>();
        Set<String> deps = new HashSet<String>();
        for ( Dependency dependency : dependencies )
        {
            String key = dependency.getManagementKey();

            if ( deps.contains( key ) )
            {
                int times = 1;
                if ( duplicateDeps.containsKey( key ) )
                {
                    times = duplicateDeps.get( key );
                }
                duplicateDeps.put( key, times + 1 );
            }
            else
            {
                deps.add( key );
            }
        }
        return duplicateDeps;
    }

}
