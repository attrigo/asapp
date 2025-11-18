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

package com.bcn.asapp.authentication.testutil;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.EXPIRATION_TIME;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.testEncodedTokenBuilder;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.SecretKey;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtClaimsEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtEntity;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;

public class TestFactory {

    public static final class TestUserFactory {

        static final String TEST_USER_USERNAME = "user@asapp.com";

        static final String TEST_USER_RAW_PASSWORD = "TEST@09_password?!";

        static final Role TEST_USER_ROLE = USER;

        TestUserFactory() {}

        public static JdbcUserEntity defaultTestUser() {
            return new Builder().build();
        }

        public static Builder testUserBuilder() {
            return new Builder();
        }

        public static class Builder {

            private String username;

            private String password;

            private String role;

            private String passwordEncoderPrefix;

            private PasswordEncoder passwordEncoder;

            Builder() {
                this.username = TEST_USER_USERNAME;
                this.password = TEST_USER_RAW_PASSWORD;
                this.role = TEST_USER_ROLE.name();
                this.passwordEncoderPrefix = "{bcrypt}";
                this.passwordEncoder = new BCryptPasswordEncoder();
            }

            public Builder withUsername(String username) {
                this.username = username;
                return this;
            }

            public Builder withPassword(String password) {
                this.password = password;
                return this;
            }

            public Builder withRole(String role) {
                this.role = role;
                return this;
            }

            public Builder withPasswordEncoder(String passwordEncoderPrefix, PasswordEncoder passwordEncoder) {
                this.passwordEncoderPrefix = passwordEncoderPrefix;
                this.passwordEncoder = passwordEncoder;
                return this;
            }

            public JdbcUserEntity build() {
                var passwordEncoded = passwordEncoderPrefix + passwordEncoder.encode(password);
                return new JdbcUserEntity(null, username, passwordEncoded, role);
            }

        }

    }

    public static final class TestJwtAuthenticationFactory {

        static final String TEST_JWT_AUTH_SUBJECT = "user@asapp.com";

        static final String TEST_JWT_AUTH_ROLE = USER.name();

        TestJwtAuthenticationFactory() {}

        public static JdbcJwtAuthenticationEntity defaultTestJwtAuthentication() {
            return new Builder().build();
        }

        public static Builder testJwtAuthenticationBuilder() {
            return new Builder();
        }

        public static class Builder {

            private UUID userId;

            private JdbcJwtEntity accessToken;

            private JdbcJwtEntity refreshToken;

            Builder() {
                this.userId = UUID.randomUUID();
                this.accessToken = createJwtEntity(ACCESS_TOKEN_TYPE);
                this.refreshToken = createJwtEntity(REFRESH_TOKEN_TYPE);
            }

            public Builder withUserId(UUID userId) {
                this.userId = userId;
                return this;
            }

            public Builder withAccessToken(JdbcJwtEntity accessToken) {
                this.accessToken = accessToken;
                return this;
            }

            public Builder withRefreshToken(JdbcJwtEntity refreshToken) {
                this.refreshToken = refreshToken;
                return this;
            }

            public JdbcJwtAuthenticationEntity build() {
                return new JdbcJwtAuthenticationEntity(null, userId, accessToken, refreshToken);
            }

            private static JdbcJwtEntity createJwtEntity(String type) {
                var subject = TEST_JWT_AUTH_SUBJECT;
                var issuedAt = generateRandomIssueAt();
                var expiration = issuedAt.plus(EXPIRATION_TIME, SECONDS);
                var tokenUseClaim = ACCESS_TOKEN_TYPE.equals(type) ? ACCESS_TOKEN_USE : REFRESH_TOKEN_USE;
                Map<String, Object> claimsMap = Map.of(TOKEN_USE, tokenUseClaim, ROLE, TEST_JWT_AUTH_ROLE);

                var token = testEncodedTokenBuilder().withType(type)
                                                     .withSubject(subject)
                                                     .withClaims(claimsMap)
                                                     .withIssuedAt(issuedAt)
                                                     .withExpiration(expiration)
                                                     .build();

                var claims = new JdbcJwtClaimsEntity(claimsMap);
                return new JdbcJwtEntity(token, type, subject, claims, issuedAt, expiration);
            }

        }

    }

    public static final class TestEncodedTokenFactory {

        static final String JWT_SECRET;

        static final Long EXPIRATION_TIME = 300000L;

        static final String TEST_ENCODED_TOKEN_SUBJECT = "user@asapp.com";

        static final String TEST_ENCODED_TOKEN_ROLE_CLAIM = USER.name();

        static final Map<String, Object> TEST_ENCODED_TOKEN_AT_CLAIMS = Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, TEST_ENCODED_TOKEN_ROLE_CLAIM);

        static final Map<String, Object> TEST_ENCODED_TOKEN_RT_CLAIMS = Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, TEST_ENCODED_TOKEN_ROLE_CLAIM);

        static {
            try (InputStream input = TestEncodedTokenFactory.class.getClassLoader()
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

        TestEncodedTokenFactory() {}

        public static String defaultTestEncodedAccessToken() {
            return testEncodedTokenBuilder().accessToken()
                                            .build();
        }

        public static String defaultTestEncodedRefreshToken() {
            return testEncodedTokenBuilder().refreshToken()
                                            .build();
        }

        public static Builder testEncodedTokenBuilder() {
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
                subject = TEST_ENCODED_TOKEN_SUBJECT;
                issuedAt = generateRandomIssueAt();
                expiration = issuedAt.plusMillis(EXPIRATION_TIME);
                secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
            }

            public Builder accessToken() {
                return withType(ACCESS_TOKEN_TYPE).withClaims(TEST_ENCODED_TOKEN_AT_CLAIMS);
            }

            public Builder refreshToken() {
                return withType(REFRESH_TOKEN_TYPE).withClaims(TEST_ENCODED_TOKEN_RT_CLAIMS);
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
                this.claims = Map.copyOf(claims);
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
                return withIssuedAt(now.minusMillis(EXPIRATION_TIME + 60000)).withExpiration(now.minusMillis(60000));
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

    /**
     * Generate a random issueAt value between now and 4.5 minutes before now.
     *
     * @return the random issueAt value.
     */
    private static Instant generateRandomIssueAt() {
        Instant now = Instant.now();
        Instant fourMinsAgo = Instant.now()
                                     .minusMillis(EXPIRATION_TIME - 30000);
        long startMillis = fourMinsAgo.toEpochMilli();
        long endMillis = now.toEpochMilli();
        long randomMillis = ThreadLocalRandom.current()
                                             .nextLong(startMillis, endMillis + 1);
        return Instant.ofEpochMilli(randomMillis);
    }

}
