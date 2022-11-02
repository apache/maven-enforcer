<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
Maven Enforcer Plugin - The Loving Iron Fist of Maven™
======================================================

[![ASF Jira](https://img.shields.io/endpoint?url=https%3A%2F%2Fmaven.apache.org%2Fbadges%2Fasf_jira-MENFORCER.json)][jira]
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven-enforcer.svg?label=License)][license]
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-enforcer-plugin.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.apache.maven.plugins/maven-enforcer-plugin)
[![Reproducible Builds](https://img.shields.io/badge/Reproducible_Builds-ok-green?labelColor=blue)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/apache/maven/enforcer/README.md)
[![Jenkins Status](https://img.shields.io/jenkins/s/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven-enforcer/job/master.svg?)][build]
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven-enforcer/job/master.svg?)][test-results]

The Enforcer plugin provides goals to control certain environmental constraints
such as Maven version, JDK version and OS family along with many more built-in
rules and user created rules.

Documentation
-------------

More information can be found on [Apache Maven Enforcer Plugin Homepage][enforcer-home].
Question related to the usage of the Maven Enforcer Plugin should be posted on
the [Maven User List][users-list].


Where can I get the latest release?
-----------------------------------
You can download release source from our [download page][enforcer-download].

You can get the Maven Enforcer plugin via the following coordinates from central:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <version>3.1.0</version>
</plugin>
```

Contributing
------------

We accept Pull Requests via GitHub. The [developer mailing list][dev-ml-list] is the
main channel of communication for contributors.  
There are some guidelines which will make applying PRs easier for us:
+ No tabs! Please use spaces for indentation.
+ Respect the [code style][code-style].
+ Create minimal diffs - disable on save actions like reformat source code or
  organize imports. If you feel the source code should be reformatted create a
  separate PR for this change.
+ Provide JUnit/Invoker tests for your changes and make sure your changes don't break
  any existing tests by running ```mvn -Prun-its verify```.

If you plan to contribute on a regular basis, please consider filing a [contributor license agreement](https://www.apache.org/licenses/#clas).
You can learn more about contributing via GitHub in our [contribution guidelines](CONTRIBUTING.md).


License
-------
This code is under the [Apache Licence v2][license]

See the `NOTICE` file for required notices and attributions.


Donations
---------
You like Apache Maven? Then [donate back to the ASF](https://www.apache.org/foundation/contributing.html) to support the development.


License
-------
[Apache License, Version 2.0, January 2004][license]


[home]: https://maven.apache.org/enforcer/maven-enforcer-plugin
[jira]: https://issues.apache.org/jira/projects/MENFORCER/
[license]: https://www.apache.org/licenses/LICENSE-2.0
[build]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven-enforcer/
[test-results]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven-enforcer/job/master/lastCompletedBuild/testReport/
[build-status]: https://img.shields.io/jenkins/s/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven-enforcer/job/master.svg?
[build-tests]: https://img.shields.io/jenkins/t/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven-enforcer/job/master.svg?
[enforcer-home]: https://maven.apache.org/enforcer/maven-enforcer-plugin/
[enforcer-download]: https://maven.apache.org/enforcer/download.cgi
[users-list]: https://maven.apache.org/mailing-lists.html
[dev-ml-list]: https://www.mail-archive.com/dev@maven.apache.org/
[code-style]: https://maven.apache.org/developers/conventions/code.html
