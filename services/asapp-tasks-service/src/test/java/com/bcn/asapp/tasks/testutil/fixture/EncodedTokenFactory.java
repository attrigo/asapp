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

package com.bcn.asapp.tasks.testutil.fixture;

import static com.bcn.asapp.tasks.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_ACCESS_TOKEN_CLAIMS;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_REFRESH_TOKEN_CLAIMS;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_SUBJECT;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.EXPIRATION_TIME_MILLIS;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.TOKEN_EXPIRED_OFFSET_MILLIS;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.generateRandomIssueAt;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import javax.crypto.SecretKey;

import org.springframework.util.Assert;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

/**
 * Provides test data builders for JWT token strings with fluent API.
 *
 * @since 0.2.0
 */
public final class EncodedTokenFactory {

    private static final String JWT_SECRET;

    static {
        try (InputStream input = EncodedTokenFactory.class.getClassLoader()
                                                          .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("application.properties not found in classpath");
            }
            Properties props = new Properties();
            props.load(input);
            JWT_SECRET = props.getProperty("asapp.security.jwt-secret");
            Assert.hasText(JWT_SECRET, "asapp.security.jwt-secret not found or empty in application.properties");
        } catch (IOException e) {
            throw new IllegalStateException("Could not load JWT secret from properties", e);
        }
    }

    private EncodedTokenFactory() {}

    public static String encodedAccessToken() {
        return anEncodedTokenBuilder().accessToken()
                                      .build();
    }

    public static String encodedRefreshToken() {
        return anEncodedTokenBuilder().refreshToken()
                                      .build();
    }

    public static Builder anEncodedTokenBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String type;

        private String subject;

        private Map<String, Object> claims;

        private Instant issuedAt;

        private Instant expiration;

        private boolean signed = true;

        private byte[] secretBytes;

        Builder() {
            this.subject = DEFAULT_SUBJECT;
            this.claims = Map.of();
            this.issuedAt = generateRandomIssueAt();
            this.expiration = issuedAt.plusMillis(EXPIRATION_TIME_MILLIS);
            this.secretBytes = Base64.getDecoder()
                                     .decode(JWT_SECRET);
        }

        public Builder accessToken() {
            return withType(ACCESS_TOKEN_TYPE).withClaims(DEFAULT_ACCESS_TOKEN_CLAIMS);
        }

        public Builder refreshToken() {
            return withType(REFRESH_TOKEN_TYPE).withClaims(DEFAULT_REFRESH_TOKEN_CLAIMS);
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder withClaims(Map<String, Object> claims) {
            this.claims = claims;
            return this;
        }

        public Builder withIssuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Builder withExpiration(Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        public Builder withSecretKey(SecretKey secretKey) {
            this.secretBytes = secretKey.getEncoded();
            return this;
        }

        public Builder withSecretKey(String key) {
            this.secretBytes = Base64.getDecoder()
                                     .decode(key);
            return this;
        }

        public Builder notSigned() {
            this.signed = false;
            return this;
        }

        public Builder expired() {
            var now = Instant.now();
            var issuedAt = now.minusMillis(EXPIRATION_TIME_MILLIS + TOKEN_EXPIRED_OFFSET_MILLIS);
            var expiration = now.minusMillis(TOKEN_EXPIRED_OFFSET_MILLIS);
            return withIssuedAt(issuedAt).withExpiration(expiration);
        }

        public String build() {
            try {
                var claimsSetBuilder = new JWTClaimsSet.Builder().subject(subject)
                                                                 .issueTime(Date.from(issuedAt));
                if (expiration != null) {
                    claimsSetBuilder.expirationTime(Date.from(expiration));
                }
                claims.forEach(claimsSetBuilder::claim);
                var claimsSet = claimsSetBuilder.build();

                if (!signed) {
                    var header = new PlainHeader.Builder().type(new JOSEObjectType(type))
                                                          .build();
                    return new PlainJWT(header, claimsSet).serialize();
                }

                var algorithm = selectAlgorithm(secretBytes);
                var header = new JWSHeader.Builder(algorithm).type(new JOSEObjectType(type))
                                                             .build();
                var signedJwt = new SignedJWT(header, claimsSet);
                signedJwt.sign(new MACSigner(secretBytes));
                return signedJwt.serialize();
            } catch (JOSEException e) {
                throw new RuntimeException("Failed to build encoded token", e);
            }
        }

        private static JWSAlgorithm selectAlgorithm(byte[] keyBytes) {
            int bitLength = keyBytes.length * 8;
            if (bitLength >= 512) {
                return JWSAlgorithm.HS512;
            }
            if (bitLength >= 384) {
                return JWSAlgorithm.HS384;
            }
            return JWSAlgorithm.HS256;
        }

    }

}
