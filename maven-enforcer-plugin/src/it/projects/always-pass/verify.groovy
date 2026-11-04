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
def buildLog = new File(basedir, 'build.log').text

// the rules of the project pom, in the order of the configuration
def expectedRules = [
    'org.apache.maven.enforcer.rules.AlwaysPass',
    'org.apache.maven.enforcer.rules.BanDistributionManagement',
    'org.apache.maven.enforcer.rules.property.BanPropertyPrefix',
    'org.apache.maven.enforcer.rules.dependency.BannedDependencies',
    'org.apache.maven.enforcer.rules.BannedPlugins',
    'org.apache.maven.enforcer.rules.dependency.DependencyConvergence',
    'org.apache.maven.enforcer.rules.EvaluateBeanshell',
    'org.apache.maven.enforcer.rules.EvaluateBeanshell',
    'org.apache.maven.enforcer.rules.property.RequireEnvironmentVariable',
    'org.apache.maven.enforcer.rules.checksum.RequireFileChecksum',
    'org.apache.maven.enforcer.rules.checksum.RequireFileChecksum',
    'org.apache.maven.enforcer.rules.checksum.RequireFileChecksum',
    'org.apache.maven.enforcer.rules.checksum.RequireFileChecksum',
    'org.apache.maven.enforcer.rules.checksum.RequireFileChecksum',
    'org.apache.maven.enforcer.rules.files.RequireFilesDontExist',
    'org.apache.maven.enforcer.rules.files.RequireFilesExist',
    'org.apache.maven.enforcer.rules.files.RequireFilesSize',
    'org.apache.maven.enforcer.rules.files.RequireFilesSize',
    'org.apache.maven.enforcer.rules.version.RequireJavaVersion',
    'org.apache.maven.enforcer.rules.version.RequireJavaVersion',
    'org.apache.maven.enforcer.rules.version.RequireMavenVersion',
    'org.apache.maven.enforcer.rules.version.RequireMavenVersion',
    'org.apache.maven.enforcer.rules.RequireNoRepositories',
    'org.apache.maven.enforcer.rules.RequireOS',
    'org.apache.maven.enforcer.rules.property.RequireProperty',
    'org.apache.maven.enforcer.rules.dependency.RequireReleaseDeps',
    'org.apache.maven.enforcer.rules.RequireReleaseVersion',
    'org.apache.maven.enforcer.rules.RequireSameVersions',
    'org.apache.maven.enforcer.rules.RequireSameVersions',
    'org.apache.maven.enforcer.rules.checksum.RequireTextFileChecksum',
    'org.apache.maven.enforcer.rules.checksum.RequireTextFileChecksum',
    'org.apache.maven.enforcer.rules.checksum.RequireTextFileChecksum',
    'org.apache.maven.enforcer.rules.checksum.RequireTextFileChecksum',
    'org.apache.maven.enforcer.rules.checksum.RequireTextFileChecksum'
]

for (int i = 0; i < expectedRules.size(); i++) {
    assert buildLog.contains("[INFO] Rule ${i}: ${expectedRules[i]} passed".toString()) : "rule ${i} ${expectedRules[i]} did not pass"
}

// no further rule is configured
assert !buildLog.contains("[INFO] Rule ${expectedRules.size()}: ".toString())

// requireOS display
assert buildLog.contains('OS Info - Arch')
