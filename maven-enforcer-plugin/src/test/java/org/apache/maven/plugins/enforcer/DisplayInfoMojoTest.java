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

import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks the information logged by the deprecated {@code display-info} goal.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("deprecation")
class DisplayInfoMojoTest {

    @Mock
    private MavenSession session;

    @InjectMocks
    private DisplayInfoMojo mojo;

    @Test
    void logsMavenJdkAndVendorInformation() {
        Properties systemProperties = new Properties();
        systemProperties.setProperty("maven.version", "9.8.7");
        when(session.getSystemProperties()).thenReturn(systemProperties);

        Log logSpy = Mockito.spy(mojo.getLog());
        mojo.setLog(logSpy);

        mojo.execute();

        verify(logSpy).info("Maven Version: 9.8.7");
        verify(logSpy)
                .info(Mockito.startsWith("JDK Version: " + System.getProperty("java.version") + " normalized as: "));
        verify(logSpy).info("Java Vendor: " + System.getProperty("java.vendor"));
    }
}
