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

# Built-In Rules

The following built-in rules ship along with the enforcer plugin:

- [alwaysFail](./alwaysFail.html) - Always fail... used to test plugin configuration.
- [alwaysPass](./alwaysPass.html) - Always passes... used to test plugin configuration.
- [banDependencyManagementScope](./banDependencyManagementScope.html) - bans all scope values except for 'import' from dependencies within the dependency management.
- [banDistributionManagement](./banDistributionManagement.html) - enforces that project doesn't have distributionManagement.
- [banDuplicatePomDependencyVersions](./banDuplicatePomDependencyVersions.html) - enforces that the project doesn't have duplicate declared dependencies.
- [banDynamicVersions](./banDynamicVersions.html) - bans all dependencies requiring version resolution at build time (i.e. version ranges, placeholders `RELEASE`/`LATEST` or SNAPSHOT versions).
- [bannedDependencies](./bannedDependencies.html) - enforces that excluded dependencies aren't included.
- [bannedPlugins](./bannedPlugins.html) - enforces that specific plugins aren't included in the build.
- [bannedRepositories](./bannedRepositories.html) - enforces to not include banned repositories.
- [banTransitiveDependencies](./banTransitiveDependencies.html) - enforces that project doesn't have transitive dependencies.
- [dependencyConvergence](./dependencyConvergence.html) - ensure all dependencies converge to the same version.
- [enforceBytecodeVersion](./enforceBytecodeVersion.html) - enforces that dependency bytecode versions do not exceed the configured maximum.
- [evaluateBeanshell](./evaluateBeanshell.html) - evaluates a beanshell script.
- [externalRules](./externalRules.html) - evaluate rules from an external resource.
- [reactorModuleConvergence](./reactorModuleConvergence.html) - enforces that a multi module build follows best practice.
- [requireActiveProfile](./requireActiveProfile.html) - enforces one or more active profiles.
- [requireEnvironmentVariable](./requireEnvironmentVariable.html) - enforces the existence of an environment variable.
- [requireExplicitDependencyScope](./requireExplicitDependencyScope.html) - enforces that all dependencies have an explicit scope.
- [requireFileChecksum](./requireFileChecksum.html) - enforces that the specified file has a certain checksum.
- [requireFilesDontExist](./requireFilesDontExist.html) - enforces that the list of files does not exist.
- [requireFilesExist](./requireFilesExist.html) - enforces that the list of files does exist.
- [requireFilesSize](./requireFilesSize.html) - enforces that the list of files exists and is within a certain size range.
- [requireJavaVendor](./requireJavaVendor.html) - enforces the JDK vendor.
- [requireJavaVersion](./requireJavaVersion.html) - enforces the JDK version.
- [requireMatchingCoordinates](./requireMatchingCoordinates.html) - enforces specific group ID and/or artifact ID patterns.
- [requireMavenVersion](./requireMavenVersion.html) - enforces the Maven version.
- [requireNoRepositories](./requireNoRepositories.html) - enforces to not include repositories.
- [requireOS](./requireOS.html) - enforces the OS / CPU Architecture.
- [requirePluginVersions](./requirePluginVersions.html) - enforces that all plugins have a specified version.
- [requirePrerequisite](./requirePrerequisite.html) - enforces that prerequisites have been specified.
- [requireProfileIdsExist](./requireProfileIdsExist.html) - enforces the existence of profiles specified on the commandline.
- [requireProperty](./requireProperty.html) - enforces the existence and values of properties.
- [requireReleaseDeps](./requireReleaseDeps.html) - enforces that no snapshots are included as dependencies.
- [requireReleaseVersion](./requireReleaseVersion.html) - enforces that the artifact is not a snapshot.
- [requireSnapshotVersion](./requireSnapshotVersion.html) - enforces that the artifact is not a release.
- [requireSameVersions](./requireSameVersions.html) - enforces that specific dependencies and/or plugins have the same version.
- [requireTextFileChecksum](./requireTextFileChecksum.html) - enforces that the specified text file has a certain checksum (after normalizing line separators).
- [requireUpperBoundDeps](./requireUpperBoundDeps.html) - ensures that every (transitive) dependency is resolved to its specified version or higher.
# Common parameters

The following parameters are supported by all rules:

- **level** - steering whether a rule should fail a build or just display a warning, allowed values: **ERROR**, **WARN**. Default is **ERROR**
- **ruleName** - optional name of rule configuration
# Custom rules

You may also create and inject your own custom rules by following the [maven-enforcer-rule-api](https://maven.apache.org/enforcer/enforcer-api/writing-a-custom-rule.html) instructions.
