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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

/**
 * The Class MockEnforcerExpressionEvaluator.
 */
public class MockEnforcerExpressionEvaluator extends PluginParameterExpressionEvaluator {
    /**
     * Instantiates a new mock enforcer expression evaluator.
     *
     * @param theContext the context
     */
    public MockEnforcerExpressionEvaluator(MavenSession theContext) {
        super(theContext, new MojoExecution(new MojoDescriptor()));
    }

    @Override
    public Object evaluate(String expr) {
        if (expr != null) {
            // just remove the ${ } and return the name as the value
            return expr.replaceAll("\\$\\{|}", "");
        } else {
            return expr;
        }
    }
}
