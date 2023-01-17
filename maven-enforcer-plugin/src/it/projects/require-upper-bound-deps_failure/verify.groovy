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
def LS = System.getProperty( "line.separator" )
File buildLog = new File( basedir, 'build.log' )

assert buildLog.text.contains( 'Rule 0: org.apache.maven.enforcer.rules.dependency.RequireUpperBoundDeps failed with message:' )
def message = 
'Require upper bound dependencies error for org.apache.maven.plugins.enforcer.its:menforcer128_api:1.4.0 paths to dependency are:'+LS+
'+-org.apache.maven.plugins.enforcer.its:menforcer128:1.0-SNAPSHOT'+LS+
'  +-org.apache.maven.plugins.enforcer.its:menforcer128_api:1.4.0'+LS+
'and'+LS+
'+-org.apache.maven.plugins.enforcer.its:menforcer128:1.0-SNAPSHOT'+LS+
'  +-org.apache.maven.plugins.enforcer.its:menforcer128_classic:0.9.9'+LS+
'    +-org.apache.maven.plugins.enforcer.its:menforcer128_api:1.5.0'+LS
assert buildLog.text.contains( message )

