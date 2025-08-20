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

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This filter checks for path traversal attempts in the request URI and blocks them.
 * It matches patterns that indicate attempts to traverse directories using sequences like "../"
 * or URL encoded equivalents.
 */
public class PathTraversalFilter implements Filter {

    // Patterns for ../, ..\, and their URL-encoded or double-encoded equivalents
    private static final String PATH_TRAVERSAL_REGEX =
            "(?i)(\\.\\.[/\\\\]|%2e%2e/|%2e%2e%2f|%2e%2e\\\\|%2e%2e%5c|%252e%252e%252f|%252e%252e%255c)";
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(PATH_TRAVERSAL_REGEX);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String requestURI = ((HttpServletRequest) request).getRequestURI();
        if (isPathTraversalAttempt(requestURI)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPathTraversalAttempt(String requestURI) {

        if (StringUtils.isBlank(requestURI)) {
            return false;
        }
        return PATH_TRAVERSAL_PATTERN.matcher(requestURI).find();
    }
}
