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
assert buildLog.text.contains( 'Rule 0: org.apache.maven.enforcer.rules.dependency.RequireUpperBoundDeps failed with message:' )
assert buildLog.text.contains( 'Require upper bound dependencies error for org.apache.maven.plugins.enforcer.its:menforcer134_model:1.0-20130423.042904-7222 paths to dependency are:' )
