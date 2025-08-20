/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.tomcat.ext.filter;

import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.*;

public class PathTraversalFilterTest {

    @Mock
    private HttpServletRequest mockRequest;

    private PathTraversalFilter filter;

    @BeforeClass
    public void setUp() {

        initMocks(this);
        filter = new PathTraversalFilter();
    }

    @Test
    public void testDoFilter_ValidURI_ShouldPassThrough() throws IOException, ServletException {

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockFilterChain = mock(FilterChain.class);
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/api/users/123");

        // Act
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockFilterChain, atLeastOnce()).doFilter(mockRequest, mockResponse);
        verify(mockResponse, never()).setStatus(anyInt());
    }

    @DataProvider(name = "maliciousURIs")
    public Object[][] maliciousURIs() {

        return new Object[][]{
                // Basic path traversal
                {"/api/../../../etc/passwd"}, {"/api/..\\..\\windows\\system32"}, {"/files/../../../secret.txt"},
                {"/upload/../../admin/config"},

                // URL encoded attacks
                {"/api/%2e%2e/secret"}, {"/api/%2e%2e%2f"}, {"/api/%2e%2e%5c"}, {"/files/%2e%2e/%2e%2e/admin"},

                // Double URL encoded
                {"/api/%252e%252e%252f"}, {"/api/%252e%252e%255c"}, {"/files/%252e%252e%252fadmin"},

                // Mixed case attacks
                {"/api/%2E%2E%2F"}, {"/api/%2e%2E/"}, {"/files/%2E%2e%5C"},

                // Multiple consecutive attacks
                {"/api/../../../../../../../etc/passwd"}, {"/../../../etc/hosts"},
                {"/files/../../../../windows/system32"}};
    }

    @Test(dataProvider = "maliciousURIs")
    public void testBlockMaliciousURI(String maliciousURI) throws IOException, ServletException {

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockFilterChain = mock(FilterChain.class);
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn(maliciousURI);

        // Act
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        try {
            verify(mockFilterChain, never()).doFilter(any(), any());
            verify(mockResponse).sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (AssertionError e) {
            fail("Expected filter to block malicious URI: " + maliciousURI, e);
        }
    }

    @DataProvider(name = "legitimateURIs")
    public Object[][] legitimateURIs() {

        return new Object[][]{
                // Legitimate URIs that should pass through
                {"/api/v1/users"}, {"/api/users"}, {"/api/users/123"}, {"/static/css/style.css"}, {"/images/logo.png"},
                {"/api/v2/endpoint"}, {"/files/document.pdf"}, {"/upload/image.jpg"}, {"/admin/dashboard"},
                {"/reports/monthly"},

                // Legitimate dots in filenames/paths
                {"/api/file.txt"}, {"/api/version2.0"}, {"/files/document.v2.pdf"}, {"/static/jquery-3.6.0.min.js"},
                {"/assets/font.woff2"}, {"/api/user.profile"},

                // Complex but legitimate paths
                {"/api/v1/users/profile/settings"}, {"/files/2023/reports/quarterly.xlsx"},
                {"/static/components/header/nav.component.js"}};
    }

    @Test(dataProvider = "legitimateURIs")
    public void testAllowLegitimateURI(String legitimateURI) throws IOException, ServletException {

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockFilterChain = mock(FilterChain.class);
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn(legitimateURI);

        // Act
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        try {
            verify(mockFilterChain, atLeastOnce()).doFilter(mockRequest, mockResponse);
            verify(mockResponse, never()).sendError(anyInt());
        } catch (AssertionError e) {
            fail("Expected filter to allow legitimate URI: " + legitimateURI, e);
        }
    }

    @Test
    public void testAllowEmptyURI() throws IOException, ServletException {

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockFilterChain = mock(FilterChain.class);
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("");

        // Act
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockFilterChain, atLeastOnce()).doFilter(mockRequest, mockResponse);
    }

    @Test
    public void testAllowNullURI() throws IOException, ServletException {

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockFilterChain = mock(FilterChain.class);
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn(null);

        // Act
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockFilterChain, atLeastOnce()).doFilter(mockRequest, mockResponse);
    }

    @Test
    public void testAllowRootPath() throws IOException, ServletException {

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockFilterChain = mock(FilterChain.class);
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn("/");

        // Act
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockFilterChain, atLeastOnce()).doFilter(mockRequest, mockResponse);
    }

    @DataProvider(name = "borderlineCases")
    public Object[][] borderlineCases() {

        return new Object[][]{
                // Borderline cases that could be legitimate or malicious
                {"/api/./current", true}, {"/files/./today", true}, {"/api/../parent", false},
                {"/files/..\\parent", false}, {"/api/user..name", true}, {"/files/version..2", true},
                {"/api/user%2e%2e%2fname", false}};
    }

    @Test(dataProvider = "borderlineCases")
    public void testHandleBorderlineCases(String uri, boolean shouldAllow) throws IOException, ServletException {

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockFilterChain = mock(FilterChain.class);
        // Arrange
        when(mockRequest.getRequestURI()).thenReturn(uri);

        // Act
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        if (shouldAllow) {
            try {
                verify(mockFilterChain, atLeastOnce()).doFilter(mockRequest, mockResponse);
                verify(mockResponse, never()).sendError(anyInt());
            } catch (AssertionError e) {
                fail("Expected filter to allow legitimate URI: " + uri, e);
            }
        } else {
            try {
                verify(mockFilterChain, never()).doFilter(any(), any());
                verify(mockResponse).sendError(HttpServletResponse.SC_BAD_REQUEST);
            } catch (AssertionError e) {
                fail("Expected filter to block malicious URI: " + uri, e);
            }
        }
    }
}
