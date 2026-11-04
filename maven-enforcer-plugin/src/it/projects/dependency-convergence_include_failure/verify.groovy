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

// all four rules fail: include only, include everything but excluded, exclude only, include and exclude;
// each reports the slf4j-api conflict that its includes and excludes leave
def expectedConflicts = [0: '1.6.2', 1: '1.6.1', 2: '1.6.1', 3: '1.6.1']
for (entry in expectedConflicts) {
    def rule = entry.key
    def version = entry.value
    assert buildLog =~ /(?s)\[ERROR\] Rule ${rule}: \S+DependencyConvergence failed with message:(?:(?!\[ERROR\] Rule \d).)*?Dependency convergence error for org\.slf4j:slf4j-api:jar:\Q${version}\E\. Paths to dependency are:/ : "rule ${rule} did not report the conflict of slf4j-api ${version}"
}
