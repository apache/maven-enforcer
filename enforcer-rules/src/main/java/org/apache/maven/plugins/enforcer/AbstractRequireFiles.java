package org.apache.maven.plugins.enforcer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
abstract public class AbstractRequireFiles
    extends AbstractStandardEnforcerRule
{
    /**
     * Array of files to check.
     */
    File[] files;

    //check the file for the specific condition
    abstract boolean checkFile(File file);
    
    //retun standard error message
    abstract String getErrorMsg();
    
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {

        ArrayList failures = new ArrayList();
        for ( int i = 0; i < files.length; i++ )
        {
            if ( !checkFile(files[i]) )
            {
                failures.add( files[i]);
            }
        }

        // if anything was found, log it then append the
        // optional message.
        if ( !failures.isEmpty() )
        {
            StringBuffer buf = new StringBuffer();
            if ( message != null )
            {
                buf.append( message + "\n" );
            }
            buf.append( getErrorMsg() );

            Iterator iter = failures.iterator();
            while ( iter.hasNext() )
            {
                buf.append( ((File)(iter.next())).getAbsolutePath()+ "\n" );
            }

            throw new EnforcerRuleException( buf.toString() );
        }
    }

    /**
     * If your rule is cacheable, you must return a unique id when parameters or conditions change that would cause the
     * result to be different. Multiple cached results are stored based on their id. The easiest way to do this is to
     * return a hash computed from the values of your parameters. If your rule is not cacheable, then the result here is
     * not important, you may return anything.
     */
    public String getCacheId()
    {
        return Integer.toString( hashCode( files ) );
    }

    /**
     * Calculates a hash code for the specified array as <code>Arrays.hashCode()</code> would do. Unfortunately, the
     * mentioned method is only available for Java 1.5 and later.
     * 
     * @param items The array for which to compute the hash code, may be <code>null</code>.
     * @return The hash code for the array.
     */
    private static int hashCode( Object[] items )
    {
        int hash = 0;
        if ( items != null )
        {
            hash = 1;
            for ( int i = 0; i < items.length; i++ )
            {
                Object item = items[i];
                hash = 31 * hash + ( item == null ? 0 : item.hashCode() );
            }
        }
        return hash;
    }

    /**
     * This tells the system if the results are cacheable at all. Keep in mind that during forked builds and other
     * things, a given rule may be executed more than once for the same project. This means that even things that change
     * from project to project may still be cacheable in certain instances.
     */
    public boolean isCacheable()
    {
        return true;
    }

    /**
     * If the rule is cacheable and the same id is found in the cache, the stored results are passed to this method to
     * allow double checking of the results. Most of the time this can be done by generating unique ids, but sometimes
     * the results of objects returned by the helper need to be queried. You may for example, store certain objects in
     * your rule and then query them later.
     */
    public boolean isResultValid( EnforcerRule arg0 )
    {
        return true;
    }
}
