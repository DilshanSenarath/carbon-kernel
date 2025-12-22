/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.user.core.ldap;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.utils.CarbonUtils;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * Unit tests for LDAPConnectionContext class.
 */
public class LDAPConnectionContextTest {

    private String invokeResolvePort(String url) throws Exception {

        Method method = LDAPConnectionContext.class.getDeclaredMethod("resolvePort", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, url);
    }

    @DataProvider(name = "urlDataProvider")
    public Object[][] urlDataProvider() {

        return new Object[][]{
                {"ldap://localhost:389", "ldap://localhost:389"},
                {"ldap://localhost", "ldap://localhost:389"},
                {"ldaps://localhost", "ldaps://localhost:636"},
                {"ldap://127.0.0.1:389", "ldap://127.0.0.1:389"},
                {"ldap://127.0.0.1", "ldap://127.0.0.1:389"},
                {"ldaps://127.0.0.1", "ldaps://127.0.0.1:636"},
                {"ldap://[2001:db8::1]:389", "ldap://[2001:db8::1]:389"},
                {"ldap://[2001:db8::1]", "ldap://[2001:db8::1]:389"},
                {"ldap://localhost/dc=wso2,dc=org", "ldap://localhost:389/dc=wso2,dc=org"},
                {"  ldap://localhost  ", "ldap://localhost:389"}
        };
    }

    @Test(dataProvider = "urlDataProvider")
    public void testResolvePort(String url, String expectedUrl) throws Exception {

        String resolvedUrl = invokeResolvePort(url);
        Assert.assertEquals(resolvedUrl, expectedUrl, "URL resolution failed for: " + url);
    }

    @Test
    public void testResolvePortWithTemplate() throws Exception {

        String template = "Ports.EmbeddedLDAP.LDAPServerPort";
        int portValue = 10389;

        try (MockedStatic<CarbonUtils> carbonUtilsMock = Mockito.mockStatic(CarbonUtils.class)) {
            carbonUtilsMock.when(() -> CarbonUtils.getPortFromServerConfig(anyString())).thenReturn(portValue);

            String url = "ldap://localhost:${" + template + "}";
            String expectedUrl = "ldap://localhost:" + portValue;

            String resolvedUrl = invokeResolvePort(url);
            Assert.assertEquals(resolvedUrl, expectedUrl, "Template resolution failed");
        }
    }
}
