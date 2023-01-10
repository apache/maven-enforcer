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
package org.apache.maven.enforcer.rules.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

/**
 * A {@link DependencyVisitor} building a map of parent nodes
 */
public class ParentsVisitor implements DependencyVisitor, ParentNodeProvider {

    private final Map<DependencyNode, DependencyNode> parents = new HashMap<>();
    private final Stack<DependencyNode> parentStack = new Stack<>();

    @Override
    public DependencyNode getParent(DependencyNode node) {
        return parents.get(node);
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        parents.put(node, parentStack.isEmpty() ? null : parentStack.peek());
        parentStack.push(node);
        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        parentStack.pop();
        return true;
    }
}
