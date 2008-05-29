package org.apache.maven.plugins.enforcer;

import junit.framework.TestCase;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;

/**
 * Test AlwaysPass rule.
 * @author Ben Lidgey
 * @see AlwaysPass
 */
public class TestAlwaysPass extends TestCase
{

    public void testExecute()
    {
        final AlwaysPass rule = new AlwaysPass();
        try
        {
            // execute rule -- should NOT throw EnforcerRuleException
            rule.execute( EnforcerTestUtils.getHelper() );
            assertTrue( true );
        }
        catch ( EnforcerRuleException e )
        {
            fail( "Should NOT throw EnforcerRuleException" );
        }
    }

}
