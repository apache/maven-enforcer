package org.apache.maven.plugins.enforcer;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;

/**
 * Abstract enforcer rule that give a foundation to validate properties from multiple sources.
 * 
 * @author Paul Gier
 * @author <a href='mailto:marvin[at]marvinformatics[dot]com'>Marvin Froeder</a>
 * @version $Id: AbstractPropertyEnforcerRule.java $
 */
public abstract class AbstractPropertyEnforcerRule
    extends AbstractNonCacheableEnforcerRule
{

    /**
     * Match the property value to a given regular expression. Defaults to <code>null</code> (any value is ok).
     */
    public String regex = null;

    /** Specify a warning message if the regular expression is not matched. */
    public String regexMessage = null;

    public AbstractPropertyEnforcerRule()
    {
        super();
    }

    /**
     * Execute the rule.
     * 
     * @param helper the helper
     * @throws EnforcerRuleException the enforcer rule exception
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        Object propValue = resolveValue( helper );

        // Check that the property is not null or empty string
        if ( propValue == null )
        {
            if ( message == null )
            {
                message = getName() + " \"" + getPropertyName() + "\" is required for this build.";
            }
            throw new EnforcerRuleException( message );
        }
        // If there is a regex, check that the property matches it
        if ( regex != null && !propValue.toString().matches( regex ) )
        {
            if ( regexMessage == null )
            {
                regexMessage =
                    getName() + " \"" + getPropertyName() + "\" evaluates to \"" + propValue + "\".  "
                        + "This does not match the regular expression \"" + regex + "\"";
            }
            throw new EnforcerRuleException( regexMessage );
        }
    }

    /**
     * How the property that is being evaluated is called
     */
    public abstract String getName();

    /**
     * The name of the property currently being evaluated, this is used for default message pourpouses only
     */
    public abstract String getPropertyName();

    /**
     * Resolves the property value
     * 
     * @param helper
     * @throws EnforcerRuleException
     */
    public abstract Object resolveValue( EnforcerRuleHelper helper )
        throws EnforcerRuleException;

}