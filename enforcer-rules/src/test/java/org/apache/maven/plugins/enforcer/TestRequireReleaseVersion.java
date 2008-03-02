package org.apache.maven.plugins.enforcer;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.testing.ArtifactStubFactory;
import org.apache.maven.plugins.enforcer.utils.TestEnforcerRuleUtils;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class TestRequireReleaseVersion
    extends TestCase
{
    public void testMojo() throws IOException
    {
        ArtifactStubFactory factory = new ArtifactStubFactory();
        MockProject project = new MockProject();
        EnforcerRuleHelper helper = EnforcerTestUtils.getHelper( project );
        
        project.setArtifact( factory.getReleaseArtifact() );
        
        EnforcerRule rule = new RequireReleaseVersion();
        
        TestEnforcerRuleUtils.execute( rule, helper, false );
        
        project.setArtifact( factory.getSnapshotArtifact() );
        
        TestEnforcerRuleUtils.execute( rule, helper, true );
        
    }
    
    public void testCache()
    {
        EnforcerRule rule = new RequireReleaseVersion();
        assertFalse( rule.isCacheable() );
        assertFalse(rule.isResultValid(null));
        assertEquals( "0", rule.getCacheId() );   
    }
    

}
