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
package org.apache.maven.plugins.enforcer;

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

import java.text.ChoiceFormat;
import java.util.List;
import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerRule2;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * Checks that all dependencies have an explicitly declared scope in the non-effective pom (i.e. without taking
 * inheritance or dependency management into account).
 */
public class RequireExplicitDependencyScope extends AbstractNonCacheableEnforcerRule implements EnforcerRule2 {

    @Override
    public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
        try {
            int numMissingDependencyScopes = 0;
            MavenProject project = (MavenProject) helper.evaluate("${project}");
            if (project == null) {
                throw new ExpressionEvaluationException("${project} is null");
            }
            List<Dependency> dependencies = project.getOriginalModel().getDependencies(); // this is the non-effective
            // model but the original one
            // without inheritance and
            // interpolation resolved
            // check scope without considering inheritance
            for (Dependency dependency : dependencies) {
                helper.getLog().debug("Found dependency " + dependency);
                if (dependency.getScope() == null) {
                    MessageBuilder msgBuilder = MessageUtils.buffer();
                    msgBuilder
                            .a("Dependency ")
                            .strong(dependency.getManagementKey())
                            .a(" @ ")
                            .strong(formatLocation(project, dependency.getLocation("")))
                            .a(" does not have an explicit scope defined!")
                            .toString();
                    if (getLevel() == EnforcerLevel.ERROR) {
                        helper.getLog().error(msgBuilder.toString());
                    } else {
                        helper.getLog().warn(msgBuilder.toString());
                    }
                    numMissingDependencyScopes++;
                }
            }
            if (numMissingDependencyScopes > 0) {
                ChoiceFormat scopesFormat = new ChoiceFormat("1#scope|1<scopes");
                String logCategory = getLevel() == EnforcerLevel.ERROR ? "errors" : "warnings";
                throw new EnforcerRuleException("Found " + numMissingDependencyScopes + " missing dependency "
                        + scopesFormat.format(numMissingDependencyScopes)
                        + ". Look at the " + logCategory + " emitted above for the details.");
            }
        } catch (ExpressionEvaluationException eee) {
            throw new EnforcerRuleException("Cannot resolve expression: " + eee.getCause(), eee);
        }
    }
}
