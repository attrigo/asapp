/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.authentication.infrastructure.config;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;

/**
 * Configures JWT cryptographic components as Spring-managed beans.
 * <p>
 * Decodes the configured secret key once at startup and exposes a {@link MACSigner} for token signing and a {@link MACVerifier} for token verification.
 * <p>
 * Key-length validation is performed eagerly at application startup, failing fast with a descriptive message if the secret is too short for any supported HMAC
 * algorithm.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Configuration(proxyBeanMethods = false)
public class JwtCryptoConfiguration {

    /**
     * Creates a {@link MACSigner} bean from the configured JWT secret.
     *
     * @param jwtSecret the base64-encoded JWT secret from configuration
     * @return a configured {@link MACSigner}
     * @throws IllegalStateException if the decoded key is too short for any supported HMAC algorithm
     */
    @Bean
    MACSigner macSigner(@Value("${asapp.security.jwt-secret}") String jwtSecret) {
        var secretBytes = Base64.getDecoder()
                                .decode(jwtSecret);
        try {
            return new MACSigner(secretBytes);
        } catch (KeyLengthException e) {
            throw new IllegalStateException("Invalid JWT secret key: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a {@link MACVerifier} bean from the configured JWT secret.
     *
     * @param jwtSecret the base64-encoded JWT secret from configuration
     * @return a configured {@link MACVerifier}
     * @throws IllegalStateException if the decoded key is too short for any supported HMAC algorithm
     */
    @Bean
    MACVerifier macVerifier(@Value("${asapp.security.jwt-secret}") String jwtSecret) {
        var secretBytes = Base64.getDecoder()
                                .decode(jwtSecret);
        try {
            return new MACVerifier(secretBytes);
        } catch (JOSEException e) {
            throw new IllegalStateException("Invalid JWT secret key: " + e.getMessage(), e);
        }
    }

}
