package org.apache.maven.plugins.enforcer.utils;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.enforcer.EnforcerExpressionEvaluator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

public class MockEnforcerExpressionEvaluator
    extends EnforcerExpressionEvaluator
{

    public MockEnforcerExpressionEvaluator( MavenSession theContext, PathTranslator thePathTranslator,
                                            MavenProject theProject )
    {
        super( theContext, thePathTranslator, theProject );
        // TODO Auto-generated constructor stub
    }

    public Object evaluate( String expr )
        throws ExpressionEvaluationException
    {
        if (expr !=null)
        {
        //just remove the ${ } and return the name as the value
        return expr.replaceAll( "\\$\\{|}", "" );
        }
        else
        {
            return expr;
        }
    }
    

}
