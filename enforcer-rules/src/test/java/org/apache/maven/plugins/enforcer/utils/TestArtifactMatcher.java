package org.apache.maven.plugins.enforcer.utils;

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

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugins.enforcer.utils.ArtifactMatcher.Pattern;
import java.util.ArrayList;
import java.util.Collection;

public class TestArtifactMatcher extends TestCase
{
	private ArtifactMatcher matcher;
	
	Collection<String> patterns = new ArrayList<String>();
	
	Collection<String> ignorePatterns = new ArrayList<String>();
	
	public void testPatternInvalidInput() throws InvalidVersionSpecificationException
	{
		try
		{
			new Pattern(null);
			fail("NullPointerException expected.");
		}
		catch(NullPointerException e){}
		
		try
		{
			new Pattern("a:b:c:d:e:f:g");
			fail("IllegalArgumentException expected.");
		}
		catch(IllegalArgumentException e){}
		
		try
		{
			new Pattern("a::");
			fail("IllegalArgumentException expected.");
		}
		catch(IllegalArgumentException e){}
		
		try
		{
			Pattern p = new Pattern("*");
			p.match(null);
			fail("NullPointerException expected.");
		}
		catch(NullPointerException e){}
	}

	public void testPattern() throws InvalidVersionSpecificationException
	{
		executePatternMatch("groupId:artifactId:1.0:jar:compile", "groupId", "artifactId", "1.0", "compile", "jar", true);
		
		executePatternMatch("groupId:artifactId:1.0:jar:compile", "groupId", "artifactId", "1.0", "", "", true);
		
		executePatternMatch("groupId:artifactId:1.0", "groupId", "artifactId", "1.0", "", "", true);
		
		executePatternMatch("groupId:artifactId:1.0", "groupId", "artifactId", "1.1", "", "", true);
		
		executePatternMatch("groupId:artifactId:[1.0]", "groupId", "artifactId", "1.1", "", "", false);
		
		executePatternMatch("groupId:*:1.0", "groupId", "artifactId", "1.0", "test", "", true);
		
		executePatternMatch("*:*:1.0", "groupId", "artifactId", "1.0", "", "", true);
		
		executePatternMatch("*:artifactId:*", "groupId", "artifactId", "1.0", "", "", true);
		
		executePatternMatch("*", "groupId", "artifactId", "1.0", "", "", true);
		
		// MENFORCER-74/75
		executePatternMatch("*:*:*:jar:compile:tests", "groupId", "artifactId", "1.0", "", "", "tests", true);
		
		// MENFORCER-83
        executePatternMatch("*upId", "groupId", "artifactId", "1.0", "", "", true);
        
        executePatternMatch("gr*pId:?rt?f?ct?d:1.0", "groupId", "artifactId", "1.0", "", "", true);
        
        executePatternMatch("org.apache.*:maven-*:*", "org.apache.maven", "maven-core", "3.0", "", "", true);
	}
	
	public void testMatch() throws InvalidVersionSpecificationException
	{
		patterns.add("groupId:artifactId:1.0");
		patterns.add("*:anotherArtifact");
		
		ignorePatterns.add("badGroup:*:*:test");
		ignorePatterns.add("*:anotherArtifact:1.1");
		
		matcher = new ArtifactMatcher(patterns, ignorePatterns);
		
		executeMatch(matcher, "groupId", "artifactId", "1.0", "", "", true);	
		
		executeMatch(matcher, "groupId", "anotherArtifact", "1.0", "", "", true);	
		
		executeMatch(matcher, "badGroup", "artifactId", "1.0", "", "test", false);
		
		executeMatch(matcher, "badGroup", "anotherArtifact", "1.0", "", "", true);	
		
		executeMatch(matcher, "groupId", "anotherArtifact", "1.1", "", "", false);	
	}
	
    private void executePatternMatch( final String pattern, final String groupId, final String artifactId,
                                      final String versionRange, final String scope, final String type,
                                      boolean expectedResult )
        throws InvalidVersionSpecificationException
    {
        executePatternMatch( pattern, groupId, artifactId, versionRange, scope, type, "", expectedResult );
    }

    private void executePatternMatch( final String pattern, final String groupId, final String artifactId,
                                      final String versionRange, final String scope, final String type,
                                      final String classifier, boolean expectedResult )
        throws InvalidVersionSpecificationException
    {
        assertEquals( expectedResult, new ArtifactMatcher.Pattern( pattern ).match( createMockArtifact( groupId,
                                                                                                        artifactId,
                                                                                                        versionRange,
                                                                                                        scope, type,
                                                                                                        classifier ) ) );
    }

    private void executeMatch( final ArtifactMatcher matcher, final String groupId, final String artifactId,
                               final String versionRange, final String scope, final String type,
                               final boolean expectedResult )
        throws InvalidVersionSpecificationException
    {
        assertEquals( expectedResult,
                      matcher.match( createMockArtifact( groupId, artifactId, versionRange, scope, type, "" ) ) );
    }

    private static Artifact createMockArtifact( final String groupId, final String artifactId,
                                                final String versionRange, final String scope, final String type,
                                                final String classifier )
    {
        ArtifactHandler artifactHandler = new DefaultArtifactHandler();

        VersionRange version = VersionRange.createFromVersion( versionRange );
        return new DefaultArtifact( groupId, artifactId, version, scope, type, classifier, artifactHandler );
    }
}
