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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Build;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * very simple stub of maven project, going to take a lot of work to make it useful as a stub though.
 */
public class MockProject
    extends MavenProject
{

    /** The group id. */
    private String groupId;

    /** The artifact id. */
    private String artifactId;

    /** The name. */
    private String name;

    /** The model. */
    private Model model;

    /** The parent. */
    private MavenProject parent;

    /** The dependencies. */
    private List dependencies;

    /** The file. */
    private File file;

    /** The collected projects. */
    private List collectedProjects;

    /** The attached artifacts. */
    private List attachedArtifacts;

    /** The compile source roots. */
    private List compileSourceRoots;

    /** The test compile source roots. */
    private List testCompileSourceRoots;

    /** The script source roots. */
    private List scriptSourceRoots;

    /** The plugin artifact repositories. */
    private List pluginArtifactRepositories;
    
    /** The artifact repositories. */
    private List artifactRepositories;

    // private ArtifactRepository releaseArtifactRepository;

    // private ArtifactRepository snapshotArtifactRepository;

    /** The active profiles. */
    private List activeProfiles;

    /** The dependency artifacts. */
    private Set dependencyArtifacts;

    /** The dependency management. */
    private DependencyManagement dependencyManagement;

    /** The artifact. */
    private Artifact artifact;

    // private Map artifactMap;

    /** The original model. */
    private Model originalModel;

    // private Map pluginArtifactMap;

    // private Map reportArtifactMap;

    // private Map extensionArtifactMap;

    // private Map projectReferences;

    // private Build buildOverlay;

    /** The execution root. */
    private boolean executionRoot;

    /** The compile artifacts. */
    private List compileArtifacts;

    /** The compile dependencies. */
    private List compileDependencies;

    /** The system dependencies. */
    private List systemDependencies;

    /** The test classpath elements. */
    private List testClasspathElements;

    /** The test dependencies. */
    private List testDependencies;

    /** The system classpath elements. */
    private List systemClasspathElements;

    /** The system artifacts. */
    private List systemArtifacts;

    /** The test artifacts. */
    private List testArtifacts;

    /** The runtime artifacts. */
    private List runtimeArtifacts;

    /** The runtime dependencies. */
    private List runtimeDependencies;

    /** The runtime classpath elements. */
    private List runtimeClasspathElements;

    /** The model version. */
    private String modelVersion;

    /** The packaging. */
    private String packaging;

    /** The inception year. */
    private String inceptionYear;

    /** The url. */
    private String url;

    /** The description. */
    private String description;

    /** The version. */
    private String version;

    /** The default goal. */
    private String defaultGoal;

    /** The artifacts. */
    private Set artifacts;

    /** The properties. */
    private Properties properties = new Properties();

    /** The base dir. */
    private File baseDir = null;

    /**
     * Instantiates a new mock project.
     */
    public MockProject()
    {
        super( (Model) null );
    }

    // kinda dangerous...
    /**
     * Instantiates a new mock project.
     *
     * @param model the model
     */
    public MockProject( Model model )
    {
        // super(model);
        super( (Model) null );
    }

    // kinda dangerous...
    /**
     * Instantiates a new mock project.
     *
     * @param project the project
     */
    public MockProject( MavenProject project )
    {
        // super(project);
        super( (Model) null );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getModulePathAdjustment(org.apache.maven.project.MavenProject)
     */
    public String getModulePathAdjustment( MavenProject mavenProject )
        throws IOException
    {
        return "";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getArtifact()
     */
    public Artifact getArtifact()
    {
        return artifact;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setArtifact(org.apache.maven.artifact.Artifact)
     */
    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getModel()
     */
    public Model getModel()
    {
        return model;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getParent()
     */
    public MavenProject getParent()
    {
        return parent;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setParent(org.apache.maven.project.MavenProject)
     */
    public void setParent( MavenProject mavenProject )
    {
        this.parent = mavenProject;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setRemoteArtifactRepositories(java.util.List)
     */
    public void setRemoteArtifactRepositories( List list )
    {
        this.artifactRepositories = list;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getRemoteArtifactRepositories()
     */
    public List getRemoteArtifactRepositories()
    {
        return artifactRepositories;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#hasParent()
     */
    public boolean hasParent()
    {
        if ( parent != null )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getFile()
     */
    public File getFile()
    {
        return file;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setFile(java.io.File)
     */
    public void setFile( File file )
    {
        this.file = file;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getBasedir()
     */
    public File getBasedir()
    {
        if ( baseDir == null )
        {
            baseDir = new File( PlexusTestCase.getBasedir() );
        }
        return baseDir;
    }

    /**
     * Sets the base dir.
     *
     * @param base the new base dir
     */
    public void setBaseDir( File base )
    {
        baseDir = base;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setDependencies(java.util.List)
     */
    public void setDependencies( List list )
    {
        dependencies = list;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getDependencies()
     */
    public List getDependencies()
    {
        if ( dependencies == null )
        {
            dependencies = Collections.EMPTY_LIST;
        }
        return dependencies;
    }

    /**
     * Sets the dependency management.
     *
     * @param depMgt the new dependency management
     */
    public void setDependencyManagement( DependencyManagement depMgt )
    {
        this.dependencyManagement = depMgt;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getDependencyManagement()
     */
    public DependencyManagement getDependencyManagement()
    {
        if ( dependencyManagement == null )
        {
            dependencyManagement = new DependencyManagement();
        }

        return dependencyManagement;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addCompileSourceRoot(java.lang.String)
     */
    public void addCompileSourceRoot( String string )
    {
        if ( compileSourceRoots == null )
        {
            compileSourceRoots = Collections.singletonList( string );
        }
        else
        {
            compileSourceRoots.add( string );
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addScriptSourceRoot(java.lang.String)
     */
    public void addScriptSourceRoot( String string )
    {
        if ( scriptSourceRoots == null )
        {
            scriptSourceRoots = Collections.singletonList( string );
        }
        else
        {
            scriptSourceRoots.add( string );
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addTestCompileSourceRoot(java.lang.String)
     */
    public void addTestCompileSourceRoot( String string )
    {
        if ( testCompileSourceRoots == null )
        {
            testCompileSourceRoots = Collections.singletonList( string );
        }
        else
        {
            testCompileSourceRoots.add( string );
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getCompileSourceRoots()
     */
    public List getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getScriptSourceRoots()
     */
    public List getScriptSourceRoots()
    {
        return scriptSourceRoots;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getTestCompileSourceRoots()
     */
    public List getTestCompileSourceRoots()
    {
        return testCompileSourceRoots;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getCompileClasspathElements()
     */
    public List getCompileClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return compileSourceRoots;
    }

    /**
     * Sets the compile artifacts.
     *
     * @param compileArtifacts the new compile artifacts
     */
    public void setCompileArtifacts( List compileArtifacts )
    {
        this.compileArtifacts = compileArtifacts;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getCompileArtifacts()
     */
    public List getCompileArtifacts()
    {
        return compileArtifacts;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getCompileDependencies()
     */
    public List getCompileDependencies()
    {
        return compileDependencies;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getTestClasspathElements()
     */
    public List getTestClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return testClasspathElements;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getTestArtifacts()
     */
    public List getTestArtifacts()
    {
        return testArtifacts;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getTestDependencies()
     */
    public List getTestDependencies()
    {
        return testDependencies;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getRuntimeClasspathElements()
     */
    public List getRuntimeClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return runtimeClasspathElements;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getRuntimeArtifacts()
     */
    public List getRuntimeArtifacts()
    {
        return runtimeArtifacts;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getRuntimeDependencies()
     */
    public List getRuntimeDependencies()
    {
        return runtimeDependencies;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getSystemClasspathElements()
     */
    public List getSystemClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return systemClasspathElements;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getSystemArtifacts()
     */
    public List getSystemArtifacts()
    {
        return systemArtifacts;
    }

    /**
     * Sets the runtime classpath elements.
     *
     * @param runtimeClasspathElements the new runtime classpath elements
     */
    public void setRuntimeClasspathElements( List runtimeClasspathElements )
    {
        this.runtimeClasspathElements = runtimeClasspathElements;
    }

    /**
     * Sets the attached artifacts.
     *
     * @param attachedArtifacts the new attached artifacts
     */
    public void setAttachedArtifacts( List attachedArtifacts )
    {
        this.attachedArtifacts = attachedArtifacts;
    }

    /**
     * Sets the compile source roots.
     *
     * @param compileSourceRoots the new compile source roots
     */
    public void setCompileSourceRoots( List compileSourceRoots )
    {
        this.compileSourceRoots = compileSourceRoots;
    }

    /**
     * Sets the test compile source roots.
     *
     * @param testCompileSourceRoots the new test compile source roots
     */
    public void setTestCompileSourceRoots( List testCompileSourceRoots )
    {
        this.testCompileSourceRoots = testCompileSourceRoots;
    }

    /**
     * Sets the script source roots.
     *
     * @param scriptSourceRoots the new script source roots
     */
    public void setScriptSourceRoots( List scriptSourceRoots )
    {
        this.scriptSourceRoots = scriptSourceRoots;
    }

    /**
     * Sets the artifact map.
     *
     * @param artifactMap the new artifact map
     */
    public void setArtifactMap( Map artifactMap )
    {
        // this.artifactMap = artifactMap;
    }

    /**
     * Sets the plugin artifact map.
     *
     * @param pluginArtifactMap the new plugin artifact map
     */
    public void setPluginArtifactMap( Map pluginArtifactMap )
    {
        // this.pluginArtifactMap = pluginArtifactMap;
    }

    /**
     * Sets the report artifact map.
     *
     * @param reportArtifactMap the new report artifact map
     */
    public void setReportArtifactMap( Map reportArtifactMap )
    {
        // this.reportArtifactMap = reportArtifactMap;
    }

    /**
     * Sets the extension artifact map.
     *
     * @param extensionArtifactMap the new extension artifact map
     */
    public void setExtensionArtifactMap( Map extensionArtifactMap )
    {
        // this.extensionArtifactMap = extensionArtifactMap;
    }

    /**
     * Sets the project references.
     *
     * @param projectReferences the new project references
     */
    public void setProjectReferences( Map projectReferences )
    {
        // this.projectReferences = projectReferences;
    }

    /**
     * Sets the builds the overlay.
     *
     * @param buildOverlay the new builds the overlay
     */
    public void setBuildOverlay( Build buildOverlay )
    {
        // this.buildOverlay = buildOverlay;
    }

    /**
     * Sets the compile dependencies.
     *
     * @param compileDependencies the new compile dependencies
     */
    public void setCompileDependencies( List compileDependencies )
    {
        this.compileDependencies = compileDependencies;
    }

    /**
     * Sets the system dependencies.
     *
     * @param systemDependencies the new system dependencies
     */
    public void setSystemDependencies( List systemDependencies )
    {
        this.systemDependencies = systemDependencies;
    }

    /**
     * Sets the test classpath elements.
     *
     * @param testClasspathElements the new test classpath elements
     */
    public void setTestClasspathElements( List testClasspathElements )
    {
        this.testClasspathElements = testClasspathElements;
    }

    /**
     * Sets the test dependencies.
     *
     * @param testDependencies the new test dependencies
     */
    public void setTestDependencies( List testDependencies )
    {
        this.testDependencies = testDependencies;
    }

    /**
     * Sets the system classpath elements.
     *
     * @param systemClasspathElements the new system classpath elements
     */
    public void setSystemClasspathElements( List systemClasspathElements )
    {
        this.systemClasspathElements = systemClasspathElements;
    }

    /**
     * Sets the system artifacts.
     *
     * @param systemArtifacts the new system artifacts
     */
    public void setSystemArtifacts( List systemArtifacts )
    {
        this.systemArtifacts = systemArtifacts;
    }

    /**
     * Sets the test artifacts.
     *
     * @param testArtifacts the new test artifacts
     */
    public void setTestArtifacts( List testArtifacts )
    {
        this.testArtifacts = testArtifacts;
    }

    /**
     * Sets the runtime artifacts.
     *
     * @param runtimeArtifacts the new runtime artifacts
     */
    public void setRuntimeArtifacts( List runtimeArtifacts )
    {
        this.runtimeArtifacts = runtimeArtifacts;
    }

    /**
     * Sets the runtime dependencies.
     *
     * @param runtimeDependencies the new runtime dependencies
     */
    public void setRuntimeDependencies( List runtimeDependencies )
    {
        this.runtimeDependencies = runtimeDependencies;
    }

    /**
     * Sets the model.
     *
     * @param model the new model
     */
    public void setModel( Model model )
    {
        this.model = model;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getSystemDependencies()
     */
    public List getSystemDependencies()
    {
        return systemDependencies;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setModelVersion(java.lang.String)
     */
    public void setModelVersion( String string )
    {
        this.modelVersion = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getModelVersion()
     */
    public String getModelVersion()
    {
        return modelVersion;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getId()
     */
    public String getId()
    {
        return "";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setGroupId(java.lang.String)
     */
    public void setGroupId( String string )
    {
        this.groupId = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getGroupId()
     */
    public String getGroupId()
    {
        return groupId;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setArtifactId(java.lang.String)
     */
    public void setArtifactId( String string )
    {
        this.artifactId = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getArtifactId()
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setName(java.lang.String)
     */
    public void setName( String string )
    {
        this.name = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getName()
     */
    public String getName()
    {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setVersion(java.lang.String)
     */
    public void setVersion( String string )
    {
        this.version = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getVersion()
     */
    public String getVersion()
    {
        return version;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getPackaging()
     */
    public String getPackaging()
    {
        return packaging;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setPackaging(java.lang.String)
     */
    public void setPackaging( String string )
    {
        this.packaging = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setInceptionYear(java.lang.String)
     */
    public void setInceptionYear( String string )
    {
        this.inceptionYear = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getInceptionYear()
     */
    public String getInceptionYear()
    {
        return inceptionYear;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setUrl(java.lang.String)
     */
    public void setUrl( String string )
    {
        this.url = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getUrl()
     */
    public String getUrl()
    {
        return url;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getPrerequisites()
     */
    public Prerequisites getPrerequisites()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setIssueManagement(org.apache.maven.model.IssueManagement)
     */
    public void setIssueManagement( IssueManagement issueManagement )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getCiManagement()
     */
    public CiManagement getCiManagement()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setCiManagement(org.apache.maven.model.CiManagement)
     */
    public void setCiManagement( CiManagement ciManagement )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getIssueManagement()
     */
    public IssueManagement getIssueManagement()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setDistributionManagement(org.apache.maven.model.DistributionManagement)
     */
    public void setDistributionManagement( DistributionManagement distributionManagement )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getDistributionManagement()
     */
    public DistributionManagement getDistributionManagement()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setDescription(java.lang.String)
     */
    public void setDescription( String string )
    {
        this.description = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getDescription()
     */
    public String getDescription()
    {
        return description;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setOrganization(org.apache.maven.model.Organization)
     */
    public void setOrganization( Organization organization )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getOrganization()
     */
    public Organization getOrganization()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setScm(org.apache.maven.model.Scm)
     */
    public void setScm( Scm scm )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getScm()
     */
    public Scm getScm()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setMailingLists(java.util.List)
     */
    public void setMailingLists( List list )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getMailingLists()
     */
    public List getMailingLists()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addMailingList(org.apache.maven.model.MailingList)
     */
    public void addMailingList( MailingList mailingList )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setDevelopers(java.util.List)
     */
    public void setDevelopers( List list )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getDevelopers()
     */
    public List getDevelopers()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addDeveloper(org.apache.maven.model.Developer)
     */
    public void addDeveloper( Developer developer )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setContributors(java.util.List)
     */
    public void setContributors( List list )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getContributors()
     */
    public List getContributors()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addContributor(org.apache.maven.model.Contributor)
     */
    public void addContributor( Contributor contributor )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setBuild(org.apache.maven.model.Build)
     */
    public void setBuild( Build build )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getBuild()
     */
    public Build getBuild()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getResources()
     */
    public List getResources()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getTestResources()
     */
    public List getTestResources()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addResource(org.apache.maven.model.Resource)
     */
    public void addResource( Resource resource )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addTestResource(org.apache.maven.model.Resource)
     */
    public void addTestResource( Resource resource )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setReporting(org.apache.maven.model.Reporting)
     */
    public void setReporting( Reporting reporting )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getReporting()
     */
    public Reporting getReporting()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setLicenses(java.util.List)
     */
    public void setLicenses( List list )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getLicenses()
     */
    public List getLicenses()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addLicense(org.apache.maven.model.License)
     */
    public void addLicense( License license )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setArtifacts(java.util.Set)
     */
    public void setArtifacts( Set set )
    {
        this.artifacts = set;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getArtifacts()
     */
    public Set getArtifacts()
    {
        if ( artifacts == null )
        {
            return Collections.EMPTY_SET;
        }
        else
        {
            return artifacts;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getArtifactMap()
     */
    public Map getArtifactMap()
    {
        return Collections.singletonMap( "", "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setPluginArtifacts(java.util.Set)
     */
    public void setPluginArtifacts( Set set )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getPluginArtifacts()
     */
    public Set getPluginArtifacts()
    {
        return Collections.singleton( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getPluginArtifactMap()
     */
    public Map getPluginArtifactMap()
    {
        return Collections.singletonMap( "", "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setReportArtifacts(java.util.Set)
     */
    public void setReportArtifacts( Set set )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getReportArtifacts()
     */
    public Set getReportArtifacts()
    {
        return Collections.singleton( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getReportArtifactMap()
     */
    public Map getReportArtifactMap()
    {
        return Collections.singletonMap( "", "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setExtensionArtifacts(java.util.Set)
     */
    public void setExtensionArtifacts( Set set )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getExtensionArtifacts()
     */
    public Set getExtensionArtifacts()
    {
        return Collections.singleton( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getExtensionArtifactMap()
     */
    public Map getExtensionArtifactMap()
    {
        return Collections.singletonMap( "", "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setParentArtifact(org.apache.maven.artifact.Artifact)
     */
    public void setParentArtifact( Artifact artifact )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getParentArtifact()
     */
    public Artifact getParentArtifact()
    {
        if (parent !=null)
        {
            return parent.getArtifact();
        }
        else
            return null;

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getRepositories()
     */
    public List getRepositories()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getReportPlugins()
     */
    public List getReportPlugins()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getBuildPlugins()
     */
    public List getBuildPlugins()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getModules()
     */
    public List getModules()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getPluginManagement()
     */
    public PluginManagement getPluginManagement()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addPlugin(org.apache.maven.model.Plugin)
     */
    public void addPlugin( Plugin plugin )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#injectPluginManagementInfo(org.apache.maven.model.Plugin)
     */
    public void injectPluginManagementInfo( Plugin plugin )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getCollectedProjects()
     */
    public List getCollectedProjects()
    {
        return collectedProjects;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setCollectedProjects(java.util.List)
     */
    public void setCollectedProjects( List list )
    {
        this.collectedProjects = list;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setPluginArtifactRepositories(java.util.List)
     */
    public void setPluginArtifactRepositories( List list )
    {
        this.pluginArtifactRepositories = list;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getPluginArtifactRepositories()
     */
    public List getPluginArtifactRepositories()
    {
        return pluginArtifactRepositories;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getDistributionManagementArtifactRepository()
     */
    public ArtifactRepository getDistributionManagementArtifactRepository()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getPluginRepositories()
     */
    public List getPluginRepositories()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setActiveProfiles(java.util.List)
     */
    public void setActiveProfiles( List list )
    {
        activeProfiles = list;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getActiveProfiles()
     */
    public List getActiveProfiles()
    {
        return activeProfiles;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addAttachedArtifact(org.apache.maven.artifact.Artifact)
     */
    public void addAttachedArtifact( Artifact theArtifact )
    {
        if ( attachedArtifacts == null )
        {
            this.attachedArtifacts = Collections.singletonList( theArtifact );
        }
        else
        {
            attachedArtifacts.add( theArtifact );
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getAttachedArtifacts()
     */
    public List getAttachedArtifacts()
    {
        return attachedArtifacts;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getGoalConfiguration(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public Xpp3Dom getGoalConfiguration( String string, String string1, String string2, String string3 )
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getReportConfiguration(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    public Xpp3Dom getReportConfiguration( String string, String string1, String string2 )
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getExecutionProject()
     */
    public MavenProject getExecutionProject()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setExecutionProject(org.apache.maven.project.MavenProject)
     */
    public void setExecutionProject( MavenProject mavenProject )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#writeModel(java.io.Writer)
     */
    public void writeModel( Writer writer )
        throws IOException
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#writeOriginalModel(java.io.Writer)
     */
    public void writeOriginalModel( Writer writer )
        throws IOException
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getDependencyArtifacts()
     */
    public Set getDependencyArtifacts()
    {
        return dependencyArtifacts;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setDependencyArtifacts(java.util.Set)
     */
    public void setDependencyArtifacts( Set set )
    {
        this.dependencyArtifacts = set;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setReleaseArtifactRepository(org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public void setReleaseArtifactRepository( ArtifactRepository artifactRepository )
    {
        // this.releaseArtifactRepository = artifactRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setSnapshotArtifactRepository(org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public void setSnapshotArtifactRepository( ArtifactRepository artifactRepository )
    {
        // this.snapshotArtifactRepository = artifactRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setOriginalModel(org.apache.maven.model.Model)
     */
    public void setOriginalModel( Model model )
    {
        this.originalModel = model;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getOriginalModel()
     */
    public Model getOriginalModel()
    {
        return originalModel;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getBuildExtensions()
     */
    public List getBuildExtensions()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#createArtifacts(org.apache.maven.artifact.factory.ArtifactFactory,
     *      java.lang.String, org.apache.maven.artifact.resolver.filter.ArtifactFilter)
     */
    public Set createArtifacts( ArtifactFactory artifactFactory, String string, ArtifactFilter artifactFilter )
        throws InvalidDependencyVersionException
    {
        return Collections.EMPTY_SET;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#addProjectReference(org.apache.maven.project.MavenProject)
     */
    public void addProjectReference( MavenProject mavenProject )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#attachArtifact(java.lang.String, java.lang.String, java.io.File)
     */
    public void attachArtifact( String string, String string1, File theFile )
    {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getProperties()
     */
    public Properties getProperties()
    {
        return this.properties;
    }

    /**
     * Sets the property.
     *
     * @param key the key
     * @param value the value
     */
    public void setProperty( String key, String value )
    {
        properties.setProperty( key, value );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getFilters()
     */
    public List getFilters()
    {
        return Collections.singletonList( "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getProjectReferences()
     */
    public Map getProjectReferences()
    {
        return Collections.singletonMap( "", "" );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#isExecutionRoot()
     */
    public boolean isExecutionRoot()
    {
        return executionRoot;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#setExecutionRoot(boolean)
     */
    public void setExecutionRoot( boolean b )
    {
        this.executionRoot = b;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#getDefaultGoal()
     */
    public String getDefaultGoal()
    {
        return defaultGoal;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.MavenProject#replaceWithActiveArtifact(org.apache.maven.artifact.Artifact)
     */
    public Artifact replaceWithActiveArtifact( Artifact theArtifact )
    {
        return null;
    }
}
