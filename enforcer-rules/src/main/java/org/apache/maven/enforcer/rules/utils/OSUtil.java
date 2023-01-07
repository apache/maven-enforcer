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
package org.apache.maven.enforcer.rules.utils;

import org.codehaus.plexus.util.Os;

/**
 * Common os utility.
 *
 * @author Slawomir Jaranowski
 * @since 3.2.0
 */
public class OSUtil {

    /**
     * OS Information used by Enforcer rules and display Mojo.
     *
     * @return an on information
     */
    public static String getOSInfo() {

        return String.format(
                "OS Info - Arch: %s, Family: %s, Name: %s, Version: %s",
                Os.OS_ARCH, Os.OS_FAMILY, Os.OS_NAME, Os.OS_VERSION);
    }
}
