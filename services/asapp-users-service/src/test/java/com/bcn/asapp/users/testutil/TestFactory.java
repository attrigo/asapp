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

import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ROLE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.REFRESH_TOKEN_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.SecretKey;

import org.springframework.util.Assert;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.users.infrastructure.user.persistence.JdbcUserEntity;

public class TestFactory {

    public static final class TestUserFactory {

        static final String TEST_USER_FIRST_NAME = "FirstName";

        static final String TEST_USER_LAST_NAME = "LastName";

        static final String TEST_USER_EMAIL = "user@asapp.com";

        static final String TEST_USER_PHONE_NUMBER = "555 555 555";

        TestUserFactory() {}

        public static JdbcUserEntity defaultTestUser() {
            return new Builder().build();
        }

        public static Builder testUserBuilder() {
            return new Builder();
        }

        public static class Builder {

            private String firstName;

            private String lastName;

            private String email;

            private String phoneNumber;

            Builder() {
                firstName = TEST_USER_FIRST_NAME;
                lastName = TEST_USER_LAST_NAME;
                email = TEST_USER_EMAIL;
                phoneNumber = TEST_USER_PHONE_NUMBER;
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

            public JdbcUserEntity build() {
                return new JdbcUserEntity(null, firstName, lastName, email, phoneNumber);
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
