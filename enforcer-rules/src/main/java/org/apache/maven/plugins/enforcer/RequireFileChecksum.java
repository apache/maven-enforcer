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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;

/**
 * Rule to validate a binary file to match the specified checksum.
 *
 * @author Edward Samson
 * @author Lyubomyr Shaydariv
 * @see RequireTextFileChecksum
 */
public class RequireFileChecksum
    extends AbstractNonCacheableEnforcerRule
{

    protected File file;

    private String checksum;

    private String type;

    private String nonexistentFileMessage;

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        if ( this.file == null )
        {
            throw new EnforcerRuleException( "Input file unspecified" );
        }

        if ( this.type == null )
        {
            throw new EnforcerRuleException( "Hash type unspecified" );
        }

        if ( this.checksum == null )
        {
            throw new EnforcerRuleException( "Checksum unspecified" );
        }

        if ( !this.file.exists() )
        {
            String message = nonexistentFileMessage;
            if ( message == null )
            {
                message = "File does not exist: " + this.file.getAbsolutePath();
            }
            throw new EnforcerRuleException( message );
        }

        if ( this.file.isDirectory() )
        {
            throw new EnforcerRuleException( "Cannot calculate the checksum of directory: "
                + this.file.getAbsolutePath() );
        }

        if ( !this.file.canRead() )
        {
            throw new EnforcerRuleException( "Cannot read file: " + this.file.getAbsolutePath() );
        }

        String checksum = calculateChecksum();

        if ( !checksum.equalsIgnoreCase( this.checksum ) )
        {
            String exceptionMessage = getMessage();
            if ( exceptionMessage == null )
            {
                exceptionMessage = this.type + " hash of " + this.file + " was " + checksum
                    + " but expected " + this.checksum;
            }
            throw new EnforcerRuleException( exceptionMessage );
        }
    }

    /**
     * The file to check.
     *
     * @param file file
     */
    public void setFile( File file )
    {
        this.file = file;
    }

    /**
     * The expected checksum value.
     *
     * @param checksum checksum
     */
    public void setChecksum( String checksum )
    {
        this.checksum = checksum;
    }

    /**
     * The checksum algorithm to use. Possible values: "md5", "sha1", "sha256", "sha384", "sha512".
     *
     * @param type algorithm
     */
    public void setType( String type )
    {
        this.type = type;
    }

    /**
     * The friendly message to use when the file does not exist.
     *
     * @param nonexistentFileMessage message
     */
    public void setNonexistentFileMessage( String nonexistentFileMessage )
    {
        this.nonexistentFileMessage = nonexistentFileMessage;
    }

    protected String calculateChecksum()
        throws EnforcerRuleException
    {
        try ( InputStream inputStream = new FileInputStream( this.file ) )
        {
            return calculateChecksum( inputStream );
        }
        catch ( IOException e )
        {
            throw new EnforcerRuleException( "Unable to calculate checksum", e );
        }
    }

    protected String calculateChecksum( InputStream inputStream )
        throws IOException, EnforcerRuleException
    {
        String checksum;
        if ( "md5".equals( this.type ) )
        {
            checksum = DigestUtils.md5Hex( inputStream );
        }
        else if ( "sha1".equals( this.type ) )
        {
            checksum = DigestUtils.sha1Hex( inputStream );
        }
        else if ( "sha256".equals( this.type ) )
        {
            checksum = DigestUtils.sha256Hex( inputStream );
        }
        else if ( "sha384".equals( this.type ) )
        {
            checksum = DigestUtils.sha384Hex( inputStream );
        }
        else if ( "sha512".equals( this.type ) )
        {
            checksum = DigestUtils.sha512Hex( inputStream );
        }
        else
        {
            throw new EnforcerRuleException( "Unsupported hash type: " + this.type );
        }
        return checksum;
    }
}
