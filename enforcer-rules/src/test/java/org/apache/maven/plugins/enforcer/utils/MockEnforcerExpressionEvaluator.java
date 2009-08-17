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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.enforcer.EnforcerExpressionEvaluator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

// TODO: Auto-generated Javadoc
/**
 * The Class MockEnforcerExpressionEvaluator.
 */
public class MockEnforcerExpressionEvaluator
    extends EnforcerExpressionEvaluator
{

    /**
     * Instantiates a new mock enforcer expression evaluator.
     *
     * @param theContext the the context
     * @param thePathTranslator the the path translator
     * @param theProject the the project
     */
    public MockEnforcerExpressionEvaluator( MavenSession theContext, PathTranslator thePathTranslator,
                                            MavenProject theProject )
    {
        super( theContext, thePathTranslator, theProject );
        // TODO Auto-generated constructor stub
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.plugin.PluginParameterExpressionEvaluator#evaluate(java.lang.String)
     */
    public Object evaluate( String expr )
        throws ExpressionEvaluationException
    {
        if ( expr != null )
        {
            // just remove the ${ } and return the name as the value
            return expr.replaceAll( "\\$\\{|}", "" );
        }
        else
        {
            return expr;
        }
    }

}
