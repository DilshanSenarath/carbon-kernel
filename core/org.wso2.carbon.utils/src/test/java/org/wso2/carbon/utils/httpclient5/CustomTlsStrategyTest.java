/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.utils.httpclient5;

import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContexts;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.BaseTest;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

/**
 * Unit tests for CustomTlsStrategy class.
 * Verifies that the TLS strategy is properly instantiated with valid parameters
 * and rejects invalid configurations.
 */
public class CustomTlsStrategyTest extends BaseTest {

    @Test
    public void testCustomTlsStrategyWithValidParameters() {

        SSLContext sslContext = SSLContexts.createDefault();
        HostnameVerifier hostnameVerifier = LocalhostSANsTrustedHostnameVerifier.getInstance();
        CustomTlsStrategy customTlsStrategy = new CustomTlsStrategy(sslContext, hostnameVerifier);
        Assert.assertNotNull(customTlsStrategy, "CustomTlsStrategy should be created successfully");
    }

    @Test(expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = ".*SSLContext cannot be null.*")
    public void testCustomTlsStrategyWithNullSSLContext() {

        SSLContext sslContext = null;
        HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
        new CustomTlsStrategy(sslContext, hostnameVerifier);
    }

    @Test(expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = ".*HostnameVerifier cannot be null.*")
    public void testCustomTlsStrategyWithNullHostnameVerifier() {

        SSLContext sslContext = SSLContexts.createDefault();
        HostnameVerifier hostnameVerifier = null;
        new CustomTlsStrategy(sslContext, hostnameVerifier);
    }
}
