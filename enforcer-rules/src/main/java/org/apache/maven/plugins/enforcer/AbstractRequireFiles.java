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
    private File[] files;

    /** if null file handles should be allowed. If they are allowed, it means treat it as a success. */
    private boolean allowNulls = false;

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

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {

        if ( !allowNulls && files.length == 0 )
        {
            throw new EnforcerRuleException( "The file list is empty and Null files are disabled." );
        }

        List<File> failures = new ArrayList<>();
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
                buf.append( message + System.lineSeparator() );
            }
            buf.append( getErrorMsg() );

            for ( File file : failures )
            {
                if ( file != null )
                {
                    buf.append( file.getAbsolutePath() + System.lineSeparator() );
                }
                else
                {
                    buf.append( "(an empty filename was given and allowNulls is false)" + System.lineSeparator() );
                }
            }

            throw new EnforcerRuleException( buf.toString() );
        }
    }

    @Override
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
            for ( Object item : items )
            {
                hash = 31 * hash + ( item == null ? 0 : item.hashCode() );
            }
        }
        return hash;
    }

    @Override
    public boolean isCacheable()
    {
        return true;
    }

    @Override
    public boolean isResultValid( EnforcerRule cachedRule )
    {
        return true;
    }

    public File[] getFiles()
    {
        return files;
    }

    public void setFiles( File[] files )
    {
        this.files = files;
    }

    public boolean isAllowNulls()
    {
        return allowNulls;
    }

    public void setAllowNulls( boolean allowNulls )
    {
        this.allowNulls = allowNulls;
    }
}
