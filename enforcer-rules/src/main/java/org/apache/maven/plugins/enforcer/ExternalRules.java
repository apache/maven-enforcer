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
package org.apache.maven.plugins.enforcer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * An enforcer rule that will invoke rules from an external resource
 *
 * @author <a href="mailto:gastaldi@apache.org">George Gastaldi</a>
 */
public class ExternalRules extends AbstractNonCacheableEnforcerRule {
    private static final String LOCATION_PREFIX_CLASSPATH = "classpath:";

    /**
     * The external rules location. If it starts with "classpath:", the resource is read from the classpath.
     * Otherwise, it is handled as a filesystem path, either absolute, or relative to <code>${project.basedir}</code>
     */
    String location;

    public ExternalRules() {}

    public ExternalRules(String location) {
        this.location = location;
    }

    @Override
    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        // Find descriptor
        EnforcerDescriptor enforcerDescriptor = getEnforcerDescriptor(helper);
        for (EnforcerRule rule : enforcerDescriptor.getRules()) {
            rule.execute(helper);
        }
    }

    /**
     * Resolve the {@link EnforcerDescriptor} based on the provided {@link #descriptor} or {@link #descriptorRef}
     *
     * @param helper used to build the {@link EnforcerDescriptor}
     * @return an {@link EnforcerDescriptor} for this rule
     * @throws EnforcerRuleException if any failure happens while reading the descriptor
     */
    EnforcerDescriptor getEnforcerDescriptor(EnforcerRuleHelper helper) throws EnforcerRuleException {
        try (InputStream descriptorStream = resolveDescriptor(helper)) {
            EnforcerDescriptor descriptor = new EnforcerDescriptor();
            // To get configuration from the enforcer-plugin mojo do:
            // helper.evaluate(helper.getComponent(MojoExecution.class).getConfiguration().getChild("fail").getValue())
            // Configure EnforcerDescriptor from the XML
            ComponentConfigurator configurator = helper.getComponent(ComponentConfigurator.class, "basic");
            configurator.configureComponent(
                    descriptor, toPlexusConfiguration(descriptorStream), helper, getClassRealm(helper));
            return descriptor;
        } catch (EnforcerRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new EnforcerRuleException("Error while enforcing rules", e);
        }
    }

    private InputStream resolveDescriptor(EnforcerRuleHelper helper)
            throws ComponentLookupException, EnforcerRuleException {
        InputStream descriptorStream;
        if (location != null) {
            if (location.startsWith(LOCATION_PREFIX_CLASSPATH)) {
                String classpathLocation = location.substring(LOCATION_PREFIX_CLASSPATH.length());
                ClassLoader classRealm = getClassRealm(helper);
                descriptorStream = classRealm.getResourceAsStream(classpathLocation);
                if (descriptorStream == null) {
                    throw new EnforcerRuleException("Location '" + classpathLocation + "' not found in classpath");
                }
            } else {
                File descriptorFile = helper.alignToBaseDirectory(new File(location));
                try {
                    descriptorStream = Files.newInputStream(descriptorFile.toPath());
                } catch (IOException e) {
                    throw new EnforcerRuleException("Could not read descriptor in " + descriptorFile, e);
                }
            }
        } else {
            throw new EnforcerRuleException("No location provided");
        }
        return descriptorStream;
    }

    private static PlexusConfiguration toPlexusConfiguration(InputStream descriptorStream)
            throws XmlPullParserException, IOException {
        return new XmlPlexusConfiguration(Xpp3DomBuilder.build(descriptorStream, "UTF-8"));
    }

    private ClassRealm getClassRealm(EnforcerRuleHelper helper) throws ComponentLookupException {
        return helper.getComponent(MojoExecution.class).getMojoDescriptor().getRealm();
    }
}
