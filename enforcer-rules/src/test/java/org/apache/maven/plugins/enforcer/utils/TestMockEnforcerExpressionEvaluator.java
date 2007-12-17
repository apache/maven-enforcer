package org.apache.maven.plugins.enforcer.utils;

import junit.framework.TestCase;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.enforcer.EnforcerExpressionEvaluator;
import org.apache.maven.plugins.enforcer.EnforcerTestUtils;
import org.apache.maven.plugins.enforcer.MockPathTranslator;
import org.apache.maven.plugins.enforcer.MockProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

public class TestMockEnforcerExpressionEvaluator
    extends TestCase
{

    public void testEvaluate()
    {
        MavenSession session = EnforcerTestUtils.getMavenSession();

        EnforcerExpressionEvaluator ev =
            new MockEnforcerExpressionEvaluator( session, new MockPathTranslator(), new MockProject() );
        assertMatch( ev, "SNAPSHOT" );
        assertMatch( ev, "RELEASE" );
        assertMatch( ev, "SNAPSHOT" );
        assertMatch( ev, "LATEST" );
        assertMatch( ev, "1.0" );
    }

    public void assertMatch( EnforcerExpressionEvaluator ev, String exp )
    {
        // the mock enforcer should return the name of the expression as the value.
        try
        {
            assertEquals( exp, ev.evaluate( "${" + exp + "}") );
        }
        catch ( ExpressionEvaluationException e )
        {
            fail(e.getLocalizedMessage());
        }
    }
}
