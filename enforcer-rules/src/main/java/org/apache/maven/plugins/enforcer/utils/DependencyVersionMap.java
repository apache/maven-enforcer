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
package org.apache.maven.plugins.enforcer.utils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;

/**
 * @author Brian Fox
 *
 */
public class DependencyVersionMap implements DependencyNodeVisitor {
    private boolean uniqueVersions;

    private Map<String, List<DependencyNode>> idsToNode;

    public DependencyVersionMap(Log log) {
        idsToNode = new HashMap<>();
    }

    public void setUniqueVersions(boolean uniqueVersions) {
        this.uniqueVersions = uniqueVersions;
    }

    @Override
    public boolean visit(DependencyNode node) {
        addDependency(node);
        return !containsConflicts(node);
    }

    @Override
    public boolean endVisit(DependencyNode node) {
        return true;
    }

    private String constructKey(DependencyNode node) {
        return constructKey(node.getArtifact());
    }

    private String constructKey(Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    public void addDependency(DependencyNode node) {
        String key = constructKey(node);
        List<DependencyNode> nodes = idsToNode.get(key);
        if (nodes == null) {
            nodes = new ArrayList<>();
            idsToNode.put(key, nodes);
        }
        nodes.add(node);
    }

    private String getVersion(Artifact artifact) {
        return uniqueVersions ? artifact.getVersion() : artifact.getBaseVersion();
    }

    private boolean containsConflicts(DependencyNode node) {
        return containsConflicts(node.getArtifact());
    }

    private boolean containsConflicts(Artifact artifact) {
        return containsConflicts(idsToNode.get(constructKey(artifact)));
    }

    private boolean containsConflicts(List<DependencyNode> nodes) {
        String version = null;
        for (DependencyNode node : nodes) {
            if (version == null) {
                version = getVersion(node.getArtifact());
            } else {
                if (version.compareTo(getVersion(node.getArtifact())) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<List<DependencyNode>> getConflictedVersionNumbers(List<String> includes, List<String> excludes)
            throws EnforcerRuleException {
        List<String> formattedIncludes = formatPatterns(includes);
        List<String> formattedExcludes = formatPatterns(excludes);
        List<List<DependencyNode>> output = new ArrayList<>();
        for (List<DependencyNode> nodes : idsToNode.values()) {
            List<DependencyNode> filteredNodes = nodes;
            if (formattedIncludes != null || formattedExcludes != null) {
                filteredNodes = new ArrayList<>();
                for (DependencyNode node : nodes) {
                    if (includeArtifact(node.getArtifact(), formattedIncludes, formattedExcludes)) {
                        filteredNodes.add(node);
                    }
                }
            }
            if (containsConflicts(filteredNodes)) {
                output.add(filteredNodes);
            }
        }
        return output;
    }

    private static boolean includeArtifact(Artifact artifact, List<String> includes, List<String> excludes)
            throws EnforcerRuleException {
        boolean included = includes == null || includes.isEmpty();
        if (!included) {
            for (String pattern : includes) {
                if (ArtifactUtils.compareDependency(pattern, artifact)) {
                    included = true;
                    break;
                }
            }
        }
        if (!included) {
            return false;
        }
        boolean excluded = false;
        if (excludes != null) {
            for (String pattern : excludes) {
                if (ArtifactUtils.compareDependency(pattern, artifact)) {
                    excluded = true;
                    break;
                }
            }
        }
        return !excluded;
    }

    private static List<String> formatPatterns(List<String> patterns) {
        if (patterns == null) {
            return null;
        }
        List<String> formattedPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            String[] subStrings = pattern.split(":");
            subStrings = StringUtils.stripAll(subStrings);
            String formattedPattern = StringUtils.join(subStrings, ":");
            formattedPatterns.add(formattedPattern);
        }
        return formattedPatterns;
    }
}
