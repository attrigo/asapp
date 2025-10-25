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

import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ROLE_CLAIM_NAME;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.TOKEN_USE_CLAIM_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.tasks.infrastructure.task.out.entity.TaskEntity;

public class TestDataFaker {

    public static class TaskDataFaker {

        public static final String DEFAULT_FAKE_TITLE = "Title";

        public static final String DEFAULT_FAKE_DESCRIPTION = "Description";

        public static final Instant DEFAULT_FAKE_START_DATE = Instant.now()
                                                                     .truncatedTo(ChronoUnit.SECONDS);

        public static final Instant DEFAULT_FAKE_END_DATE = DEFAULT_FAKE_START_DATE.plus(1, ChronoUnit.DAYS);

        TaskDataFaker() {}

        public static TaskEntity defaultFakeTask() {
            return new Builder().build();
        }

        public static Builder fakeTaskBuilder() {
            return new Builder();
        }

        public static class Builder {

            private UUID userId;

            private String title;

            private String description;

            private Instant startDate;

            private Instant endDate;

            Builder() {
                this.userId = UUID.randomUUID();
                this.title = DEFAULT_FAKE_TITLE;
                this.description = DEFAULT_FAKE_DESCRIPTION;
                this.startDate = DEFAULT_FAKE_START_DATE;
                this.endDate = DEFAULT_FAKE_END_DATE;
            }

            public Builder withUserId(UUID userId) {
                this.userId = userId;
                return this;
            }

            public Builder withTitle(String title) {
                this.title = title;
                return this;
            }

            public Builder withDescription(String description) {
                this.description = description;
                return this;
            }

            public Builder withStartDate(Instant startDate) {
                this.startDate = startDate;
                return this;
            }

            public Builder withEndDate(Instant endDate) {
                this.endDate = endDate;
                return this;
            }

            public TaskEntity build() {
                return new TaskEntity(null, userId, title, description, startDate, endDate);
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
