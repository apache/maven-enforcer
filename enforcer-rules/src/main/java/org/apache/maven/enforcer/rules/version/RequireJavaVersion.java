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
package org.apache.maven.enforcer.rules.version;

import javax.inject.Named;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.codehaus.plexus.util.StringUtils;

/**
 * This rule checks that the Java version is allowed.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@Named("requireJavaVersion")
public final class RequireJavaVersion extends AbstractVersionEnforcer {

    private static final Pattern JDK8_VERSION_PATTERN = Pattern.compile("([\\d.]+)");

    /**
     * Display the normalized JDK version.
     */
    private boolean display = false;

    @Override
    public void setVersion(String theVersion) {

        if ("8".equals(theVersion)) {
            super.setVersion("1.8");
            return;
        }

        if (!theVersion.contains("8")) {
            super.setVersion(theVersion);
            return;
        }

        Matcher matcher = JDK8_VERSION_PATTERN.matcher(theVersion);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            if ("8".equals(matcher.group(1))) {
                matcher.appendReplacement(result, "1.8");
            } else {
                matcher.appendReplacement(result, "$1");
            }
        }
        matcher.appendTail(result);

        super.setVersion(result.toString());
    }

    @Override
    public void execute() throws EnforcerRuleException {
        String javaVersion = SystemUtils.JAVA_VERSION;
        String javaVersionNormalized = normalizeJDKVersion(javaVersion);
        if (display) {
            getLog().info("Detected Java Version: '" + javaVersion + "'");
            getLog().info("Normalized Java Version: '" + javaVersionNormalized + "'");
        } else {
            getLog().debug("Detected Java Version: '" + javaVersion + "'");
            getLog().debug("Normalized Java Version: '" + javaVersionNormalized + "'");
        }

        ArtifactVersion detectedJdkVersion = new DefaultArtifactVersion(javaVersionNormalized);

        getLog().debug("Parsed Version: Major: " + detectedJdkVersion.getMajorVersion() + " Minor: "
                + detectedJdkVersion.getMinorVersion() + " Incremental: " + detectedJdkVersion.getIncrementalVersion()
                + " Build: " + detectedJdkVersion.getBuildNumber() + " Qualifier: "
                + detectedJdkVersion.getQualifier());

        setCustomMessageIfNoneConfigured(detectedJdkVersion, getVersion());

        enforceVersion("JDK", getVersion(), detectedJdkVersion);
    }

    /**
     * Converts a jdk string from 1.5.0-11b12 to a single 3 digit version like 1.5.0-11
     *
     * @param theJdkVersion to be converted.
     * @return the converted string.
     */
    public static String normalizeJDKVersion(String theJdkVersion) {

        theJdkVersion = theJdkVersion.replaceAll("_|-", ".");
        String tokenArray[] = StringUtils.split(theJdkVersion, ".");
        List<String> tokens = Arrays.asList(tokenArray);
        StringBuilder buffer = new StringBuilder(theJdkVersion.length());

        Iterator<String> iter = tokens.iterator();
        for (int i = 0; i < tokens.size() && i < 4; i++) {
            String section = iter.next();
            section = section.replaceAll("[^0-9]", "");

            if (section != null && !section.isEmpty()) {
                buffer.append(Integer.parseInt(section));

                if (i != 2) {
                    buffer.append('.');
                } else {
                    buffer.append('-');
                }
            }
        }

        String version = buffer.toString();
        version = StringUtils.stripEnd(version, "-");
        return StringUtils.stripEnd(version, ".");
    }

    private void setCustomMessageIfNoneConfigured(ArtifactVersion detectedJdkVersion, String allowedVersionRange) {
        if (getMessage() == null) {
            String version;
            try {
                VersionRange vr = VersionRange.createFromVersionSpec(allowedVersionRange);
                version = AbstractVersionEnforcer.toString(vr);
            } catch (InvalidVersionSpecificationException e) {
                getLog().debug("Could not parse allowed version range " + allowedVersionRange + " " + e.getMessage());
                version = allowedVersionRange;
            }
            String message = String.format(
                    "Detected JDK version %s (JAVA_HOME=%s) is not in the allowed range %s.",
                    detectedJdkVersion, SystemUtils.JAVA_HOME, version);
            super.setMessage(message);
        }
    }

    @Override
    public String toString() {
        return String.format(
                "%s[message=%s, version=%s, display=%b]",
                getClass().getSimpleName(), getMessage(), getVersion(), display);
    }
}
