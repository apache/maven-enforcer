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

import org.apache.maven.enforcer.rules.utils.OSUtil;
import org.apache.maven.enforcer.rules.version.RequireJavaVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * This goal displays the current platform information.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @deprecated please use {@code mvn --version}
 */
@Deprecated
@Mojo(name = "display-info", threadSafe = true)
public class DisplayInfoMojo extends AbstractMojo {

    /**
     * The MavenSession
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    /**
     * Entry point to the mojo
     */
    public void execute() {
        String mavenVersion = session.getSystemProperties().getProperty("maven.version");
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");

        getLog().info("Maven Version: " + mavenVersion);
        getLog().info("JDK Version: " + javaVersion + " normalized as: "
                + RequireJavaVersion.normalizeJDKVersion(javaVersion));
        getLog().info("Java Vendor: " + javaVendor);

        getLog().info(OSUtil.getOSInfo());
    }
}
