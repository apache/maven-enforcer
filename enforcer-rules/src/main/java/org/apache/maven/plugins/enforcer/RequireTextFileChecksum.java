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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugins.enforcer.utils.NormalizeLineSeparatorReader;
import org.apache.maven.plugins.enforcer.utils.NormalizeLineSeparatorReader.LineSeparator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * Rule to validate a text file to match the specified checksum.
 *
 * @author Konrad Windszus
 * @see RequireFileChecksum
 */
public class RequireTextFileChecksum
    extends RequireFileChecksum
{
    private NormalizeLineSeparatorReader.LineSeparator normalizeLineSeparatorTo = LineSeparator.UNIX;

    Charset encoding;

    public void setNormalizeLineSeparatorTo( NormalizeLineSeparatorReader.LineSeparator normalizeLineSeparatorTo )
    {
        this.normalizeLineSeparatorTo = normalizeLineSeparatorTo;
    }

    public void setEncoding( String encoding )
    {
        this.encoding = Charset.forName( encoding );
    }

    @Override
    public void execute( EnforcerRuleHelper helper )
        throws EnforcerRuleException
    {
        // set defaults
        if ( encoding == null )
        {
            // https://maven.apache.org/plugins/maven-resources-plugin/examples/encoding.html
            try
            {
                String encoding = (String) helper.evaluate( "${project.build.sourceEncoding}" );
                if ( StringUtils.isBlank( encoding ) )
                {
                    encoding = System.getProperty( "file.encoding" );
                    helper.getLog().warn( "File encoding has not been set, using platform encoding " + encoding
                        + ". Build is platform dependent!" );
                }
                this.encoding = Charset.forName( encoding );
            }
            catch ( ExpressionEvaluationException e )
            {
                throw new EnforcerRuleException( "Unable to retrieve the project's build source encoding "
                    + "(${project.build.sourceEncoding}): ", e );
            }
        }
        super.execute( helper );
    }

    @Override
    protected String calculateChecksum()
        throws EnforcerRuleException
    {
        try ( Reader reader = new NormalizeLineSeparatorReader( Files.newBufferedReader( file.toPath(), encoding ),
                                                                normalizeLineSeparatorTo );
                        InputStream inputStream = new ReaderInputStream( reader, encoding ) )
        {
            return super.calculateChecksum( inputStream );
        }
        catch ( IOException e )
        {
            throw new EnforcerRuleException( "Unable to calculate checksum (with normalized line separators)", e );
        }
    }
}
