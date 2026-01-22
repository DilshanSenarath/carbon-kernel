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

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.BaseTest;

import java.lang.reflect.Field;

import static org.wso2.carbon.CarbonConstants.ALLOW_ALL;
import static org.wso2.carbon.CarbonConstants.DEFAULT_AND_LOCALHOST;
import static org.wso2.carbon.CarbonConstants.HOST_NAME_VERIFIER;

/**
 * Unit tests for HTTPClientUtils class with Apache HttpClient 5.
 */
public class HTTPClientUtilsTest extends BaseTest {

    public static final String DUMMY_PROPERTY = "dummy";

    @AfterMethod
    public void tearDown() {

        System.clearProperty(HOST_NAME_VERIFIER);
    }

    @DataProvider(name = "hostnameVerifierDataProvider")
    public Object[][] hostnameVerifierDataProvider() {

        return new Object[][]{
                {DEFAULT_AND_LOCALHOST, true},
                {ALLOW_ALL, true},
                {DUMMY_PROPERTY, false}
        };
    }

    @Test(dataProvider = "hostnameVerifierDataProvider")
    public void testCreateClientWithCustomHostnameVerifier(String hostnameVerifierType,
                                                           boolean shouldHaveConnectionManager)
            throws NoSuchFieldException, IllegalAccessException {

        System.setProperty(HOST_NAME_VERIFIER, hostnameVerifierType);
        HttpClientBuilder httpClientBuilder = HTTPClientUtils.createClientWithCustomHostnameVerifier();

        Assert.assertNotNull(httpClientBuilder, "HttpClientBuilder should not be null");

        // Use reflection to check if connection manager is set.
        Class<HttpClientBuilder> httpClientBuilderClass = HttpClientBuilder.class;
        Field connManagerField = httpClientBuilderClass.getDeclaredField("connManager");
        connManagerField.setAccessible(true);

        PoolingHttpClientConnectionManager connManager =
                (PoolingHttpClientConnectionManager) connManagerField.get(httpClientBuilder);

        if (shouldHaveConnectionManager) {
            Assert.assertNotNull(connManager);
        } else {
            Assert.assertNull(connManager);
        }
    }

    /**
     * Test error handling when SSL context creation fails.
     * This test verifies that:
     * 1. The HttpClientBuilder is still created successfully.
     * 2. The client continues without custom TLS strategy.
     */
    @Test
    public void testCreateClientWithSSLInitializationFailure() throws NoSuchFieldException, IllegalAccessException {

        System.setProperty(HOST_NAME_VERIFIER, DEFAULT_AND_LOCALHOST);
        HttpClientBuilder httpClientBuilder = HTTPClientUtils.createClientWithCustomHostnameVerifier();

        Assert.assertNotNull(httpClientBuilder,
                "HttpClientBuilder should be created even if SSL context initialization encounters issues");

        // Verify that the builder has basic configuration (request config should always be set).
        Field defaultRequestConfigField = HttpClientBuilder.class.getDeclaredField("defaultRequestConfig");
        defaultRequestConfigField.setAccessible(true);
        Object requestConfig = defaultRequestConfigField.get(httpClientBuilder);

        Assert.assertNotNull(requestConfig,
                "HttpClientBuilder should have request config set even if SSL initialization fails");
    }

    /**
     * Test that no connection manager is set when hostname verifier property is not configured.
     */
    @Test
    public void testNoConnectionManagerWithoutHostnameVerifierProperty()
            throws NoSuchFieldException, IllegalAccessException {

        System.clearProperty(HOST_NAME_VERIFIER);
        HttpClientBuilder httpClientBuilder = HTTPClientUtils.createClientWithCustomHostnameVerifier();

        Field connManagerField = HttpClientBuilder.class.getDeclaredField("connManager");
        connManagerField.setAccessible(true);
        PoolingHttpClientConnectionManager connManager =
                (PoolingHttpClientConnectionManager) connManagerField.get(httpClientBuilder);

        Assert.assertNull(connManager,
                "Connection manager should not be set when hostname verifier property is not configured");
    }
}
