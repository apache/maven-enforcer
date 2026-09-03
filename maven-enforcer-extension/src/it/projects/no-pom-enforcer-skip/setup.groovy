/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

File extensionsXml = new File(basedir, '.mvn/extensions.xml')

if (extensionsXml.exists()) {
    File extDir = new File(localRepositoryPath, 'org/apache/maven/extensions/maven-enforcer-extension')
    String version = extDir.list()?.find { it != 'maven-metadata-local.xml' }

    extensionsXml.text = extensionsXml.text.replace('@project.version@', version)
}

File testProjectDir = new File(basedir, 'test-project')
if (testProjectDir.exists()) {
    testProjectDir.deleteDir()
}

return true