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

package com.bcn.asapp.users.testutil;

import static com.bcn.asapp.users.infrastructure.security.DecodedToken.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.ROLE_CLAIM_NAME;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.TOKEN_USE_CLAIM_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.users.infrastructure.user.out.entity.UserEntity;

public class TestDataFaker {

    public static class UserDataFaker {

        public static final String DEFAULT_FAKE_FIRST_NAME = "John";

        public static final String DEFAULT_FAKE_LAST_NAME = "Doe";

        public static final String DEFAULT_FAKE_EMAIL = "test.user@asapp.com";

        public static final String DEFAULT_FAKE_PHONE_NUMBER = "555 555 555";

        UserDataFaker() {}

        public static UserEntity defaultFakeUser() {
            return new Builder().build();
        }

        public static Builder fakeUserBuilder() {
            return new Builder();
        }

        public static class Builder {

            private String firstName;

            private String lastName;

            private String email;

            private String phoneNumber;

            Builder() {
                this.firstName = DEFAULT_FAKE_FIRST_NAME;
                this.lastName = DEFAULT_FAKE_LAST_NAME;
                this.email = DEFAULT_FAKE_EMAIL;
                this.phoneNumber = DEFAULT_FAKE_PHONE_NUMBER;
            }

            public Builder withFirstName(String firstName) {
                this.firstName = firstName;
                return this;
            }

            public Builder withLastName(String lastName) {
                this.lastName = lastName;
                return this;
            }

            public Builder withEmail(String email) {
                this.email = email;
                return this;
            }

            public Builder withPhoneNumber(String phoneNumber) {
                this.phoneNumber = phoneNumber;
                return this;
            }

            public UserEntity build() {
                return new UserEntity(null, firstName, lastName, email, phoneNumber);
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

            private static final String DEFAULT_FAKE_SUBJECT = "test.user@asapp.com";

            private static final String DEFAULT_FAKE_ROLE_CLAIM = "USER";

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
                return new EncodedJwtContentBuilder(ACCESS_TOKEN_TYPE);
            }

            public EncodedJwtContentBuilder refreshToken() {
                return new EncodedJwtContentBuilder(REFRESH_TOKEN_TYPE);
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
                    this.subject = DEFAULT_FAKE_SUBJECT;
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
                    var tokenUseClaim = ACCESS_TOKEN_TYPE.equals(type) ? ACCESS_TOKEN_USE_CLAIM_VALUE : REFRESH_TOKEN_USE_CLAIM_VALUE;
                    Map<String, Object> claimsMap = Map.of(ROLE_CLAIM_NAME, DEFAULT_FAKE_ROLE_CLAIM, TOKEN_USE_CLAIM_NAME, tokenUseClaim);
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
