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

import org.apache.maven.model.Model;
import org.apache.maven.project.path.PathTranslator;

/**
 * The Class MockPathTranslator.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 */
public class MockPathTranslator
    implements PathTranslator
{

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.path.PathTranslator#alignToBaseDirectory(org.apache.maven.model.Model,
     *      java.io.File)
     */
    public void alignToBaseDirectory( Model theModel, File theBasedir )
    {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.path.PathTranslator#alignToBaseDirectory(java.lang.String, java.io.File)
     */
    public String alignToBaseDirectory( String thePath, File theBasedir )
    {
        return theBasedir.getAbsolutePath();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.path.PathTranslator#unalignFromBaseDirectory(org.apache.maven.model.Model,
     *      java.io.File)
     */
    public void unalignFromBaseDirectory( Model theModel, File theBasedir )
    {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.project.path.PathTranslator#unalignFromBaseDirectory(java.lang.String, java.io.File)
     */
    public String unalignFromBaseDirectory( String theDirectory, File theBasedir )
    {
        return theBasedir.getAbsolutePath();
    }

}
