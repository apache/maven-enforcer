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

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * Rule for Maven Enforcer using Beanshell to evaluate a conditional expression.
 *
 * @author hugonnem
 */
public class EvaluateBeanshell
    extends AbstractNonCacheableEnforcerRule
{

    /** Beanshell interpreter. */
    private static final ThreadLocal<Interpreter> INTERPRETER = new ThreadLocal<Interpreter>()
    {
        @Override
        protected Interpreter initialValue()
        {
            return new Interpreter();
        }
    };

    /** The condition to be evaluated.
     *  
     * @see {@link #setCondition(String)}
     * @see {@link #getCondition()}
     * */
    private String condition;

    public final void setCondition( String condition )
    {
        this.condition = condition;
    }
    
    public final String getCondition()
    {
        return condition;
    }

    @Override
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
                String message = getMessage();
                if ( StringUtils.isEmpty( message ) )
                {
                    message = "The expression \"" + condition + "\" is not true.";
                }
                throw new EnforcerRuleException( message );
            }
        }
        catch ( ExpressionEvaluationException e )
        {
            throw new EnforcerRuleException( "Unable to evaluate an expression '" + condition + "'", e );
        }
    }

    /**
     * Evaluate expression using Beanshell.
     *
     * @param script the expression to be evaluated
     * @param log the logger
     * @return boolean the evaluation of the expression
     * @throws EnforcerRuleException if the script could not be evaluated
     */
    protected boolean evaluateCondition( String script, Log log )
        throws EnforcerRuleException
    {
        Boolean evaluation;
        try
        {
            evaluation = (Boolean) INTERPRETER.get().eval( script );
            log.debug( "Echo evaluating : " + evaluation );
        }
        catch ( EvalError ex )
        {
            throw new EnforcerRuleException( "Couldn't evaluate condition: " + script, ex );
        }
        return evaluation.booleanValue();
    }
}
