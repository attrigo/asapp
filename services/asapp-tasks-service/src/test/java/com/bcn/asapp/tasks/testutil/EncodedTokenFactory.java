/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.tasks.testutil;

import static com.bcn.asapp.tasks.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.tasks.testutil.TestFactoryConstants.DEFAULT_ACCESS_TOKEN_CLAIMS;
import static com.bcn.asapp.tasks.testutil.TestFactoryConstants.DEFAULT_REFRESH_TOKEN_CLAIMS;
import static com.bcn.asapp.tasks.testutil.TestFactoryConstants.DEFAULT_SUBJECT;
import static com.bcn.asapp.tasks.testutil.TestFactoryConstants.EXPIRATION_TIME_MILLIS;
import static com.bcn.asapp.tasks.testutil.TestFactoryConstants.TOKEN_EXPIRED_OFFSET_MILLIS;
import static com.bcn.asapp.tasks.testutil.TestFactoryConstants.generateRandomIssueAt;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import javax.crypto.SecretKey;

import org.springframework.util.Assert;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

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

        private SecretKey secretKey;

        Builder() {
            subject = DEFAULT_SUBJECT;
            issuedAt = generateRandomIssueAt();
            expiration = issuedAt.plusMillis(EXPIRATION_TIME_MILLIS);
            secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
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
            this.secretKey = secretKey;
            return this;
        }

        public Builder withSecretKey(String key) {
            this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));
            return this;
        }

        public Builder expired() {
            Instant now = Instant.now();
            return withIssuedAt(now.minusMillis(EXPIRATION_TIME_MILLIS + TOKEN_EXPIRED_OFFSET_MILLIS)).withExpiration(
                    now.minusMillis(TOKEN_EXPIRED_OFFSET_MILLIS));
        }

        public Builder notSigned() {
            this.signed = false;
            return this;
        }

        public String build() {
            var jwts = Jwts.builder()
                           .header()
                           .type(type)
                           .and()
                           .subject(subject)
                           .claims(claims)
                           .issuedAt(Date.from(issuedAt))
                           .expiration(Date.from(expiration));
            if (signed) {
                jwts.signWith(secretKey);
            }
            return jwts.compact();
        }

    }

}
