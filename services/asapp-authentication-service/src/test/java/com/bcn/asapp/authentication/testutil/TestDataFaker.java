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

import static com.bcn.asapp.authentication.domain.authentication.Jwt.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.EncodedJwtDataFaker.fakeEncodedJwtBuilder;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.JwtDataFaker.defaultFakeAccessToken;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.JwtDataFaker.defaultFakeRefreshToken;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_ROLE;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_USERNAME;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.infrastructure.authentication.out.entity.JwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.out.entity.JwtClaimsEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.out.entity.JwtEntity;
import com.bcn.asapp.authentication.infrastructure.user.out.entity.UserEntity;

public class TestDataFaker {

    public static class UserDataFaker {

        public static final String DEFAULT_FAKE_USERNAME = "test.username@asapp.com";

        public static final String DEFAULT_FAKE_RAW_PASSWORD = "TEST@09_password?!";

        public static final Role DEFAULT_FAKE_ROLE = USER;

        UserDataFaker() {}

        public static UserEntity defaultFakeUser() {
            return new Builder().build();
        }

        public static Builder fakeUserBuilder() {
            return new Builder();
        }

        public static class Builder {

            private String username;

            private String password;

            private String role;

            private String passwordEncoderPrefix;

            private PasswordEncoder passwordEncoder;

            Builder() {
                this.username = DEFAULT_FAKE_USERNAME;
                this.password = DEFAULT_FAKE_RAW_PASSWORD;
                this.role = DEFAULT_FAKE_ROLE.name();
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

            public UserEntity build() {
                var passwordEncoded = passwordEncoderPrefix + passwordEncoder.encode(password);
                return new UserEntity(null, username, passwordEncoded, role);
            }

        }

    }

    public static class JwtAuthenticationDataFaker {

        JwtAuthenticationDataFaker() {}

        public static JwtAuthenticationEntity defaultFakeJwtAuthentication() {
            return new Builder().build();
        }

        public static Builder fakeJwtAuthenticationBuilder() {
            return new Builder();
        }

        public static class Builder {

            private UUID userId;

            private JwtEntity accessToken;

            private JwtEntity refreshToken;

            Builder() {
                this.userId = UUID.randomUUID();
                this.accessToken = defaultFakeAccessToken();
                this.refreshToken = defaultFakeRefreshToken();
            }

            public Builder withUserId(UUID userId) {
                this.userId = userId;
                return this;
            }

            public Builder withAccessToken(JwtEntity accessToken) {
                this.accessToken = accessToken;
                return this;
            }

            public Builder withRefreshToken(JwtEntity refreshToken) {
                this.refreshToken = refreshToken;
                return this;
            }

            public JwtAuthenticationEntity build() {
                return new JwtAuthenticationEntity(null, userId, accessToken, refreshToken);
            }

        }

    }

    public static class JwtDataFaker {

        JwtDataFaker() {}

        public static JwtEntity defaultFakeAccessToken() {
            return new Builder().accessToken()
                                .build();
        }

        public static JwtEntity defaultFakeRefreshToken() {
            return new Builder().refreshToken()
                                .build();
        }

        public static Builder fakeJwtBuilder() {
            return new Builder();
        }

        public static class Builder {

            Builder() {}

            public JwtContentBuilder accessToken() {
                return new JwtContentBuilder(ACCESS_TOKEN.type());
            }

            public JwtContentBuilder refreshToken() {
                return new JwtContentBuilder(REFRESH_TOKEN.type());
            }

            public static class JwtContentBuilder {

                private static final Long EXPIRATION_TIME = 300000L;

                private final String type;

                private String subject;

                private Instant issuedAt;

                private Instant expiration;

                JwtContentBuilder(String type) {
                    this.type = type;
                    this.subject = DEFAULT_FAKE_USERNAME;
                    this.issuedAt = generateRandomIssueAt();
                    this.expiration = issuedAt.plus(EXPIRATION_TIME, SECONDS);
                }

                public JwtContentBuilder withSubject(String subject) {
                    this.subject = subject;
                    return this;
                }

                public JwtContentBuilder withIssuedAt(Instant issuedAt) {
                    this.issuedAt = issuedAt;
                    return this;
                }

                public JwtContentBuilder withExpiration(Instant expiration) {
                    this.expiration = expiration;
                    return this;
                }

                public JwtContentBuilder expired() {
                    return withExpiration(Instant.now());
                }

                public JwtEntity build() {
                    var token = fakeEncodedJwtBuilder().ofType(type)
                                                       .withSubject(subject)
                                                       .withIssuedAt(issuedAt)
                                                       .withExpiration(expiration)
                                                       .build();

                    var tokenUseClaim = ACCESS_TOKEN.type()
                                                    .equals(type) ? ACCESS_TOKEN_USE_CLAIM_VALUE : REFRESH_TOKEN_USE_CLAIM_VALUE;
                    Map<String, Object> claimsMap = Map.of(ROLE_CLAIM_NAME, DEFAULT_FAKE_ROLE.name(), TOKEN_USE_CLAIM_NAME, tokenUseClaim);
                    var claims = new JwtClaimsEntity(claimsMap);
                    return new JwtEntity(token, type, subject, claims, issuedAt, expiration);
                }

                /**
                 * Generate a random issueAt value between now and 4.5 minutes before now.
                 *
                 * @return the random issueAt value.
                 */
                private Instant generateRandomIssueAt() {
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

        }

    }

    public static class EncodedJwtDataFaker {

        EncodedJwtDataFaker() {}

        public static String defaultFakeEncodedAccessToken() {
            return new Builder().accessToken()
                                .build();
        }

        public static String defaultFakeEncodedRefreshToken() {
            return new Builder().refreshToken()
                                .build();
        }

        public static Builder fakeEncodedJwtBuilder() {
            return new Builder();
        }

        public static class Builder {

            private static String JWT_SECRET;

            private static final Long EXPIRATION_TIME = 300000L;

            static {
                loadJwtSecretProperty();
            }

            private static void loadJwtSecretProperty() {
                if (JWT_SECRET == null) {
                    try (InputStream input = TestDataFaker.class.getClassLoader()
                                                                .getResourceAsStream("application.properties")) {
                        Properties props = new Properties();
                        props.load(input);
                        JWT_SECRET = props.getProperty("asapp.security.jwt-secret");
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not load JWT secret from properties", e);
                    }
                }
            }

            Builder() {}

            public EncodedJwtContentBuilder ofType(String type) {
                return new EncodedJwtContentBuilder(type);
            }

            public EncodedJwtContentBuilder accessToken() {
                return new EncodedJwtContentBuilder(ACCESS_TOKEN.type());
            }

            public EncodedJwtContentBuilder refreshToken() {
                return new EncodedJwtContentBuilder(REFRESH_TOKEN.type());
            }

            public static class EncodedJwtContentBuilder {

                private final String type;

                private String subject;

                private Instant issuedAt;

                private Instant expiration;

                private boolean signed = true;

                private String signature;

                EncodedJwtContentBuilder(String type) {
                    this.type = type;
                    this.subject = DEFAULT_FAKE_USERNAME;
                    this.issuedAt = generateRandomIssueAt();
                    this.expiration = issuedAt.plusMillis(EXPIRATION_TIME);
                    this.signature = JWT_SECRET;
                }

                public EncodedJwtContentBuilder withSubject(String subject) {
                    this.subject = subject;
                    return this;
                }

                public EncodedJwtContentBuilder withIssuedAt(Instant issuedAt) {
                    this.issuedAt = issuedAt;
                    return this;
                }

                public EncodedJwtContentBuilder withExpiration(Instant expiration) {
                    this.expiration = expiration;
                    return this;
                }

                public EncodedJwtContentBuilder expired() {
                    return withExpiration(Instant.now());
                }

                public EncodedJwtContentBuilder withSignature(String signature) {
                    this.signature = signature;
                    return this;
                }

                public EncodedJwtContentBuilder notSigned() {
                    this.signed = false;
                    return this;
                }

                public String build() {
                    var tokenUseClaim = ACCESS_TOKEN.type()
                                                    .equals(type) ? ACCESS_TOKEN_USE_CLAIM_VALUE : REFRESH_TOKEN_USE_CLAIM_VALUE;
                    Map<String, Object> claimsMap = Map.of(ROLE_CLAIM_NAME, USER.name(), TOKEN_USE_CLAIM_NAME, tokenUseClaim);
                    var jwts = Jwts.builder()
                                   .header()
                                   .type(type)
                                   .and()
                                   .subject(subject)
                                   .claims(claimsMap)
                                   .issuedAt(Date.from(issuedAt))
                                   .expiration(Date.from(expiration));
                    if (signed) {
                        jwts.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(signature)));
                    }
                    return jwts.compact();
                }

                /**
                 * Generate a random issueAt value between now and 4.5 minutes before now.
                 *
                 * @return the random issueAt value.
                 */
                private Instant generateRandomIssueAt() {
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

        }

    }

}
