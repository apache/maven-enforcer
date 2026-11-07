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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

/**
 * Bans unqualified deep-reflection access into a module. A bare {@code opens a.b.c;} (or an
 * {@code open module}) grants every other module reflective access to a package's private members,
 * which defeats strong encapsulation. A framework that needs reflective access should be named
 * explicitly with a qualified {@code opens a.b.c to some.framework;}.
 *
 * <p>Packages that genuinely must be opened to the whole world can be listed in
 * {@code <allowedOpens>} to pass the check.
 */
@Named("banUnjustifiedOpens")
public final class BanUnjustifiedOpens extends AbstractModuleInfoRule {

    /** Packages allowed to be opened unqualifiedly despite the ban. */
    private List<String> allowedOpens = new ArrayList<>();

    @Inject
    public BanUnjustifiedOpens(MavenProject project) {
        super(project);
    }

    public void setAllowedOpens(List<String> allowedOpens) {
        this.allowedOpens = allowedOpens != null ? allowedOpens : new ArrayList<>();
    }

    @Override
    public void execute() throws EnforcerRuleException {
        for (ModuleOutput output : moduleOutputs()) {
            JavaModuleInfo module = output.moduleInfo();
            if (module != null) {
                checkModule(module);
            }
        }
    }

    private void checkModule(JavaModuleInfo module) throws EnforcerRuleException {
        if (module.isOpen()) {
            String message = getMessage();
            throw new EnforcerRuleException(
                    message != null
                            ? message
                            : "Module " + module.name() + " is declared as an 'open module', opening every package for "
                                    + "deep reflection. Declare a normal module and use qualified 'opens ... to <module>' "
                                    + "for the packages that actually need it.");
        }

        List<String> violations = new ArrayList<>();
        for (JavaModuleInfo.Directive open : module.opens()) {
            if (!open.isQualified() && !allowedOpens.contains(open.packageName())) {
                violations.add(open.packageName());
            }
        }
        if (!violations.isEmpty()) {
            String message = getMessage();
            throw new EnforcerRuleException(
                    message != null
                            ? message
                            : "Module " + module.name() + " opens package(s) " + violations + " unqualifiedly. Use a "
                                    + "qualified 'opens ... to <module>', or list the package in <allowedOpens>.");
        }
    }

    @Override
    public String toString() {
        return String.format("BanUnjustifiedOpens[allowedOpens=%s, message=%s]", allowedOpens, getMessage());
    }
}
