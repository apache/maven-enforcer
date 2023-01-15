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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRuleConfigProvider;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rules.utils.ExpressionEvaluator;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * An enforcer rule that will provide rules configuration from an external resource.
 *
 * @author <a href="mailto:gastaldi@apache.org">George Gastaldi</a>
 * @since 3.2.0
 */
@Named("externalRules")
public final class ExternalRules extends AbstractEnforcerRuleConfigProvider {
    private static final String LOCATION_PREFIX_CLASSPATH = "classpath:";

    /**
     * The external rules location. If it starts with "classpath:", the resource is read from the classpath.
     * Otherwise, it is handled as a filesystem path, either absolute, or relative to <code>${project.basedir}</code>
     */
    private String location;

    private final MojoExecution mojoExecution;

    private final ExpressionEvaluator evaluator;

    @Inject
    public ExternalRules(MojoExecution mojoExecution, ExpressionEvaluator evaluator) {
        this.mojoExecution = Objects.requireNonNull(mojoExecution);
        this.evaluator = Objects.requireNonNull(evaluator);
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public Xpp3Dom getRulesConfig() throws EnforcerRuleError {

        try (InputStream descriptorStream = resolveDescriptor()) {
            Xpp3Dom enforcerRules = Xpp3DomBuilder.build(descriptorStream, "UTF-8");
            if (enforcerRules.getChildCount() == 1 && "enforcer".equals(enforcerRules.getName())) {
                return enforcerRules.getChild(0);
            } else {
                throw new EnforcerRuleError("Enforcer rules configuration not found in: " + location);
            }
        } catch (IOException | XmlPullParserException e) {
            throw new EnforcerRuleError(e);
        }
    }

    private InputStream resolveDescriptor() throws EnforcerRuleError {
        InputStream descriptorStream;
        if (location != null) {
            if (location.startsWith(LOCATION_PREFIX_CLASSPATH)) {
                String classpathLocation = location.substring(LOCATION_PREFIX_CLASSPATH.length());
                getLog().debug("Read rules form classpath location: " + classpathLocation);
                ClassLoader classRealm = mojoExecution.getMojoDescriptor().getRealm();
                descriptorStream = classRealm.getResourceAsStream(classpathLocation);
                if (descriptorStream == null) {
                    throw new EnforcerRuleError("Location '" + classpathLocation + "' not found in classpath");
                }
            } else {
                File descriptorFile = evaluator.alignToBaseDirectory(new File(location));
                getLog().debug("Read rules form file location: " + descriptorFile);
                try {
                    descriptorStream = Files.newInputStream(descriptorFile.toPath());
                } catch (IOException e) {
                    throw new EnforcerRuleError("Could not read descriptor in " + descriptorFile, e);
                }
            }
        } else {
            throw new EnforcerRuleError("No location provided");
        }
        return descriptorStream;
    }

    @Override
    public String toString() {
        return String.format("ExternalRules[location=%s]", location);
    }
}
