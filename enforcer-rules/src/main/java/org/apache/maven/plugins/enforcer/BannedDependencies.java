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

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.enforcer.utils.ArtifactMatcher;
import org.apache.maven.plugins.enforcer.utils.ArtifactMatcher.Pattern;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This rule checks that lists of dependencies are not included.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class BannedDependencies
    extends AbstractBanDependencies
{

    private static final Integer STATUS_SUCCESS = 200;
    
    /**
     * Specify the banned dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard 
     * by using '*' (ie group:*:1.0) <br>
     * The rule will fail if any dependency matches any exclude, unless it also matches 
     * an include rule.
     * 
     * @see {@link #setExcludes(List)}
     * @see {@link #getExcludes()}
     * 
     * @see {@link #excludesUrl}
     */
    private List<String> excludes = null;
    
    /**
     * URL pointing to a list of banned dependencies at a specific location. This can be 
     * a list of artifacts in the format <code>groupId[:artifactId][:version]</code>. 
     * Any of the sections can be a wildcard by using '*' (ie group:*:1.0) <br> 
     * Either <b>excludes</b> or <b>excludesUrl</b> or both can be used.
     * If both are used, all banned dependencies specified at this <b>excludesUrl</b> location 
     * will be combined with the list of banned dependencies specified in <b>excludes</b>.
     * The rule will fail if any dependency matches any exclude, unless it also matches 
     * an include rule.
     * 
     * @see {@link #setExcludesUrl(String)}
     * @see {@link #getExcludesUrl()}
     */
    private String excludesUrl = null;

    /**
     * Specify the allowed dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard 
     * by using '*' (ie group:*:1.0) <br>
     * Includes override the exclude rules. It is meant to allow wide exclusion rules 
     * with wildcards and still allow a
     * smaller set of includes. <br>
     * For example, to ban all xerces except xerces-api -> exclude "xerces", include "xerces:xerces-api"
     * 
     * @see {@link #setIncludes(List)}
     * @see {@link #getIncludes()}
     */
    private List<String> includes = null;
    
    /**
     * {@inheritDoc}
     */
    protected Set<Artifact> checkDependencies( Set<Artifact> theDependencies, Log log )
        throws EnforcerRuleException
    {
        log.info( "excludesUrl=" + excludesUrl + ", excludes=" + excludes );
        log.info( "No of excludes before reading from excludesUrl=" 
                      + ( ( excludes != null ) ? excludes.size() : "0" ) );
        
        if ( excludesUrl != null && !excludesUrl.isEmpty() )
        {
          fetchExcludedList( log );
        }
        
        log.info( "No of excludes after reading from excludesUrl=" + ( ( excludes != null ) ? excludes.size() : "0" ) );
        
        Set<Artifact> excluded = checkDependencies( theDependencies, excludes );
        
        log.debug( "No of excluded artifacts = " + ( excluded != null ? excluded.size() : 0 ) ); 
        
        // anything specifically included should be removed
        // from the ban list.
        if ( excluded != null )
        {
            Set<Artifact> included = checkDependencies( theDependencies, includes );

            if ( included != null )
            {
                excluded.removeAll( included );
            }
        }
        return excluded;

    }
    
    private void fetchExcludedList ( Log logger ) throws EnforcerRuleException
    {
      logger.debug( "Inside getExcludedList()..." );
      int timeout = 3 * 1000; // milliseconds
      CloseableHttpClient httpClient = null;
      HttpRequestBase httpRequest = null;
      try
      {
       
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout( timeout )
                                                      .setSocketTimeout( timeout ).build();
        httpClient = HttpClientBuilder.create()
                              .setDefaultRequestConfig( requestConfig )
                              .setMaxConnPerRoute( 1000 ).setMaxConnTotal( 1000 )
                              .setRetryHandler( new DefaultHttpRequestRetryHandler() )
                              .build( );
      
        httpRequest = new HttpGet( excludesUrl );
        
        HttpResponse response = httpClient.execute( httpRequest );
        int statusCode = 0;
        String line = null;
        if ( response != null )
        {
          if ( response.getStatusLine( ) != null )
          {
            statusCode = response.getStatusLine( ).getStatusCode( );
            logger.info( "Response status code from excludesUrl: " + statusCode );
          }
          
          if ( statusCode == STATUS_SUCCESS && response.getEntity() != null )
          {
            BufferedReader br = new BufferedReader( new InputStreamReader( response.getEntity().getContent() ) );
      
            if ( br != null ) 
            {
              while ( ( line = br.readLine() ) != null ) 
              {
                if ( excludes == null )
                {
                  excludes = new ArrayList<String>();
                }
                if ( line != null && !line.trim().isEmpty() )
                {
                  logger.debug( "blacklisted artifact=" + line );
                  excludes.add( line );
                }
              }
                
            }
          }
        }
        
      }
      catch ( UnknownHostException uhe )
      {
        logger.info( "Invalid excludesUrl value --> " + excludesUrl );
        throw new EnforcerRuleException( "Invalid excludesUrl value --> " + excludesUrl, uhe );
      }
      catch ( Exception e )
      {
        logger.info( "Error while reading from excludesUrl, error=" + e.getMessage() );
        throw new EnforcerRuleException( "Error reading blacklist artifacts from url " + excludesUrl, e );
      }
      finally
      {
        httpRequest.releaseConnection();
        try 
        {
          if ( httpClient != null )
          {
            httpClient.close();
          }
        } 
        catch ( Exception e ) 
        {
          logger.info( "Error occured while closing http client, error=" + e.getMessage() );
        }
      }
      
    }

    /**
     * Checks the set of dependencies against the list of patterns.
     * 
     * @param thePatterns the patterns
     * @param dependencies the dependencies
     * @return a set containing artifacts matching one of the patterns or <code>null</code>
     * @throws EnforcerRuleException the enforcer rule exception
     */
    private Set<Artifact> checkDependencies( Set<Artifact> dependencies, List<String> thePatterns )
        throws EnforcerRuleException
    {
        Set<Artifact> foundMatches = null;
        
        if ( thePatterns != null && thePatterns.size() > 0 )
        {

            for ( String pattern : thePatterns )
            {
                String[] subStrings = pattern.split( ":" );
                subStrings = StringUtils.stripAll( subStrings );
                String resultPattern = StringUtils.join( subStrings, ":" );

                for ( Artifact artifact : dependencies )
                {
                    if ( compareDependency( resultPattern, artifact ) )
                    {
                        // only create if needed
                        if ( foundMatches == null )
                        {
                            foundMatches = new HashSet<Artifact>();
                        }
                        foundMatches.add( artifact );
                    }
                }
            }
        }
        return foundMatches;
    }

    /**
     * Compares the given pattern against the given artifact. The pattern should follow the format
     * <code>groupId:artifactId:version:type:scope:classifier</code>.
     * 
     * @param pattern The pattern to compare the artifact with.
     * @param artifact the artifact
     * @return <code>true</code> if the artifact matches one of the patterns
     * @throws EnforcerRuleException the enforcer rule exception
     */
    protected boolean compareDependency( String pattern, Artifact artifact )
        throws EnforcerRuleException
    {

        ArtifactMatcher.Pattern am = new Pattern( pattern );
        boolean result;
        try
        {
            result = am.match( artifact );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new EnforcerRuleException( "Invalid Version Range: ", e );
        }

        return result;
    }

    /**
     * Gets the excludes.
     * 
     * @return the excludes
     */
    public List<String> getExcludes()
    {
        return this.excludes;
    }

    /**
     * Specify the banned dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard 
     * by using '*' (ie group:*:1.0) <br>
     * The rule will fail if any dependency matches any exclude, unless it also matches an 
     * include rule.
     * 
     * @see #getExcludes()
     * @param theExcludes the excludes to set
     */
    public void setExcludes( List<String> theExcludes )
    {
        this.excludes = theExcludes;
    }

    /**
     * Gets the includes.
     * 
     * @return the includes
     */
    public List<String> getIncludes()
    {
        return this.includes;
    }

    /**
     * Specify the allowed dependencies. This can be a list of artifacts in the format
     * <code>groupId[:artifactId][:version]</code>. Any of the sections can be a wildcard 
     * by using '*' (ie group:*:1.0) <br>
     * Includes override the exclude rules. It is meant to allow wide exclusion rules with 
     * wildcards and still allow a
     * smaller set of includes. <br>
     * For example, to ban all xerces except xerces-api -> exclude "xerces", 
     * include "xerces:xerces-api"
     * 
     * @see #setIncludes(List)
     * @param theIncludes the includes to set
     */
    public void setIncludes( List<String> theIncludes )
    {
        this.includes = theIncludes;
    }

    public String getExcludesUrl() 
    {
      return excludesUrl;
    }
    

    public void setExcludesUrl( String excludesUrl ) 
    {
      this.excludesUrl = excludesUrl;
    }
    
    
    

}
