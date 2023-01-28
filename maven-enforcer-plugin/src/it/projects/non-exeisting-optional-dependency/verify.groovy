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
def buildLog = new File( basedir, 'build.log' ).text

// only warning about missing dependencies
assert buildLog.contains( '[WARNING] The POM for org.example:test-not-existing:jar:1.0 is missing, no dependency information available' )

// rule executed
assert buildLog.contains( '[INFO] Rule 0: org.apache.maven.enforcer.rules.AlwaysPass passed' )
assert buildLog.contains( '[INFO] Rule 1: org.apache.maven.enforcer.rules.dependency.BanTransitiveDependencies passed' )
assert buildLog.contains( '[INFO] Rule 2: org.apache.maven.enforcer.rules.dependency.BannedDependencies passed' )
assert buildLog.contains( '[INFO] Rule 3: org.apache.maven.enforcer.rules.dependency.DependencyConvergence passed')
assert buildLog.contains( '[INFO] Rule 4: org.apache.maven.enforcer.rules.dependency.RequireReleaseDeps passed')
assert buildLog.contains( '[INFO] Rule 5: org.apache.maven.enforcer.rules.dependency.RequireUpperBoundDeps passed')
