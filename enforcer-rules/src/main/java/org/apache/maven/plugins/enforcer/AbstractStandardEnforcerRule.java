package org.apache.maven.plugins.enforcer;

import org.apache.maven.enforcer.rule.api.EnforcerRule;

public abstract class AbstractStandardEnforcerRule
    implements EnforcerRule
{
    /**
     * Specify a friendly message if the rule fails.
     * 
     * @parameter
     */
    public String message = null;

}
