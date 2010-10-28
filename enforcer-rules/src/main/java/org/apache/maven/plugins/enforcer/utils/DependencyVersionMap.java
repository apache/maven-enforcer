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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

public class DependencyVersionMap implements DependencyNodeVisitor {
  
  private boolean demandReleasedVersions = false;
  
  private Map<String, List<DependencyNode>> idsToNode;
  
  private List<DependencyNode> snapshots;
  
  public DependencyVersionMap(Log log){
    idsToNode = new HashMap<String, List<DependencyNode>>();
    snapshots = new ArrayList<DependencyNode>();
  }

  public DependencyVersionMap(boolean demandReleasedVersions, Log log){
    this(log);
    this.demandReleasedVersions = demandReleasedVersions;
  }
  
  public boolean visit(DependencyNode node) {
    addDependency(node);
    if (containsConflicts(node)){
      return false;
    }
    if (demandReleasedVersions){
      if (node.getArtifact().isSnapshot()){
        snapshots.add(node);
        return false;
      }
    }
    return true;
  }

  public boolean endVisit(DependencyNode node) {
    return true;
  } 
  
  private String constructKey(DependencyNode node){
    return constructKey(node.getArtifact());
  }
  
  private String constructKey(Artifact artifact){
    return artifact.getGroupId()+":"+artifact.getArtifactId();
  }

  public void addDependency(DependencyNode node) {
    String key = constructKey(node);
    if (node.getArtifact().isSnapshot()){
      snapshots.add(node);
    }
    List<DependencyNode> nodes = idsToNode.get(key);
    if (nodes == null){
      nodes = new ArrayList<DependencyNode>();
      idsToNode.put(key,nodes);
    }
    nodes.add(node);
  }  
  
  public List<DependencyNode> getSnapshots(){
    return snapshots;
  }
  
  private boolean containsConflicts(DependencyNode node){
    return containsConflicts(node.getArtifact());
  }

  private boolean containsConflicts(Artifact artifact){
    return containsConflicts(idsToNode.get(constructKey(artifact)));
  }

  private boolean containsConflicts(List<DependencyNode> nodes){
    String version = null;
    for (DependencyNode node : nodes){
      if (version == null){
        version = node.getArtifact().getVersion();
      } else {
        if (version.compareTo(node.getArtifact().getVersion()) != 0){
          return true;
        }
      }      
    }
    return false;
  }
  
  public List<List<DependencyNode>> getConflictedVersionNumbers(){
    List<List<DependencyNode>> output = new ArrayList<List<DependencyNode>>();
    for (List<DependencyNode> nodes : idsToNode.values()) {
      if(containsConflicts(nodes)){
        output.add(nodes);
      }
    }
    return output;
  }
}
