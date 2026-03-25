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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.tomcat.ext.valves;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import java.io.IOException;
import javax.servlet.ServletException;

/**
 * Valve that normalizes request URIs by redirecting GET requests to remove trailing slashes.
 * Only redirects paths longer than "/" that end with trailing slashes. Query strings are preserved.
 * Non-GET requests are passed through unchanged to avoid dropping the request body.
 */
public class RequestNormalizationValve extends ValveBase {

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

		String decodedUri = request.getDecodedRequestURI();

		if ("GET".equalsIgnoreCase(request.getMethod()) && decodedUri != null && decodedUri.length() > 1
				&& decodedUri.endsWith("/")) {
			String rawUri = request.getRequestURI().replaceAll("/+$", "");
			String queryString = request.getQueryString();
			String location = queryString != null ? rawUri + "?" + queryString : rawUri;
			response.sendRedirect(location);
			return;
		}

		// Invoking other valves.
		if (getNext() != null) {
			getNext().invoke(request, response);
		}
	}
}
