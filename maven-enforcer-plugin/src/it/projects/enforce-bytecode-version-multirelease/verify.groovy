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

File file = new File(basedir, "build.log")
assert file.exists()

String text = file.getText("utf-8")

// old
// assert text.contains('[DEBUG] Ignore: module-info maps to regex ^module-info(\\.class)?$')
// new
assert text.contains('[DEBUG] Ignore: module-info (target < Java 8)')
// The log4j-api version is test input, pinned in this IT's own pom.xml. Read it back here
// rather than repeating it, so that bumping the fixture cannot leave this assertion stale.
def log4jVersion = (new File(basedir, 'pom.xml').getText('UTF-8')
        =~ /<artifactId>log4j-api<\/artifactId>\s*<version>([^<]+)<\/version>/)[0][1]
assert text.contains("[DEBUG] log4j-api-${log4jVersion}.jar => ")
