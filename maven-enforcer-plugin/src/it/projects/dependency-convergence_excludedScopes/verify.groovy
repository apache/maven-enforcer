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

assert buildLog.contains( '[ERROR] Rule 0: org.apache.maven.enforcer.rules.dependency.DependencyConvergence failed with message' )
assert buildLog.contains( 'Dependency convergence error for org.slf4j:slf4j-api:jar:1.6.2 paths to dependency are:' )
assert buildLog.contains( '+-org.slf4j:slf4j-api:jar:1.6.2:compile' )
assert buildLog.contains( '+-org.slf4j:slf4j-api:jar:1.6.1:provided' )
assert !buildLog.contains( 'org.slf4j:slf4j-api:jar:1.6.0' )

