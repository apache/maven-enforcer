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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugins.enforcer.EnforcerTestUtils;
import org.apache.maven.plugins.enforcer.MockProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

// TODO: Auto-generated Javadoc
/**
 * The Class TestEnforcerRuleUtils.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class TestEnforcerRuleUtils
    extends AbstractMojoTestCase
{

    /**
     * Test check if model matches.
     */
    public void testCheckIfModelMatches()
    {

        EnforcerRuleUtils utils = new EnforcerRuleUtils( EnforcerTestUtils.getHelper() );

        Model model = new Model();
        model.setArtifactId( "" );
        model.setGroupId( "" );
        model.setVersion( "" );

        // should generate internal NPE on the parent, but
        // will still
        // compare the raw values
        assertTrue( utils.checkIfModelMatches( "", "", "", model ) );
        assertFalse( utils.checkIfModelMatches( "", "", "1.0", model ) );

        // now setup a parent
        Parent parent = new Parent();
        parent.setArtifactId( "foo" );
        parent.setGroupId( "foo-group" );
        parent.setVersion( "1.0" );
        model.setParent( parent );

        // should NOT pickup the parent artifact
        assertFalse( utils.checkIfModelMatches( "foo-group", "foo", "1.0", model ) );

        // check that the version and group are inherited
        // from the parent.
        assertTrue( utils.checkIfModelMatches( "foo-group", "", "1.0", model ) );

        // check handling of nulls
        assertFalse( utils.checkIfModelMatches( "foo-group", null, "1.0", model ) );
    }

    /**
     * Test get models recursively bottom.
     *
     * @throws ArtifactResolutionException the artifact resolution exception
     * @throws ArtifactNotFoundException the artifact not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws XmlPullParserException the xml pull parser exception
     */
    public void testGetModelsRecursivelyBottom()
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        String path = "target/test-classes/requirePluginVersions/getPomRecursively/b/c";

        StringUtils.replace( path, "/", File.separator );

        File pom = new File( getBasedir() + File.separator + path, "pom.xml" );

        EnforcerRuleUtils utils = new EnforcerRuleUtils( EnforcerTestUtils.getHelper() );
        List models = utils.getModelsRecursively( "group", "c", "1.0", pom );

        // there should be 3
        assertEquals( 3, models.size() );

        // now make sure they are all there
        Model m = new Model();
        m.setGroupId( "group" );
        m.setVersion( "1.0" );
        m.setArtifactId( "c" );

        models.contains( m );

        m.setArtifactId( "b" );
        models.contains( m );

        m.setArtifactId( "a" );
        models.contains( m );
    }

    /**
     * Test get models recursively top.
     *
     * @throws ArtifactResolutionException the artifact resolution exception
     * @throws ArtifactNotFoundException the artifact not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws XmlPullParserException the xml pull parser exception
     */
    public void testGetModelsRecursivelyTop()
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        String path = "target/test-classes/requirePluginVersions/getPomRecursively";

        StringUtils.replace( path, "/", File.separator );

        File pom = new File( getBasedir() + File.separator + path, "pom.xml" );

        EnforcerRuleUtils utils = new EnforcerRuleUtils( EnforcerTestUtils.getHelper() );

        List models = utils.getModelsRecursively( "group", "a", "1.0", pom );

        // there should be 1
        assertEquals( 1, models.size() );

        // now make sure they are all there
        Model m = new Model();
        m.setGroupId( "group" );
        m.setVersion( "1.0" );
        m.setArtifactId( "a" );

        models.contains( m );
    }

    public void testGetModelsRecursivelyParentExpression()
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        String path = "target/test-classes/requirePluginVersions/parentExpression/child";

        StringUtils.replace( path, "/", File.separator );

        File pom = new File( getBasedir() + File.separator + path, "pom.xml" );

        // bit backwards - the project here should really be the one read in the first stage of getModelsRecursively
        MockProject parent = new MockProject();
        parent.setGroupId( "org.apache.maven.plugins.enforcer.test" );
        parent.setArtifactId( "parent" );
        parent.setVersion( "1.0-SNAPSHOT" );

        MockProject project = new MockProject();
        project.setParent( parent );

        EnforcerRuleUtils utils = new EnforcerRuleUtils( EnforcerTestUtils.getHelper( project ) );

        List models =
            utils.getModelsRecursively( "org.apache.maven.plugins.enforcer.test", "child", "1.0-SNAPSHOT", pom );

        // there should be 2
        assertEquals( 2, models.size() );
    }

    public void testGetModelsRecursivelyParentRelativePath()
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        String path = "target/test-classes/requirePluginVersions/parentRelativePath";

        StringUtils.replace( path, "/", File.separator );

        File pom = new File( getBasedir() + File.separator + path, "pom.xml" );

        // bit backwards - the project here should really be the one read in the first stage of getModelsRecursively
        MockProject parent = new MockProject();
        parent.setGroupId( "org.apache.maven.plugins.enforcer.test" );
        parent.setArtifactId( "parent" );
        parent.setVersion( "1.0-SNAPSHOT" );

        MockProject project = new MockProject();
        project.setParent( parent );

        EnforcerRuleUtils utils = new EnforcerRuleUtils( EnforcerTestUtils.getHelper( project ) );

        List models =
            utils.getModelsRecursively( "org.apache.maven.plugins.enforcer.test", "aggregate", "1.0-SNAPSHOT", pom );

        // there should be 2
        assertEquals( 2, models.size() );
    }

    public void testGetModelsRecursivelyParentRelativePathDirectory()
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        String path = "target/test-classes/requirePluginVersions/parentRelativePathDirectory";

        StringUtils.replace( path, "/", File.separator );

        File pom = new File( getBasedir() + File.separator + path, "pom.xml" );

        // bit backwards - the project here should really be the one read in the first stage of getModelsRecursively
        MockProject parent = new MockProject();
        parent.setGroupId( "org.apache.maven.plugins.enforcer.test" );
        parent.setArtifactId( "parent" );
        parent.setVersion( "1.0-SNAPSHOT" );

        MockProject project = new MockProject();
        project.setParent( parent );

        EnforcerRuleUtils utils = new EnforcerRuleUtils( EnforcerTestUtils.getHelper( project ) );

        List models =
            utils.getModelsRecursively( "org.apache.maven.plugins.enforcer.test", "aggregate", "1.0-SNAPSHOT", pom );

        // there should be 2
        assertEquals( 2, models.size() );
    }

    /**
     * Simpler wrapper to execute and deal with the expected result.
     *
     * @param rule the rule
     * @param helper the helper
     * @param shouldFail the should fail
     */
    public static void execute( EnforcerRule rule, EnforcerRuleHelper helper, boolean shouldFail )
    {
        try
        {
            rule.execute( helper );
            if ( shouldFail )
            {
                fail( "Exception expected." );
            }
        }
        catch ( EnforcerRuleException e )
        {
            if ( !shouldFail )
            {
                fail( "No Exception expected:" + e.getLocalizedMessage() );
            }
            helper.getLog().debug( e.getMessage() );
        }
    }
}
