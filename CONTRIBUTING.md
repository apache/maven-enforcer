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
Contributing to Apache Maven Enforcer
======================

You have found a bug or you have an idea for a cool new feature? Contributing
code is a great way to give something back to the open source community. Before
you dig right into the code there are a few guidelines that we need
contributors to follow so that we can have a chance of keeping on top of
things.

Getting Started
---------------

+ Make sure you have a [GitHub account](https://github.com/signup/free).
+ If you're planning to implement a new feature, it makes sense to discuss your changes 
  on the [dev list](https://maven.apache.org/mail-lists.html) first. 
  This way you can make sure you're not wasting your time on something that isn't 
  considered to be in Apache Maven Enforcer's scope.
+ Open a Github issue for the issue, assuming one does not already exist.
  + Clearly describe the issue. If it is a bug, include steps to reproduce it.
  + Make sure you fill in the earliest version that you know has the issue.
+ Fork the repository on GitHub.

Making Changes
--------------

+ Create a topic branch from where you want to base your work (this is usually the master branch).
+ Make commits of logical units.
+ Respect the original code style:
  + Only use spaces for indentation.
  + Create minimal diffs - disable on save actions like reformat source code or organize imports. 
    If you feel the source code should be reformatted create a separate PR for this change.
  + Check for unnecessary whitespace with git diff --check before committing.
+ Make sure your commit messages are in the proper format. Your commit message should contain the key of the JIRA issue.
+ Make sure you have added the necessary tests for your changes.
+ Run all the tests with `mvn -Prun-its clean verify` to assure nothing else was accidentally broken.

Making Trivial Changes
----------------------

For changes of a trivial nature to comments and documentation, it is not always
necessary to create a new issue.  In this case, it is appropriate to
start the first line of a commit with '(doc)' instead of an issue number.

Submitting Changes
------------------

+ Sign the [Contributor License Agreement][cla] if you haven't already.
+ Push your changes to a topic branch in your fork of the repository.
+ Submit a pull request to the repository in the apache organization.
+ Include the issue number in the PR description as "#234" if there's more work 
  to be done on the issue following this PR, or "fixes #234" if the PR completes work
  on the issue.

Additional Resources
--------------------

+ [Contributing patches](https://maven.apache.org/guides/development/guide-maven-development.html#Creating_and_submitting_a_patch)
+ [Apache Maven Enforcer JIRA project page](https://issues.apache.org/jira/projects/MENFORCER/)
+ [Contributor License Agreement][cla]
+ [General GitHub documentation](https://help.github.com/)
+ [GitHub pull request documentation](https://help.github.com/send-pull-requests/)
+ [Apache Maven Twitter Account](https://twitter.com/ASFMavenProject)
+ #Maven IRC channel on freenode.org

[cla]:https://www.apache.org/licenses/#clas
