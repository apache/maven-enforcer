<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Enforcer Version Range Specification 

The [RequireMavenVersion](./requireMavenVersion.html) and [RequireJavaVersion](./requireJavaVersion.html) rules use the [standard Maven version range syntax](https://maven.apache.org/pom.html#Dependency_Version_Requirement_Specification) with one minor change for ease of use (denoted with *):

|Range|Meaning|
|:---:|:---|
|1\.0|x &gt;= 1.0 * The default Maven meaning for 1.0 is everything (,) but with 1.0 recommended. Obviously this doesn't work for enforcing versions here, so it has been redefined as a minimum version.|
|(,1.0\]|x &lt;= 1.0|
|(,1.0)|x &lt; 1.0|
|\[1.0\]|x == 1.0|
|\[1.0,)|x &gt;= 1.0|
|(1.0,)|x &gt; 1.0|
|(1.0,2.0)|1.0 &lt; x &lt; 2.0|
|\[1.0,2.0\]|1.0 &lt;= x &lt;= 2.0|
|(,1.0\],\[1.2,)|x &lt;= 1.0 or x &gt;= 1.2. Multiple sets are comma-separated|
|(,1.1),(1.1,)|x \!= 1.1|
