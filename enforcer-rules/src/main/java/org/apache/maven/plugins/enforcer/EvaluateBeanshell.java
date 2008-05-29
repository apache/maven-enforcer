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
package org.apache.maven.plugins.enforcer;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;

import bsh.EvalError;
import bsh.Interpreter;

// TODO: Auto-generated Javadoc
/**
 * The Class EvaluateBeanshell.
 * 
 * @author hugonnem Rule for Maven Enforcer using Beanshell to evaluate a conditional expression
 */
public class EvaluateBeanshell
    extends AbstractStandardEnforcerRule
{

    /** Beanshell interpreter. */
    private static final Interpreter bsh = new Interpreter();

    /** The condition to be evaluated. */
    public String condition;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        Log log = helper.getLog();

        try
        {
            log.debug( "Echo condition : " + this.condition );
            // Evaluate condition within Plexus Container
            String script = (String) helper.evaluate( this.condition );
            log.debug( "Echo script : " + script );
            if ( !evaluateCondition( script, log ) )
            {
                if ( StringUtils.isEmpty( message ) )
                {
                    message = "The expression \"" + condition + "\" is not true.";
                }
                throw new EnforcerRuleException( this.message );
            }
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to evaluate an expression", e );
        }
    }

    /**
     * Evaluate expression using Beanshell.
     * 
     * @param script the expression to be evaluated
     * @param log the logger
     * @return boolean the evaluation of the expression
     */
    protected boolean evaluateCondition( String script, Log log )
    {
        Boolean evaluation = Boolean.FALSE;
        try
        {
            evaluation = (Boolean) bsh.eval( script );
            log.debug( "Echo evaluating : " + evaluation );
        }
        catch ( EvalError ex )
        {
            log.warn( "Couldn't evaluate condition: " + script, ex );
        }
        return evaluation.booleanValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#getCacheId()
     */
    public String getCacheId()
    {
        return "" + this.condition.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isCacheable()
     */
    public boolean isCacheable()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#isResultValid(org.apache.maven.enforcer.rule.api.EnforcerRule)
     */
    public boolean isResultValid( EnforcerRule theCachedRule )
    {
        return false;
    }

}
