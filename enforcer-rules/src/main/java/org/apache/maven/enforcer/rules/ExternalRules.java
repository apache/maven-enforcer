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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRuleConfigProvider;
import org.apache.maven.enforcer.rule.api.EnforcerRuleError;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
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
     * The external rules location. If it starts with <code>classpath:</code> the resource is read from the classpath.
     * Otherwise, it is handled as a filesystem path, either absolute, or relative to <code>${project.basedir}</code>
     *
     * @since 3.2.0
     */
    private String location;

    /**
     * An optional location of an XSLT file used to transform the rule document available via {@link #location} before
     * it is applied. If it starts with <code>classpath:</code> the resource is read from the classpath.
     * Otherwise, it is handled as a filesystem path, either absolute, or relative to <code>${project.basedir}</code>
     * <p>
     * This is useful, when you want to consume rules defined in an external project, but you need to
     * remove or adapt some of those for the local circumstances.
     * <p>
     * <strong>Example</strong>
     * <p>
     * If <code>location</code> points at the following rule set:
     *
     * <pre>{@code
     * <enforcer>
     *   <rules>
     *     <bannedDependencies>
     *        <excludes>
     *          <exclude>com.google.code.findbugs:jsr305</exclude>
     *          <exclude>com.google.guava:listenablefuture</exclude>
     *        </excludes>
     *     </bannedDependencies>
     *   </rules>
     * </enforcer>
     * }</pre>
     *
     * And if <code>xsltLocation</code> points at the following transformation
     *
     * <pre>{@code
     * <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
     *   <xsl:output omit-xml-declaration="yes"/>
     *
     *   <!-- Copy everything unless there is a template with a more specific matcher -->
     *   <xsl:template match="node()|@*">
     *     <xsl:copy>
     *       <xsl:apply-templates select="node()|@*"/>
     *     </xsl:copy>
     *   </xsl:template>
     *
     *   <!-- An empty template will effectively remove the matching nodes -->
     *   <xsl:template match=
     * "//bannedDependencies/excludes/exclude[contains(text(), 'com.google.code.findbugs:jsr305')]"/>
     * </xsl:stylesheet>
     * }</pre>
     *
     * Then the effective rule set will look like to following:
     *
     * <pre>{@code
     * <enforcer>
     *   <rules>
     *     <bannedDependencies>
     *        <excludes>
     *          <exclude>com.google.guava:listenablefuture</exclude>
     *        </excludes>
     *     </bannedDependencies>
     *   </rules>
     * </enforcer>
     * }</pre>
     *
     * @since 3.6.0
     */
    private String xsltLocation;

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

    public void setXsltLocation(String xsltLocation) {
        this.xsltLocation = xsltLocation;
    }

    @Override
    public Xpp3Dom getRulesConfig() throws EnforcerRuleError {

        try (InputStream descriptorStream = transform(location, resolveDescriptor(location), xsltLocation)) {
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

    private InputStream resolveDescriptor(String path) throws EnforcerRuleError {
        InputStream descriptorStream;
        if (path != null) {
            if (path.startsWith(LOCATION_PREFIX_CLASSPATH)) {
                String classpathLocation = path.substring(LOCATION_PREFIX_CLASSPATH.length());
                getLog().debug("Read rules form classpath location: " + classpathLocation);
                ClassLoader classRealm = mojoExecution.getMojoDescriptor().getRealm();
                descriptorStream = classRealm.getResourceAsStream(classpathLocation);
                if (descriptorStream == null) {
                    throw new EnforcerRuleError("Location '" + classpathLocation + "' not found in classpath");
                }
            } else {
                File descriptorFile = evaluator.alignToBaseDirectory(new File(path));
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
        return String.format("ExternalRules[location=%s, xsltLocation=%s]", location, xsltLocation);
    }

    InputStream transform(String sourceLocation, InputStream sourceXml, String xsltLocation) {
        if (xsltLocation == null || xsltLocation.trim().isEmpty()) {
            return sourceXml;
        }

        try (InputStream in = resolveDescriptor(xsltLocation);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(in));
            transformer.transform(new StreamSource(sourceXml), new StreamResult(baos));
            final byte[] bytes = baos.toByteArray();
            getLog().info(() -> (CharSequence) ("Rules transformed by " + xsltLocation + " from " + location + ":\n\n"
                    + new String(bytes, StandardCharsets.UTF_8)));
            return new ByteArrayInputStream(bytes);
        } catch (IOException
                | EnforcerRuleException
                | TransformerConfigurationException
                | TransformerFactoryConfigurationError e) {
            throw new RuntimeException("Could not open resource " + xsltLocation);
        } catch (TransformerException e) {
            throw new RuntimeException("Could not transform " + sourceLocation + " usinng XSLT " + xsltLocation);
        }
    }
}
