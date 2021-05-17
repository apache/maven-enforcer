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
File buildLog = new File( basedir, 'build.log' )
String fc=buildLog.text
assert fc.contains( 'org.apache.maven.enforcer.rule.api.EnforcerRuleException: Found Banned Dependency: org.slf4j:slf4j-nop:jar:1.7.30' ) &&
  !fc.contains( 'Dependency convergence error for org.slf4j:slf4j-api:jar:1.7.29:compile paths to dependency are:' )


 