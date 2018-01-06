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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugins.enforcer.utils.PluginWrapper;

/**
 * The Class TestRequirePluginVersions.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class TestRequirePluginVersions
    extends AbstractMojoTestCase
{

    /**
     * Test has version specified.
     */
    public void testHasVersionSpecified()
    {
        Plugin source = new Plugin();
        source.setArtifactId( "foo" );
        source.setGroupId( "group" );

        // setup the plugins. I'm setting up the foo group
        // with a few bogus entries and then a real one.
        // this is to test that the list is exhaustively
        // searched for versions before giving up.
        // banLatest/Release will fail if it is found
        // anywhere in the list
        List<Plugin> plugins = new ArrayList<Plugin>();
        plugins.add( EnforcerTestUtils.newPlugin( "group", "a-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", null ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", "" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "b-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "c-artifact", "LATEST" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "c-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "d-artifact", "RELEASE" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "d-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "e-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "e-artifact", "RELEASE" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "f-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "f-artifact", "LATEST" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "f-artifact", "1.0-SNAPSHOT" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "g-artifact", "1.0-12345678.123456-1" ) );


        List<PluginWrapper> pluginWrappers = PluginWrapper.addAll( plugins, "unit" );

        RequirePluginVersions rule = new RequirePluginVersions();
        rule.setBanLatest( false );
        rule.setBanRelease( false );
        rule.setBanSnapshots( false );

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();

        assertTrue( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        // check that LATEST is allowed
        source.setArtifactId( "c-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        // check that LATEST is banned
        rule.setBanLatest( true );
        assertFalse( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        // check that LATEST is exhausively checked
        rule.setBanSnapshots( false );
        source.setArtifactId( "f-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        rule.setBanLatest( false );
        rule.setBanSnapshots( true );
        assertFalse( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        // check that TIMESTAMP is allowed
        rule.setBanTimestamps( false );
        source.setArtifactId( "g-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        // check that RELEASE is allowed
        source.setArtifactId( "d-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        // check that RELEASE is banned
        rule.setBanRelease( true );
        assertFalse( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        // check that RELEASE is exhaustively checked
        source.setArtifactId( "e-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );
    }

    /**
     * Test has version specified with properties.
     */
    public void testHasVersionSpecifiedWithProperties()
    {
        Plugin source = new Plugin();
        source.setGroupId( "group" );

        // setup the plugins.
        List<Plugin> plugins = new ArrayList<Plugin>();
        plugins.add( EnforcerTestUtils.newPlugin( "group", "a-artifact", "1.0-${SNAPSHOT}" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "b-artifact", "${1.0}" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "c-artifact", "${LATEST}" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "d-artifact", "${RELEASE}" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "e-artifact", "${}" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "f-artifact", "${   }" ) );

        List<PluginWrapper> pluginWrappers = PluginWrapper.addAll( plugins, "unit" );

        RequirePluginVersions rule = new RequirePluginVersions();
        rule.setBanLatest( false );
        rule.setBanRelease( false );
        rule.setBanSnapshots( false );

        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( true );

        source.setArtifactId( "a-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        source.setArtifactId( "b-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        source.setArtifactId( "c-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        source.setArtifactId( "d-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        // this one checks empty property values
        source.setArtifactId( "e-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        // this one checks empty property values
        source.setArtifactId( "f-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        rule.setBanLatest( true );
        source.setArtifactId( "c-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        rule.setBanRelease( true );
        source.setArtifactId( "d-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        rule.setBanSnapshots( true );
        source.setArtifactId( "a-artifact" );
        assertFalse( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );

        // release versions should pass everything
        source.setArtifactId( "b-artifact" );
        assertTrue( rule.hasValidVersionSpecified( helper, source, pluginWrappers ) );
    }

    /**
     * Test get additional plugins null.
     *
     * @throws MojoExecutionException the mojo execution exception
     */
    public void testGetAdditionalPluginsNull()
        throws MojoExecutionException
    {
        RequirePluginVersions rule = new RequirePluginVersions();
        rule.addAdditionalPlugins( null, null );
    }

    /**
     * Test get additional plugins invalid format.
     */
    public void testGetAdditionalPluginsInvalidFormat()
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        List<String> additional = new ArrayList<String>();

        // invalid format (not enough sections)
        additional.add( "group" );

        Set<Plugin> plugins = new HashSet<Plugin>();
        try
        {
            rule.addAdditionalPlugins( plugins, additional );
            fail( "Expected Exception because the format is invalid" );
        }
        catch ( MojoExecutionException e )
        {
        }

        // invalid format (too many sections)
        additional.clear();
        additional.add( "group:i:i" );
        try
        {
            rule.addAdditionalPlugins( plugins, additional );
            fail( "Expected Exception because the format is invalid" );
        }
        catch ( MojoExecutionException e )
        {
        }

    }

    /**
     * Test get additional plugins empty set.
     *
     * @throws MojoExecutionException the mojo execution exception
     */
    public void testGetAdditionalPluginsEmptySet()
        throws MojoExecutionException
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Set<Plugin> plugins = new HashSet<Plugin>();
        plugins.add( EnforcerTestUtils.newPlugin( "group", "a-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", null ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo2", "" ) );

        List<String> additional = new ArrayList<String>();
        additional.add( "group:a-artifact" );
        additional.add( "group:another-artifact" );

        // make sure a null set can be handled
        Set<Plugin> results = rule.addAdditionalPlugins( null, additional );

        assertNotNull( results );
        assertContainsPlugin( "group", "a-artifact", results );
        assertContainsPlugin( "group", "another-artifact", results );

    }

    /**
     * Test get additional plugins.
     *
     * @throws MojoExecutionException the mojo execution exception
     */
    public void testGetAdditionalPlugins()
        throws MojoExecutionException
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Set<Plugin> plugins = new HashSet<Plugin>();
        plugins.add( EnforcerTestUtils.newPlugin( "group", "a-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", null ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo2", "" ) );

        List<String> additional = new ArrayList<String>();
        additional.add( "group:a-artifact" );
        additional.add( "group:another-artifact" );

        Set<Plugin> results = rule.addAdditionalPlugins( plugins, additional );

        // make sure only one new plugin has been added
        assertNotNull( results );
        assertEquals( 4, results.size() );
        assertContainsPlugin( "group", "a-artifact", results );
        assertContainsPlugin( "group", "another-artifact", results );

    }

    /**
     * Test remove Unchecked plugins.
     *
     * @throws MojoExecutionException the mojo execution exception
     */
    public void testGetUncheckedPlugins()
        throws MojoExecutionException
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Set <Plugin> plugins = new HashSet<Plugin>();
        plugins.add( EnforcerTestUtils.newPlugin( "group", "a-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", null ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo2", "" ) );

        List<String> unchecked = new ArrayList<String>();
        //intentionally inserting spaces to make sure they are handled correctly.
        unchecked.add( "group : a-artifact" );

        Collection<Plugin> results = rule.removeUncheckedPlugins( unchecked, plugins );


        // make sure only one new plugin has been added
        assertNotNull( results );
        assertEquals( 2, results.size() );
        assertContainsPlugin( "group", "foo", results );
        assertContainsPlugin( "group", "foo2", results );
        assertNotContainPlugin( "group", "a-artifact", results );

    }

    /**
     * Test combining values from both lists
     */
    public void testCombinePlugins()
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Set<String> plugins = new HashSet<String>();
        plugins.add( "group:a-artifact" );
        plugins.add( "group:foo" );
        plugins.add( "group:foo2" );

        Collection<String> results = rule.combineUncheckedPlugins( plugins, "group2:a,group3:b" );

        // make sure only one new plugin has been added
        assertNotNull( results );
        assertEquals( 5, results.size() );
        assertTrue( results.contains( "group:foo") );
        assertTrue( results.contains( "group:foo2") );
        assertTrue( results.contains( "group:a-artifact") );
        assertTrue( results.contains( "group2:a") );
        assertTrue( results.contains( "group3:b") );
    }

    /**
     * Test combining with an empty list
     */
    public void testCombinePlugins1()
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Set<String> plugins = new HashSet<String>();
        Collection<String> results = rule.combineUncheckedPlugins( plugins, "group2:a,group3:b" );


        // make sure only one new plugin has been added
        assertNotNull( results );
        assertEquals( 2, results.size() );
        assertTrue( results.contains( "group2:a") );
        assertTrue( results.contains( "group3:b") );
    }

    /**
     * Test combining with a null list
     */
    public void testCombinePlugins2()
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Collection<String> results = rule.combineUncheckedPlugins( null, "group2:a,group3:b" );


        // make sure only one new plugin has been added
        assertNotNull( results );
        assertEquals( 2, results.size() );
        assertTrue( results.contains( "group2:a") );
        assertTrue( results.contains( "group3:b") );
    }

    /**
     * Test combining with an empty string
     */
    public void testCombinePlugins3()
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Set<String> plugins = new HashSet<String>();
        plugins.add( "group:a-artifact" );
        plugins.add( "group:foo" );
        plugins.add( "group:foo2" );

        Collection<String> results = rule.combineUncheckedPlugins( plugins, "" );
        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( "group:foo") );
        assertTrue( results.contains( "group:foo2") );
        assertTrue( results.contains( "group:a-artifact") );
    }

    /**
     * Test combining with a null string
     */
    public void testCombinePlugins4()
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Set<String> plugins = new HashSet<String>();
        plugins.add( "group:a-artifact" );
        plugins.add( "group:foo" );
        plugins.add( "group:foo2" );

        Collection<String> results = rule.combineUncheckedPlugins( plugins, null );
        assertNotNull( results );
        assertEquals( 3, results.size() );
        assertTrue( results.contains( "group:foo") );
        assertTrue( results.contains( "group:foo2") );
        assertTrue( results.contains( "group:a-artifact") );
    }

    /**
     * Test combining with an invalid plugin string
     */
    public void testCombinePlugins5()
    {
        RequirePluginVersions rule = new RequirePluginVersions();

        Set<String> plugins = new HashSet<String>();
        plugins.add( "group:a-artifact" );
        plugins.add( "group:foo" );
        plugins.add( "group:foo2" );

        Collection<String> results = rule.combineUncheckedPlugins( plugins, "a" );
        assertNotNull( results );
        assertEquals( 4, results.size() );
        assertTrue( results.contains( "group:foo") );
        assertTrue( results.contains( "group:foo2") );
        //this should be here, the checking of a valid plugin string happens in another method.
        assertTrue( results.contains( "a") );
    }


    /**
     * Assert contains plugin.
     *
     * @param group the group
     * @param artifact the artifact
     * @param theSet the the set
     */
    private void assertContainsPlugin( String group, String artifact, Collection<Plugin> theSet )
    {
        Plugin p = new Plugin();
        p.setGroupId( group );
        p.setArtifactId( artifact );
        assertTrue( theSet.contains( p ) );
    }

    /**
     * Assert doesn't contain plugin.
     *
     * @param group the group
     * @param artifact the artifact
     * @param theSet the the set
     */
    private void assertNotContainPlugin( String group, String artifact, Collection<Plugin> theSet )
    {
        Plugin p = new Plugin();
        p.setGroupId( group );
        p.setArtifactId( artifact );
        assertFalse( theSet.contains( p ) );
    }

    /**
     * Test id.
     */
    public void testId()
    {
        RequirePluginVersions rule = new RequirePluginVersions();
        rule.getCacheId();
    }
}
