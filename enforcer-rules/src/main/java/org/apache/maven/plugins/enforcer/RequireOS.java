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
package org.apache.maven.plugins.enforcer;

import java.util.Iterator;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.profiles.activation.OperatingSystemProfileActivator;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

// TODO: Auto-generated Javadoc
/**
 * This rule checks that the OS is allowed by combinations of family, name, version and cpu architecture. The behavior
 * is exactly the same as the Maven Os profile activation so the same values are allowed here.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class RequireOS
    extends AbstractStandardEnforcerRule
{

    /**
     * The OS family type desired<br />
     * Possible values:
     * <ul>
     * <li>dos</li>
     * <li>mac</li>
     * <li>netware</li>
     * <li>os/2</li>
     * <li>tandem</li>
     * <li>unix</li>
     * <li>windows</li>
     * <li>win9x</li>
     * <li>z/os</li>
     * <li>os/400</li>
     * </ul>
     */
    public String family = null;

    /** The OS name desired. */
    public String name = null;

    /** The OS version desired. */
    public String version = null;

    /** The OS architecture desired. */
    public String arch = null;

    /** Specify an optional message to the user if the rule fails. */
    public String message = "";

    /** Display detected OS information. */
    public boolean display = false;

    /**
     * Instantiates a new RequireOS.
     */
    public RequireOS()
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {

        displayOSInfo( helper.getLog(), display );

        if ( allParamsEmpty() )
        {
            throw new EnforcerRuleException( "All parameters can not be empty. You must pick at least one of (family, name, version, arch) or use -Denforcer.os.display=true to see the current OS information." );
        }

        if ( isValidFamily( this.family ) )
        {
            if ( !isAllowed() )
            {
                if ( StringUtils.isEmpty( message ) )
                {
                    message =
                        ( "OS Arch: " + Os.OS_ARCH + " Family: " + Os.OS_FAMILY + " Name: " + Os.OS_NAME +
                            " Version: " + Os.OS_VERSION + " is not allowed by" +
                            ( arch != null ? " Arch=" + arch : "" ) + ( family != null ? " Family=" + family : "" ) +
                            ( name != null ? " Name=" + name : "" ) + ( version != null ? " Version=" + version : "" ) );
                }
                throw new EnforcerRuleException( message );
            }
        }
        else
        {
            StringBuffer buffer = new StringBuffer( 50 );
            Iterator iter = Os.getValidFamilies().iterator();
            while ( iter.hasNext() )
            {
                buffer.append( iter.next() );
                buffer.append( ", " );
            }
            String help = StringUtils.stripEnd( buffer.toString().trim(), "." );
            throw new EnforcerRuleException( "Invalid Family type used. Valid family types are: " + help );
        }
    }

    /**
     * Log the current OS information.
     * 
     * @param log the log
     * @param info the info
     */
    public void displayOSInfo( Log log, boolean info )
    {
        String string =
            "OS Info: Arch: " + Os.OS_ARCH + " Family: " + Os.OS_FAMILY + " Name: " + Os.OS_NAME + " Version: " +
                Os.OS_VERSION;

        if ( !info )
        {
            log.debug( string );
        }
        else
        {
            log.info( string );
        }
    }

    /**
     * Helper method to determine if the current OS is allowed based on the injected values for family, name, version
     * and arch.
     * 
     * @return true if the version is allowed.
     */
    public boolean isAllowed()
    {
        OperatingSystemProfileActivator activator = new OperatingSystemProfileActivator();

        return activator.isActive( createProfile() );
    }

    /**
     * Helper method to check that at least one of family, name, version or arch is set.
     * 
     * @return true if all parameters are empty.
     */
    public boolean allParamsEmpty()
    {
        return ( StringUtils.isEmpty( family ) && StringUtils.isEmpty( arch ) && StringUtils.isEmpty( name ) && StringUtils.isEmpty( version ) );

    }

    /**
     * Creates a Profile object that contains the activation information.
     * 
     * @return a properly populated profile to be used for OS validation.
     */
    private Profile createProfile()
    {
        Profile profile = new Profile();
        profile.setActivation( createActivation() );
        return profile;
    }

    /**
     * Creates an Activation object that contains the ActivationOS information.
     * 
     * @return a properly populated Activation object.
     */
    private Activation createActivation()
    {
        Activation activation = new Activation();
        activation.setActiveByDefault( false );
        activation.setOs( createOsBean() );
        return activation;
    }

    /**
     * Creates an ActivationOS object containing family, name, version and arch.
     * 
     * @return a properly populated ActivationOS object.
     */
    private ActivationOS createOsBean()
    {
        ActivationOS os = new ActivationOS();

        os.setArch( arch );
        os.setFamily( family );
        os.setName( name );
        os.setVersion( version );

        return os;
    }

    /**
     * Helper method to check if the given family is in the following list:
     * <ul>
     * <li>dos</li>
     * <li>mac</li>
     * <li>netware</li>
     * <li>os/2</li>
     * <li>tandem</li>
     * <li>unix</li>
     * <li>windows</li>
     * <li>win9x</li>
     * <li>z/os</li>
     * <li>os/400</li>
     * </ul>
     * Note: '!' is allowed at the beginning of the string and still considered valid.
     * 
     * @param theFamily the family to check.
     * @return true if one of the valid families.
     */
    public boolean isValidFamily( String theFamily )
    {

        // in case they are checking !family
        theFamily = StringUtils.stripStart( theFamily, "!" );

        return ( StringUtils.isEmpty( theFamily ) || Os.getValidFamilies().contains( theFamily ) );
    }

    /**
     * Gets the arch.
     * 
     * @return the arch
     */
    public String getArch()
    {
        return this.arch;
    }

    /**
     * Sets the arch.
     * 
     * @param theArch the arch to set
     */
    public void setArch( String theArch )
    {
        this.arch = theArch;
    }

    /**
     * Gets the family.
     * 
     * @return the family
     */
    public String getFamily()
    {
        return this.family;
    }

    /**
     * Sets the family.
     * 
     * @param theFamily the family to set
     */
    public void setFamily( String theFamily )
    {
        this.family = theFamily;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Sets the name.
     * 
     * @param theName the name to set
     */
    public void setName( String theName )
    {
        this.name = theName;
    }

    /**
     * Gets the version.
     * 
     * @return the version
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * Sets the version.
     * 
     * @param theVersion the version to set
     */
    public void setVersion( String theVersion )
    {
        this.version = theVersion;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#getCacheId()
     */
    public String getCacheId()
    {
        // return the hashcodes of all the parameters
        StringBuffer b = new StringBuffer();
        if ( StringUtils.isNotEmpty( version ) )
        {
            b.append( version.hashCode() );
        }
        if ( StringUtils.isNotEmpty( name ) )
        {
            b.append( name.hashCode() );
        }
        if ( StringUtils.isNotEmpty( arch ) )
        {
            b.append( arch.hashCode() );
        }
        if ( StringUtils.isNotEmpty( family ) )
        {
            b.append( family.hashCode() );
        }
        return b.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isCacheable()
     */
    public boolean isCacheable()
    {
        // the os is not going to change between projects in the same build.
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isResultValid(org.apache.maven.enforcer.rule.api.EnforcerRule)
     */
    public boolean isResultValid( EnforcerRule theCachedRule )
    {
        // i will always return the hash of the parameters as my id. If my parameters are the same, this
        // rule must always have the same result.
        return true;
    }
}
