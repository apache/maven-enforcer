/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.enforcer.internal;

import javax.inject.Provider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnforcerRuleCacheTest {

    @Test
    void cachesRulesAcrossInstancesUsingTheSharedSessionDataLock() throws Exception {
        SessionData sessionData = mock(SessionData.class);
        Map<Object, Object> values = new HashMap<>();
        when(sessionData.get(any())).thenAnswer(invocation -> {
            assertThat(Thread.holdsLock(sessionData)).isTrue();
            return values.get(invocation.getArgument(0));
        });
        doAnswer(invocation -> {
                    assertThat(Thread.holdsLock(sessionData)).isTrue();
                    values.put(invocation.getArgument(0), invocation.getArgument(1));
                    return null;
                })
                .when(sessionData)
                .set(any(), any());

        RepositorySystemSession repositorySession = mock(RepositorySystemSession.class);
        when(repositorySession.getData()).thenReturn(sessionData);
        MavenSession mavenSession = mock(MavenSession.class);
        when(mavenSession.getRepositorySession()).thenReturn(repositorySession);
        Provider<MavenSession> sessionProvider = () -> mavenSession;
        EnforcerRuleCache cacheFromFirstRealm = new EnforcerRuleCache(sessionProvider);
        EnforcerRuleCache cacheFromSecondRealm = new EnforcerRuleCache(sessionProvider);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return cacheFromFirstRealm.isCached(new FirstCachedRule());
            });
            Future<Boolean> second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return cacheFromSecondRealm.isCached(new SecondCachedRule());
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).isFalse();
            assertThat(second.get(5, TimeUnit.SECONDS)).isFalse();
        } finally {
            executor.shutdownNow();
        }

        assertThat(cacheFromFirstRealm.isCached(new FirstCachedRule())).isTrue();
        assertThat(cacheFromSecondRealm.isCached(new SecondCachedRule())).isTrue();
    }

    private static class FirstCachedRule extends AbstractEnforcerRule {
        @Override
        public String getCacheId() {
            return "first";
        }

        @Override
        public void execute() throws EnforcerRuleException {}
    }

    private static class SecondCachedRule extends AbstractEnforcerRule {
        @Override
        public String getCacheId() {
            return "second";
        }

        @Override
        public void execute() throws EnforcerRuleException {}
    }
}
