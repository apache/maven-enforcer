/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.enforcer.rules;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Objects;

import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.OSUtil;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.Profile;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that the OS is allowed by combinations of family, name, version and cpu architecture. The behavior
 * is exactly the same as the Maven Os profile activation so the same values are allowed here.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@Named("requireOS")
public final class RequireOS extends AbstractStandardEnforcerRule {
    private final ProfileActivator activator;

    private final ProfileActivationContext profileActivationContext;

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
    private String family = null;

    /**
     * The OS name desired.
     */
    private String name = null;

    /**
     * The OS version desired.
     */
    private String version = null;

    /**
     * The OS architecture desired.
     */
    private String arch = null;

    /**
     * Display detected OS information.
     */
    private boolean display = false;

    /**
     * Instantiates a new RequireOS.
     */
    @Inject
    RequireOS(@Named("os") ProfileActivator activator, MavenSession session) {
        this.activator = Objects.requireNonNull(activator);
        this.profileActivationContext = createProfileActivationContext(session);
    }

    private ProfileActivationContext createProfileActivationContext(MavenSession session) {
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        context.setActiveProfileIds(session.getRequest().getActiveProfiles());
        context.setInactiveProfileIds(session.getRequest().getInactiveProfiles());
        context.setProjectDirectory(session.getCurrentProject().getBasedir());
        context.setProjectProperties(session.getCurrentProject().getProperties());
        context.setSystemProperties(System.getProperties());
        context.setUserProperties(session.getUserProperties());
        return context;
    }

    @Override
    public void execute() throws EnforcerRuleException {

        displayOSInfo();

        if (allParamsEmpty()) {
            throw new EnforcerRuleError("All parameters can not be empty. "
                    + "You must pick at least one of (family, name, version, arch), "
                    + "you can use mvn --version to see the current OS information.");
        }

        if (isValidFamily(this.family)) {
            if (!isAllowed()) {
                String message = getMessage();
                if (message == null || message.isEmpty()) {
                    // @formatter:off
                    message = "OS Arch: "
                            + Os.OS_ARCH + " Family: "
                            + Os.OS_FAMILY + " Name: "
                            + Os.OS_NAME + " Version: "
                            + Os.OS_VERSION + " is not allowed by" + (arch != null ? " Arch=" + arch : "")
                            + (family != null ? " Family=" + family : "")
                            + (name != null ? " Name=" + name : "")
                            + (version != null ? " Version=" + version : "");
                    // @formatter:on
                }
                throw new EnforcerRuleException(message);
            }
        } else {
            String validFamilies = String.join(",", Os.getValidFamilies());
            throw new EnforcerRuleError("Invalid Family type used. Valid family types are: " + validFamilies);
        }
    }

    /**
     * Log the current OS information.
     */
    private void displayOSInfo() {
        String string = OSUtil.getOSInfo();

        if (!display) {
            getLog().debug(string);
        } else {
            getLog().info(string);
        }
    }

    /**
     * Helper method to determine if the current OS is allowed based on the injected values for family, name, version
     * and arch.
     *
     * @return true if the version is allowed.
     */
    public boolean isAllowed() {
        // empty lambda as problems collector
        return activator.isActive(createProfile(), profileActivationContext, (req -> {}));
    }

    /**
     * Helper method to check that at least one of family, name, version or arch is set.
     *
     * @return true if all parameters are empty.
     */
    public boolean allParamsEmpty() {
        return (family == null || family.isEmpty())
                && (arch == null || arch.isEmpty())
                && (name == null || name.isEmpty())
                && (version == null || version.isEmpty());
    }

    /**
     * Creates a Profile object that contains the activation information.
     *
     * @return a properly populated profile to be used for OS validation.
     */
    private Profile createProfile() {
        Profile profile = new Profile();
        profile.setActivation(createActivation());
        return profile;
    }

    /**
     * Creates an Activation object that contains the ActivationOS information.
     *
     * @return a properly populated Activation object.
     */
    private Activation createActivation() {
        Activation activation = new Activation();
        activation.setActiveByDefault(false);
        activation.setOs(createOsBean());
        return activation;
    }

    /**
     * Creates an ActivationOS object containing family, name, version and arch.
     *
     * @return a properly populated ActivationOS object.
     */
    private ActivationOS createOsBean() {
        ActivationOS os = new ActivationOS();

        os.setArch(arch);
        os.setFamily(family);
        os.setName(name);
        os.setVersion(version);

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
    public boolean isValidFamily(String theFamily) {

        // in case they are checking !family
        theFamily = StringUtils.stripStart(theFamily, "!");

        return (theFamily == null || theFamily.isEmpty())
                || Os.getValidFamilies().contains(theFamily);
    }

    /**
     * Sets the arch.
     *
     * @param theArch the arch to set
     */
    public void setArch(String theArch) {
        this.arch = theArch;
    }

    /**
     * Sets the family.
     *
     * @param theFamily the family to set
     */
    public void setFamily(String theFamily) {
        this.family = theFamily;
    }

    /**
     * Sets the name.
     *
     * @param theName the name to set
     */
    public void setName(String theName) {
        this.name = theName;
    }

    /**
     * Sets the version.
     *
     * @param theVersion the version to set
     */
    public void setVersion(String theVersion) {
        this.version = theVersion;
    }

    /**
     * @param display The value for the display.
     */
    public void setDisplay(boolean display) {
        this.display = display;
    }

    @Override
    public String getCacheId() {
        // return the hashcodes of all the parameters
        StringBuilder b = new StringBuilder();
        if (version != null && !version.isEmpty()) {
            b.append(version.hashCode());
        }
        if (name != null && !name.isEmpty()) {
            b.append(name.hashCode());
        }
        if (arch != null && !arch.isEmpty()) {
            b.append(arch.hashCode());
        }
        if (family != null && !family.isEmpty()) {
            b.append(family.hashCode());
        }
        return b.toString();
    }

    @Override
    public String toString() {
        return String.format(
                "RequireOS[message=%s, arch=%s, family=%s, name=%s, version=%s, display=%b]",
                getMessage(), arch, family, name, version, display);
    }
}
