/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com/).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.ui.filters;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This filter handle the admin service GET request filtering.
 */
public class URIMethodBlockFilter implements Filter {

    private static Log log = LogFactory.getLog(URIMethodBlockFilter.class);

    private static final Pattern TENANT_PREFIX_PATTERN = Pattern.compile("^/{1,}t/{1,}[^/]+");
    private static final Pattern MULTI_SLASH = Pattern.compile("^/{2,}");
    private static final String ADMIN_SERVICE_PATH_PREFIX = "/services/";
    private static final String HTTP_METHOD_GET = "GET";
    private static final String WSDL_QUERY_PARAM = "wsdl";
    private static final String WSDL2_QUERY_PARAM = "wsdl2";

    private static final String ALLOW_ALL_ADMIN_SERVICE_GET_REQUESTS =
            "Security.AdminServiceGetRequestFilter.AllowAllAdminServices";
    private static final String ALLOWED_ADMIN_SERVICE_GET_URIS =
            "Security.AdminServiceGetRequestFilter.AllowedGetURIs.URI";

    private boolean allowAllAdminServiceGetRequests;
    private Set<String> allowedAdminServiceGetURIs = new HashSet<>();
    private boolean allowedGetURIsAvailable;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        // Initialize configuration to allow GET requests for all admin service endpoints.
        ServerConfiguration serverConfiguration = ServerConfiguration.getInstance();
        allowAllAdminServiceGetRequests = Boolean.parseBoolean(
                serverConfiguration.getFirstProperty(ALLOW_ALL_ADMIN_SERVICE_GET_REQUESTS));

        if (allowAllAdminServiceGetRequests) {
            if (log.isDebugEnabled()) {
                log.debug("All admin service GET requests are allowed as per the server configuration.");
            }
            // No need to initialize allowed URIs since all GET requests are allowed.
            return;
        }

        // Initialize allowed URIs when handling GET requests when 'AllowAllAdminServices' is false.
        String[] allowedListConfig = serverConfiguration.getProperties(ALLOWED_ADMIN_SERVICE_GET_URIS);
        if (allowedListConfig != null && allowedListConfig.length != 0) {
            Set<String> configured = Arrays.stream(allowedListConfig)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            // Expand configured base URIs to include SOAP11/12 endpoint variants, normalize entries, and dedupe.
            allowedAdminServiceGetURIs = expandSoapEndpointVariants(configured);
            if (log.isDebugEnabled()) {
                log.debug("Allowed admin service GET URIs (expanded): " + allowedAdminServiceGetURIs);
            }
        }
        if (!allowedAdminServiceGetURIs.isEmpty()) {
            allowedGetURIsAvailable = true;
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        if (!(servletRequest instanceof HttpServletRequest)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // If all admin service GET requests are allowed, allow the request.
        if (allowAllAdminServiceGetRequests) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest httpReq = (HttpServletRequest) servletRequest;
        String method = httpReq.getMethod().toUpperCase();
        // If the HTTP method is not GET, allow the request.
        if (!HTTP_METHOD_GET.equals(method)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String requestURI = httpReq.getRequestURI();
        String normalizedURI = getNormalizedURI(requestURI);

        // Validate the service url prefix since tenanted paths cannot be handled from filter mappings in web.xml.
        if (!normalizedURI.startsWith(ADMIN_SERVICE_PATH_PREFIX)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Allow the GET requests for WSDL retrieval.
        Map<String, String[]> parameterMap = httpReq.getParameterMap();
        if (parameterMap.size() == 1 &&
                (parameterMap.containsKey(WSDL_QUERY_PARAM) || parameterMap.containsKey(WSDL2_QUERY_PARAM))) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // If the normalized URI is allowed for GET requests, allow the request.
        if (isAllowedURI(normalizedURI)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletResponse httpResp = (HttpServletResponse) servletResponse;
        log.warn("Blocked GET request to admin service endpoint: " + requestURI);
        httpResp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET method is not allowed for " +
                "this operation");
    }

    /**
     * Removes the tenant prefix (e.g., /t/mytenant.com) from a URI string.
     *
     * @param requestURI The full request URI.
     * @return A normalized URI for checking against the allowed list.
     */
    private String getNormalizedURI(String requestURI) {

        String noTenantPath = TENANT_PREFIX_PATTERN.matcher(requestURI).replaceFirst(StringUtils.EMPTY);
        return MULTI_SLASH.matcher(noTenantPath).replaceFirst("/");
    }

    /**
     * Expand configured base admin-service URIs into SOAP 1.1/1.2 endpoint variants.
     * If the configured URI already targets a SOAP endpoint variant (contains a dot
     * in the service segment), it's kept as-is. Tenanted prefixes are removed.
     *
     * Examples (input -> outputs included in the set):
     *  - /services/Foo/op ->
     *      /services/Foo/op,
     *      /services/Foo.FooHttpsSoap11Endpoint/op,
     *      /services/Foo.FooHttpSoap11Endpoint/op,
     *      /services/Foo.FooHttpsSoap12Endpoint/op
     *      /services/Foo.FooHttpSoap12Endpoint/op
     *      /services/Foo.FooHttpsEndpoint/op
     *      /services/Foo.FooHttpEndpoint/op
     *  - /services/Foo.FooHttpsSoap11Endpoint/op -> kept as it is
     */
    private Set<String> expandSoapEndpointVariants(Set<String> configuredUris) {

        Set<String> expanded = new HashSet<>();
        for (String raw : configuredUris) {
            String normalized = getNormalizedURI(raw);
            if (!normalized.startsWith(ADMIN_SERVICE_PATH_PREFIX)) {
                expanded.add(normalized);
                continue;
            }
            String remainder = normalized.substring(ADMIN_SERVICE_PATH_PREFIX.length());
            int slashIdx = remainder.indexOf('/');
            if (slashIdx <= 0) {
                // No operation segment; keep as it is.
                expanded.add(normalized);
                continue;
            }
            String service = remainder.substring(0, slashIdx);
            String opPath = remainder.substring(slashIdx); // includes '/'.
            if (service.contains(".")) {
                // Already an endpoint-specific variant.
                expanded.add(normalized);
                continue;
            }
            // Base service: add itself and endpoint variants.
            expanded.add(normalized);
            String soap11 = ADMIN_SERVICE_PATH_PREFIX + service + "." + service + "HttpsSoap11Endpoint" + opPath;
            String soap11Http = ADMIN_SERVICE_PATH_PREFIX + service + "." + service + "HttpSoap11Endpoint" + opPath;
            String soap12 = ADMIN_SERVICE_PATH_PREFIX + service + "." + service + "HttpsSoap12Endpoint" + opPath;
            String soap12Http = ADMIN_SERVICE_PATH_PREFIX + service + "." + service + "HttpSoap12Endpoint" + opPath;
            String http = ADMIN_SERVICE_PATH_PREFIX + service + "." + service + "HttpEndpoint" + opPath;
            String https = ADMIN_SERVICE_PATH_PREFIX + service + "." + service + "HttpsEndpoint" + opPath;
            expanded.add(soap11);
            expanded.add(soap11Http);
            expanded.add(soap12);
            expanded.add(soap12Http);
            expanded.add(http);
            expanded.add(https);
        }
        return expanded;
    }

    /**
     * Checks if the normalizedURI matches any of the allowed URIs.
     *
     * @param normalizedURI The tenant-normalized URI from the request.
     * @return true if the URI is on the allowed list, false otherwise.
     */
    private boolean isAllowedURI(String normalizedURI) {

        if (!allowedGetURIsAvailable) {
            return false;
        }
        return allowedAdminServiceGetURIs.contains(normalizedURI);
    }

    @Override
    public void destroy() {

    }
}
