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

import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.core5.reactor.ssl.SSLBufferMode;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.util.Objects;

/**
 * Custom TLS strategy that disables Java's built-in hostname verification
 * to allow custom hostname verifiers to be the sole verification mechanism.
 * <p>
 * This is critical for ensuring that when a custom hostname verifier is provided,
 * Java's default endpoint identification algorithm does not interfere with the
 * custom verification logic. This strategy uses {@link HostnameVerificationPolicy#CLIENT}
 * which delegates all hostname verification to the provided custom verifier.
 */
public class CustomTlsStrategy extends DefaultClientTlsStrategy {

    public CustomTlsStrategy(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
        super(
            Objects.requireNonNull(sslContext, "SSLContext cannot be null"),
            null,  // SupportedProtocols - null to use system default TLS protocols.
            null,  // SupportedCipherSuites - null to use system default cipher suites from SSLContext.
            SSLBufferMode.STATIC,
            HostnameVerificationPolicy.CLIENT,
            Objects.requireNonNull(hostnameVerifier, "HostnameVerifier cannot be null.")
        );
    }
}
