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

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.enforcer.rule.api.EnforcerLevel;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRule2;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * This goal executes the defined enforcer-rules once per module.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
@Mojo(
        name = "enforce",
        defaultPhase = LifecyclePhase.VALIDATE,
        requiresDependencyCollection = ResolutionScope.TEST,
        threadSafe = true)
public class EnforceMojo extends AbstractMojo {
    /**
     * This is a static variable used to persist the cached results across plugin invocations.
     */
    protected static Hashtable<String, EnforcerRule> cache = new Hashtable<>();

    /**
     * MojoExecution needed by the ExpressionEvaluator
     */
    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    protected MojoExecution mojoExecution;

    /**
     * The MavenSession
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    /**
     * POM
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Flag to easily skip all checks
     */
    @Parameter(property = "enforcer.skip", defaultValue = "false")
    protected boolean skip = false;

    /**
     * Flag to fail the build if at least one check fails.
     */
    @Parameter(property = "enforcer.fail", defaultValue = "true")
    private boolean fail = true;

    /**
     * Fail on the first rule that doesn't pass
     */
    @Parameter(property = "enforcer.failFast", defaultValue = "false")
    private boolean failFast = false;

    /**
     * Flag to fail the build if no rules are present
     *
     * @since 3.2.0
     */
    @Parameter(property = "enforcer.failIfNoRules", defaultValue = "true")
    private boolean failIfNoRules = true;

    /**
     * Rules configuration to execute as XML.
     * Each first level tag represents rule name to execute.
     * Inner tags are configurations for rule.
     * Eg:
     * <pre>
     *     &lt;rules&gt;
     *         &lt;alwaysFail/&gt;
     *         &lt;alwaysPass&gt;
     *             &lt;message&gt;message for rule&lt;/message&gt;
     *         &lt;/alwaysPass&gt;
     *         &lt;myRule implementation="org.example.MyRule"/&gt;
     *     &lt;/rules>
     * </pre>
     *
     * @since 1.0.0
     */
    @Parameter
    private PlexusConfiguration rules;

    /**
     * Array of Strings that matches the EnforcerRules to execute.
     */
    @Parameter(required = false, property = "rules")
    private String[] commandLineRules;

    /**
     * Use this flag to disable rule result caching. This will cause all rules to execute on each project even if the
     * rule indicates it can safely be cached.
     */
    @Parameter(property = "enforcer.ignoreCache", defaultValue = "false")
    protected boolean ignoreCache = false;

    @Component
    private PlexusContainer container;

    @Component
    private EnforcerRuleManager enforcerRuleManager;

    @Override
    public void execute() throws MojoExecutionException {
        Log log = this.getLog();

        if (isSkip()) {
            log.info("Skipping Rule Enforcement.");
            return;
        }

        Optional<PlexusConfiguration> rulesFromCommandLine = createRulesFromCommandLineOptions();

        List<EnforcerRuleDesc> rulesList;
        try {
            // current behavior - rules from command line override all other configured rules.
            rulesList = enforcerRuleManager.createRules(rulesFromCommandLine.orElse(rules));
        } catch (EnforcerRuleManagerException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        if (rulesList.isEmpty()) {
            if (isFailIfNoRules()) {
                throw new MojoExecutionException(
                        "No rules are configured. Use the skip flag if you want to disable execution.");
            } else {
                log.warn("No rules are configured.");
                return;
            }
        }

        // messages with warn/error flag
        Map<String, Boolean> messages = new LinkedHashMap<>();

        String currentRule = "Unknown";

        // create my helper
        PluginParameterExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
        EnforcerRuleHelper helper = new DefaultEnforcementRuleHelper(session, evaluator, log, container);

        // if we are only warning, then disable
        // failFast
        if (!fail) {
            failFast = false;
        }

        boolean hasErrors = false;

        // go through each rule
        for (int i = 0; i < rulesList.size(); i++) {

            // prevent against empty rules
            EnforcerRuleDesc ruleDesc = rulesList.get(i);
            if (ruleDesc != null) {
                EnforcerRule rule = ruleDesc.getRule();
                EnforcerLevel level = getLevel(rule);
                // store the current rule for
                // logging purposes
                currentRule = rule.getClass().getName();
                try {
                    if (ignoreCache || shouldExecute(rule)) {
                        // execute the rule
                        // noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (rule) {
                            log.info("Executing rule: " + currentRule);
                            rule.execute(helper);
                        }
                    }
                } catch (EnforcerRuleException e) {
                    // i can throw an exception
                    // because failfast will be
                    // false if fail is false.
                    if (failFast && level == EnforcerLevel.ERROR) {
                        throw new MojoExecutionException(
                                currentRule + " failed with message:" + System.lineSeparator() + e.getMessage(), e);
                    } else {
                        // log a warning in case the exception message is missing
                        // so that the user can figure out what is going on
                        final String exceptionMessage = e.getMessage();
                        if (exceptionMessage != null) {
                            log.debug("Adding " + level + " message due to exception", e);
                        } else {
                            log.warn("Rule " + i + ": " + currentRule + " failed without a message", e);
                        }
                        // add the 'failed/warned' message including exceptionMessage
                        // which might be null in rare cases
                        if (level == EnforcerLevel.ERROR) {
                            hasErrors = true;
                            messages.put(
                                    "Rule " + i + ": " + currentRule + " failed with message:" + System.lineSeparator()
                                            + exceptionMessage,
                                    true);
                        } else {
                            messages.put(
                                    "Rule " + i + ": " + currentRule + " warned with message:" + System.lineSeparator()
                                            + exceptionMessage,
                                    false);
                        }
                    }
                }
            }
        }

        // log any messages
        messages.forEach((message, error) -> {
            if (fail && error) {
                log.error(message);
            } else {
                log.warn(message);
            }
        });

        // CHECKSTYLE_OFF: LineLength
        if (fail && hasErrors) {
            throw new MojoExecutionException(
                    "Some Enforcer rules have failed. Look above for specific messages explaining why the rule failed.");
        }
        // CHECKSTYLE_ON: LineLength
    }

    /**
     * Create rules configuration based on command line provided rules list.
     *
     * @return an configuration in case where rules list is present or empty
     */
    private Optional<PlexusConfiguration> createRulesFromCommandLineOptions() {

        if (commandLineRules == null || commandLineRules.length == 0) {
            return Optional.empty();
        }

        PlexusConfiguration configuration = new DefaultPlexusConfiguration("rules");

        for (String rule : commandLineRules) {
            configuration.addChild(new DefaultPlexusConfiguration(rule));
        }

        return Optional.of(configuration);
    }

    /**
     * This method determines if a rule should execute based on the cache
     *
     * @param rule the rule to verify
     * @return {@code true} if rule should be executed, otherwise {@code false}
     */
    protected boolean shouldExecute(EnforcerRule rule) {
        if (rule.isCacheable()) {
            Log log = this.getLog();
            log.debug("Rule " + rule.getClass().getName() + " is cacheable.");
            String key = rule.getClass().getName() + " " + rule.getCacheId();
            if (EnforceMojo.cache.containsKey(key)) {
                log.debug("Key " + key + " was found in the cache");
                if (rule.isResultValid(cache.get(key))) {
                    log.debug("The cached results are still valid. Skipping the rule: "
                            + rule.getClass().getName());
                    return false;
                }
            }

            // add it to the cache of executed rules
            EnforceMojo.cache.put(key, rule);
        }
        return true;
    }

    /**
     * @return the fail
     */
    public boolean isFail() {
        return this.fail;
    }

    /**
     * @param theFail the fail to set
     */
    public void setFail(boolean theFail) {
        this.fail = theFail;
    }

    /**
     * @param theFailFast the failFast to set
     */
    public void setFailFast(boolean theFailFast) {
        this.failFast = theFailFast;
    }

    public boolean isFailFast() {
        return failFast;
    }

    protected String createRuleMessage(int i, String currentRule, EnforcerRuleException e) {
        return "Rule " + i + ": " + currentRule + " failed with message:" + System.lineSeparator() + e.getMessage();
    }

    /**
     * Returns the level of the rule, defaults to {@link EnforcerLevel#ERROR} for backwards compatibility.
     *
     * @param rule might be of type {@link EnforcerRule2}.
     * @return level of the rule.
     */
    private EnforcerLevel getLevel(EnforcerRule rule) {
        if (rule instanceof EnforcerRule2) {
            return ((EnforcerRule2) rule).getLevel();
        } else {
            return EnforcerLevel.ERROR;
        }
    }

    /**
     * @return the skip
     */
    public boolean isSkip() {
        return this.skip;
    }

    /**
     * @param theSkip the skip to set
     */
    public void setSkip(boolean theSkip) {
        this.skip = theSkip;
    }

    /**
     * @return the failIfNoRules
     */
    public boolean isFailIfNoRules() {
        return this.failIfNoRules;
    }

    /**
     * @param thefailIfNoRules the failIfNoRules to set
     */
    public void setFailIfNoRules(boolean thefailIfNoRules) {
        this.failIfNoRules = thefailIfNoRules;
    }

    /**
     * @return the project
     */
    public MavenProject getProject() {
        return this.project;
    }

    /**
     * @param theProject the project to set
     */
    public void setProject(MavenProject theProject) {
        this.project = theProject;
    }

    /**
     * @return the session
     */
    public MavenSession getSession() {
        return this.session;
    }

    /**
     * @param theSession the session to set
     */
    public void setSession(MavenSession theSession) {
        this.session = theSession;
    }
}
