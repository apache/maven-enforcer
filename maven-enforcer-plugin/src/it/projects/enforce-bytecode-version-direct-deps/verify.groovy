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

// only direct dependency
assert text.contains('[DEBUG] Analyzing artifact junit:junit:jar')
assert text.contains('[DEBUG] Analyzing artifact org.slf4j:slf4j-simple:jar')

// no transitive dependencies
assert !text.contains('[DEBUG] Analyzing artifact org.hamcrest:hamcrest-core:jar')
assert !text.contains('[DEBUG] Analyzing artifact org.slf4j:slf4j-api:jar')
