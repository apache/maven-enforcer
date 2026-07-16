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
package org.apache.maven.enforcer.rules.modules;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

/**
 * Keeps a module's public surface minimal by banning {@code exports} of packages that are meant to be
 * internal. By default any package whose name has an {@code internal} or {@code impl} segment (e.g.
 * {@code com.example.foo.internal} or {@code com.example.impl.util}) must not be exported; the set is
 * configurable through {@code <internalPackagePattern>}.
 *
 * <p>Qualified exports ({@code exports a.b.c to some.friend;}) are ignored by default, because naming
 * the consumer is a deliberate, reviewable decision; set {@code <ignoreQualifiedExports>false</ignoreQualifiedExports>}
 * to check them too. Individual packages can be whitelisted through {@code <allowedExports>}.
 */
@Named("requireMinimalExports")
public final class RequireMinimalExports extends AbstractModuleInfoRule {

    /** Regex matching packages considered non-API; a match that is exported fails the build. */
    private String internalPackagePattern = ".*\\.(internal|impl)(\\..*)?";

    /** Packages exempt from the check even if they match {@link #internalPackagePattern}. */
    private List<String> allowedExports = new ArrayList<>();

    /** When {@code true} (default), only unqualified {@code exports} are checked. */
    private boolean ignoreQualifiedExports = true;

    @Inject
    public RequireMinimalExports(MavenProject project) {
        super(project);
    }

    public void setInternalPackagePattern(String internalPackagePattern) {
        this.internalPackagePattern = internalPackagePattern;
    }

    public void setAllowedExports(List<String> allowedExports) {
        this.allowedExports = allowedExports;
    }

    public void setIgnoreQualifiedExports(boolean ignoreQualifiedExports) {
        this.ignoreQualifiedExports = ignoreQualifiedExports;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        Pattern internal = Pattern.compile(internalPackagePattern);
        for (ModuleOutput output : moduleOutputs()) {
            JavaModuleInfo module = output.moduleInfo();
            if (module == null) {
                continue;
            }
            checkModule(module, internal);
        }
    }

    private void checkModule(JavaModuleInfo module, Pattern internal) throws EnforcerRuleException {
        List<String> violations = new ArrayList<>();
        for (JavaModuleInfo.Directive export : module.exports()) {
            if (ignoreQualifiedExports && export.isQualified()) {
                continue;
            }
            String packageName = export.packageName();
            if (!allowedExports.contains(packageName)
                    && internal.matcher(packageName).matches()) {
                violations.add(packageName);
            }
        }
        if (!violations.isEmpty()) {
            String message = getMessage();
            throw new EnforcerRuleException(
                    message != null
                            ? message
                            : "Module " + module.name() + " exports internal package(s) " + violations + " matching '"
                                    + internalPackagePattern
                                    + "'. Keep exports to public API packages, or whitelist them "
                                    + "in <allowedExports>.");
        }
    }

    @Override
    public String toString() {
        return String.format(
                "RequireMinimalExports[internalPackagePattern=%s, ignoreQualifiedExports=%b, allowedExports=%s, message=%s]",
                internalPackagePattern, ignoreQualifiedExports, allowedExports, getMessage());
    }
}
