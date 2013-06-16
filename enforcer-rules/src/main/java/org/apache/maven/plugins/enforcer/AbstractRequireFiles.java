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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;

/**
 * Contains the common code to compare an array of files against a requirement.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public abstract class AbstractRequireFiles
    extends AbstractStandardEnforcerRule
{

    /** Array of files to check. */
    File[] files;

    /** if null file handles should be allowed. If they are allowed, it means treat it as a success. */
    boolean allowNulls = false;

    // check the file for the specific condition
    /**
     * Check one file.
     *
     * @param file the file
     * @return <code>true</code> if successful
     */
    abstract boolean checkFile( File file );

    // return standard error message
    /**
     * Gets the error msg.
     *
     * @return the error msg
     */
    abstract String getErrorMsg();

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.enforcer.rule.api.EnforcerRule#execute(org.apache.maven.enforcer.rule.api.EnforcerRuleHelper)
     */
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {

        if ( !allowNulls && files.length == 0 )
        {
            throw new EnforcerRuleException( "The file list is empty and Null files are disabled." );
        }

        List<File> failures = new ArrayList<File>();
        for ( File file : files )
        {
            if ( !allowNulls && file == null )
            {
                failures.add( file );
            }
            else if ( !checkFile( file ) )
            {
                failures.add( file );
            }
        }

        // if anything was found, log it with the optional message.
        if ( !failures.isEmpty() )
        {
            String message = getMessage();
            
            StringBuilder buf = new StringBuilder();
            if ( message != null )
            {
                buf.append( message + "\n" );
            }
            buf.append( getErrorMsg() );

            for ( File file : failures )
            {
                if ( file != null )
                {
                    buf.append( file.getAbsolutePath() + "\n" );
                }
                else
                {
                    buf.append( "(an empty filename was given and allowNulls is false)\n" );
                }
            }

            throw new EnforcerRuleException( buf.toString() );
        }
    }

    /**
     * If your rule is cacheable, you must return a unique id when parameters or conditions change that would cause the
     * result to be different. Multiple cached results are stored based on their id. The easiest way to do this is to
     * return a hash computed from the values of your parameters. If your rule is not cacheable, then the result here is
     * not important, you may return anything.
     *
     * @return the cache id
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
     *
     * @return <code>true</code> if rule is cacheable
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
     *
     * @param cachedRule the cached rule
     * @return <code>true</code> if the stored results are valid for the same id.
     */
    public boolean isResultValid( EnforcerRule cachedRule )
    {
        return true;
    }
}
