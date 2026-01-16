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

import static com.bcn.asapp.tasks.infrastructure.security.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtClaimNames.ROLE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.tasks.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static com.bcn.asapp.tasks.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedRefreshToken;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.SecretKey;

import org.springframework.util.Assert;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.tasks.domain.task.Description;
import com.bcn.asapp.tasks.domain.task.EndDate;
import com.bcn.asapp.tasks.domain.task.StartDate;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskId;
import com.bcn.asapp.tasks.domain.task.Title;
import com.bcn.asapp.tasks.domain.task.UserId;
import com.bcn.asapp.tasks.infrastructure.security.DecodedJwt;
import com.bcn.asapp.tasks.infrastructure.task.persistence.JdbcTaskEntity;

public class TestFactory {

    public static final class TestTaskFactory {

        static final UUID TEST_TASK_USER_ID = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");

        static final String TEST_TASK_TITLE = "Title";

        static final String TEST_TASK_DESCRIPTION = "Description";

        static final Instant TEST_TASK_START_DATE = Instant.parse("2025-01-01T10:00:00Z");

        static final Instant TEST_TASK_END_DATE = Instant.parse("2025-01-02T10:00:00Z");

        TestTaskFactory() {}

        public static Task defaultTestDomainTask() {
            return testTaskBuilder().buildDomainEntity();
        }

        public static JdbcTaskEntity defaultTestJdbcTask() {
            return testTaskBuilder().buildJdbcEntity();
        }

        public static Builder testTaskBuilder() {
            return new Builder();
        }

        public static class Builder {

            private UUID taskId;

            private UUID userId;

            private String title;

            private String description;

            private Instant startDate;

            private Instant endDate;

            Builder() {
                taskId = UUID.randomUUID();
                userId = TEST_TASK_USER_ID;
                title = TEST_TASK_TITLE;
                description = TEST_TASK_DESCRIPTION;
                startDate = TEST_TASK_START_DATE;
                endDate = TEST_TASK_END_DATE;
            }

            public Builder taskId(UUID taskId) {
                this.taskId = taskId;
                return this;
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
                // Description is optional - allow null
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

            public Task buildDomainEntity() {
                var userIdVO = UserId.of(userId);
                var titleVO = Title.of(title);
                var descriptionVO = Description.of(description);
                var startDateVO = StartDate.of(startDate);
                var endDateVO = EndDate.of(endDate);

                if (userId == null) {
                    return Task.create(userIdVO, titleVO, descriptionVO, startDateVO, endDateVO);
                } else {
                    var taskIdVO = TaskId.of(taskId);
                    return Task.reconstitute(taskIdVO, userIdVO, titleVO, descriptionVO, startDateVO, endDateVO);
                }
            }

            public JdbcTaskEntity buildJdbcEntity() {
                return new JdbcTaskEntity(null, userId, title, description, startDate, endDate);
            }

        }

    }

    public static final class TestDecodedJwtFactory {

        private static final String TEST_DECODED_JWT_SUBJECT = "user@asapp.com";

        private static final String TEST_DECODED_JWT_ROLE = "USER";

        private static final Map<String, Object> TEST_DECODED_JWT_AT_CLAIMS = Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, TEST_DECODED_JWT_ROLE);

        private static final Map<String, Object> TEST_DECODED_JWT_RT_CLAIMS = Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, TEST_DECODED_JWT_ROLE);

        TestDecodedJwtFactory() {}

        public static DecodedJwt defaultTestDecodedAccessToken() {
            return testDecodedJwtBuilder().accessToken()
                                          .build();
        }

        public static DecodedJwt defaultTestDecodedRefreshToken() {
            return testDecodedJwtBuilder().refreshToken()
                                          .build();
        }

        public static Builder testDecodedJwtBuilder() {
            return new Builder();
        }

        public static class Builder {

            private String encodedToken;

            private String type;

            private String subject;

            private Map<String, Object> claims;

            Builder() {
                this.subject = TEST_DECODED_JWT_SUBJECT;
            }

            public Builder accessToken() {
                this.encodedToken = defaultTestEncodedAccessToken();
                this.type = ACCESS_TOKEN_TYPE;
                this.claims = TEST_DECODED_JWT_AT_CLAIMS;
                return this;
            }

            public Builder refreshToken() {
                this.encodedToken = defaultTestEncodedRefreshToken();
                this.type = REFRESH_TOKEN_TYPE;
                this.claims = TEST_DECODED_JWT_RT_CLAIMS;
                return this;
            }

            public Builder withEncodedToken(String encodedToken) {
                this.encodedToken = encodedToken;
                return this;
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

            public Builder withRole(Object role) {
                if (this.claims == null) {
                    this.claims = new HashMap<>();
                }
                this.claims.put(ROLE, role);
                return this;
            }

            public Builder withoutRoleClaim() {
                if (this.claims != null) {
                    this.claims.remove(ROLE);
                }
                return this;
            }

            public DecodedJwt build() {
                return new DecodedJwt(encodedToken, type, subject, claims);
            }

        }

    }

    public static final class TestEncodedTokenFactory {

        static final String JWT_SECRET;

        static final Long EXPIRATION_TIME = 300000L;

        static final String TEST_ENCODED_TOKEN_SUBJECT = "user@asapp.com";

        static final String TEST_ENCODED_TOKEN_ROLE_CLAIM = "USER";

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

    }

}
