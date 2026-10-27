---
title: Frequently Asked Questions
---

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

<a id="top"></a>

# Frequently Asked Questions

1. [Why can't I just use the prerequisites tag in the pom?](#question)

<a id="question"></a>

### Why can't I just use the prerequisites tag in the pom?

The prerequisites tag was designed to be used by tools like plugins. It will work for regular projects, but it isn't
inherited to their children. If it is set in a parent reactor, then Maven will do the check. However if one of the
children are built, the check is not performed. The enforcer plugin is designed to allow centralized control over the build environment from
a single &quot;super-pom&quot;, and to allow greater flexibility in version specification by supporting ranges.
