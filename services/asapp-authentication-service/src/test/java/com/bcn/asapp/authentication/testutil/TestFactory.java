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
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.testEncodedTokenBuilder;

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

import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.user.EncodedPassword;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.User;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtClaimsEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtEntity;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;

public class TestFactory {

    private static final Long TEST_EXPIRATION_TIME_MILLIS = 300000L;

    private static final Long TEST_TOKEN_EXPIRED_OFFSET_MILLIS = 60000L;

    public static final class TestUserFactory {

        static final String TEST_USER_USERNAME = "user@asapp.com";

        static final String TEST_USER_RAW_PASSWORD = "TEST@09_password?!";

        static final Role TEST_USER_ROLE = USER;

        TestUserFactory() {}

        public static User defaultTestDomainUser() {
            return testUserBuilder().buildDomainEntity();
        }

        public static JdbcUserEntity defaultTestJdbcUser() {
            return testUserBuilder().buildJdbcEntity();
        }

        public static Builder testUserBuilder() {
            return new Builder();
        }

        public static class Builder {

            private UUID userId;

            private String username;

            private String password;

            private String role;

            private String passwordEncoderPrefix;

            private PasswordEncoder passwordEncoder;

            Builder() {
                this.userId = UUID.randomUUID();
                this.username = TEST_USER_USERNAME;
                this.password = TEST_USER_RAW_PASSWORD;
                this.role = TEST_USER_ROLE.name();
                this.passwordEncoderPrefix = "{bcrypt}";
                this.passwordEncoder = new BCryptPasswordEncoder();
            }

            public Builder withUserId(UUID userId) {
                this.userId = userId;
                return this;
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

            public User buildDomainEntity() {
                var usernameVO = Username.of(username);
                var roleVO = Role.valueOf(role);

                if (userId == null) {
                    var encodedPasswordVO = EncodedPassword.of(passwordEncoderPrefix + passwordEncoder.encode(password));
                    return User.inactiveUser(usernameVO, encodedPasswordVO, roleVO);
                } else {
                    var userIdVO = UserId.of(userId);
                    return User.activeUser(userIdVO, usernameVO, roleVO);
                }
            }

            public JdbcUserEntity buildJdbcEntity() {
                var encodedPasswordEntity = passwordEncoderPrefix + passwordEncoder.encode(password);
                return new JdbcUserEntity(null, username, encodedPasswordEntity, role);
            }

        }

    }

    public static final class TestJwtAuthenticationFactory {

        private static final String TEST_JWT_AUTH_SUBJECT = "user@asapp.com";

        private static final String TEST_JWT_AUTH_ROLE = USER.name();

        private static final Map<String, Object> TEST_JWT_AUTH_AT_CLAIMS = Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, TEST_JWT_AUTH_ROLE);

        private static final Map<String, Object> TEST_JWT_AUTH_RT_CLAIMS = Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, TEST_JWT_AUTH_ROLE);

        TestJwtAuthenticationFactory() {}

        public static JwtAuthentication defaultTestDomainJwtAuthentication() {
            return testJwtAuthenticationBuilder().buildDomainEntity();
        }

        public static JdbcJwtAuthenticationEntity defaultTestJdbcJwtAuthentication() {
            return testJwtAuthenticationBuilder().buildJdbcEntity();
        }

        public static Builder testJwtAuthenticationBuilder() {
            return new Builder();
        }

        public static class Builder {

            private UUID authenticationId;

            private UUID userId;

            private String atEncodedToken;

            private String atSubject;

            private Map<String, Object> atClaims;

            private Instant atIssued;

            private Instant atExpiration;

            private String rtEncodedToken;

            private String rtSubject;

            private Map<String, Object> rtClaims;

            private Instant rtIssued;

            private Instant rtExpiration;

            Builder() {
                this.authenticationId = UUID.randomUUID();
                this.userId = UUID.randomUUID();

                this.atSubject = TEST_JWT_AUTH_SUBJECT;
                this.atClaims = TEST_JWT_AUTH_AT_CLAIMS;
                this.atIssued = generateRandomIssueAt();
                this.atExpiration = atIssued.plusMillis(TEST_EXPIRATION_TIME_MILLIS);

                this.rtSubject = TEST_JWT_AUTH_SUBJECT;
                this.rtClaims = TEST_JWT_AUTH_RT_CLAIMS;
                this.rtIssued = generateRandomIssueAt();
                this.rtExpiration = rtIssued.plusMillis(TEST_EXPIRATION_TIME_MILLIS);
            }

            public Builder withUserId(UUID userId) {
                this.userId = userId;
                return this;
            }

            public Builder withAuthenticationId(UUID authenticationId) {
                this.authenticationId = authenticationId;
                return this;
            }

            // Access Token modifiers
            public Builder withAccessTokenEncodedToken(String encodedToken) {
                this.atEncodedToken = encodedToken;
                return this;
            }

            public Builder withAccessTokenSubject(String subject) {
                this.atSubject = subject;
                return this;
            }

            public Builder withAccessTokenClaims(Map<String, Object> claims) {
                this.atClaims = claims;
                return this;
            }

            public Builder withAccessTokenIssued(Instant issued) {
                this.atIssued = issued;
                return this;
            }

            public Builder withAccessTokenExpiration(Instant expiration) {
                this.atExpiration = expiration;
                return this;
            }

            public Builder withAccessTokenExpired() {
                var now = Instant.now();
                var issued = now.minusMillis(TEST_EXPIRATION_TIME_MILLIS + TEST_TOKEN_EXPIRED_OFFSET_MILLIS);
                var expiration = now.minusMillis(TEST_TOKEN_EXPIRED_OFFSET_MILLIS);
                return withAccessTokenIssued(issued).withAccessTokenExpiration(expiration);
            }

            // Refresh Token modifiers
            public Builder withRefreshTokenEncodedToken(String encodedToken) {
                this.rtEncodedToken = encodedToken;
                return this;
            }

            public Builder withRefreshTokenSubject(String subject) {
                this.rtSubject = subject;
                return this;
            }

            public Builder withRefreshTokenClaims(Map<String, Object> claims) {
                this.rtClaims = claims;
                return this;
            }

            public Builder withRefreshTokenIssued(Instant issued) {
                this.rtIssued = issued;
                return this;
            }

            public Builder withRefreshTokenExpiration(Instant expiration) {
                this.rtExpiration = expiration;
                return this;
            }

            public Builder withRefreshTokenExpired() {
                var now = Instant.now();
                var issued = now.minusMillis(TEST_EXPIRATION_TIME_MILLIS + TEST_TOKEN_EXPIRED_OFFSET_MILLIS);
                var expiration = now.minusMillis(TEST_TOKEN_EXPIRED_OFFSET_MILLIS);
                return withRefreshTokenIssued(issued).withRefreshTokenExpiration(expiration);
            }

            // Builders
            public JwtAuthentication buildDomainEntity() {
                atEncodedToken = testEncodedTokenBuilder().withType(ACCESS_TOKEN.type())
                                                          .withSubject(atSubject)
                                                          .withClaims(atClaims)
                                                          .withIssuedAt(atIssued)
                                                          .withExpiration(atExpiration)
                                                          .build();
                var atEncodedTokenVO = EncodedToken.of(atEncodedToken);
                var atSubjectVO = Subject.of(atSubject);
                var atClaimsVO = JwtClaims.of(atClaims);
                var atIssuedVO = Issued.of(atIssued);
                var atExpirationVO = new Expiration(atExpiration);
                var accessTokenVO = Jwt.of(atEncodedTokenVO, ACCESS_TOKEN, atSubjectVO, atClaimsVO, atIssuedVO, atExpirationVO);

                rtEncodedToken = testEncodedTokenBuilder().withType(REFRESH_TOKEN.type())
                                                          .withSubject(rtSubject)
                                                          .withClaims(rtClaims)
                                                          .withIssuedAt(rtIssued)
                                                          .withExpiration(rtExpiration)
                                                          .build();
                var rtEncodedTokenVO = EncodedToken.of(rtEncodedToken);
                var rtSubjectVO = Subject.of(rtSubject);
                var rtClaimsVO = JwtClaims.of(rtClaims);
                var rtIssuedVO = Issued.of(rtIssued);
                var rtExpirationVO = new Expiration(rtExpiration);
                var refreshTokenVO = Jwt.of(rtEncodedTokenVO, REFRESH_TOKEN, rtSubjectVO, rtClaimsVO, rtIssuedVO, rtExpirationVO);

                var userIdVO = UserId.of(userId);
                var jwtPair = JwtPair.of(accessTokenVO, refreshTokenVO);

                if (authenticationId == null) {
                    return JwtAuthentication.unAuthenticated(userIdVO, jwtPair);
                } else {
                    var authenticationIdVO = JwtAuthenticationId.of(authenticationId);
                    return JwtAuthentication.authenticated(authenticationIdVO, userIdVO, jwtPair);
                }
            }

            public JdbcJwtAuthenticationEntity buildJdbcEntity() {
                atEncodedToken = testEncodedTokenBuilder().withType(ACCESS_TOKEN_TYPE)
                                                          .withSubject(atSubject)
                                                          .withClaims(atClaims)
                                                          .withIssuedAt(atIssued)
                                                          .withExpiration(atExpiration)
                                                          .build();
                var atClaimsEntity = new JdbcJwtClaimsEntity(atClaims);
                var accessTokenEntity = new JdbcJwtEntity(atEncodedToken, ACCESS_TOKEN_TYPE, atSubject, atClaimsEntity, atIssued, atExpiration);

                rtEncodedToken = testEncodedTokenBuilder().withType(REFRESH_TOKEN_TYPE)
                                                          .withSubject(rtSubject)
                                                          .withClaims(rtClaims)
                                                          .withIssuedAt(rtIssued)
                                                          .withExpiration(rtExpiration)
                                                          .build();
                var rtClaimsEntity = new JdbcJwtClaimsEntity(rtClaims);
                var refreshTokenEntity = new JdbcJwtEntity(rtEncodedToken, REFRESH_TOKEN_TYPE, rtSubject, rtClaimsEntity, rtIssued, rtExpiration);

                return new JdbcJwtAuthenticationEntity(null, userId, accessTokenEntity, refreshTokenEntity);
            }

        }

    }

    public static final class TestEncodedTokenFactory {

        private static final String JWT_SECRET;

        private static final String TEST_ENCODED_TOKEN_SUBJECT = "user@asapp.com";

        private static final String TEST_ENCODED_TOKEN_ROLE_CLAIM = USER.name();

        private static final Map<String, Object> TEST_ENCODED_TOKEN_AT_CLAIMS = Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, TEST_ENCODED_TOKEN_ROLE_CLAIM);

        private static final Map<String, Object> TEST_ENCODED_TOKEN_RT_CLAIMS = Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, TEST_ENCODED_TOKEN_ROLE_CLAIM);

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
                expiration = issuedAt.plusMillis(TEST_EXPIRATION_TIME_MILLIS);
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
                var now = Instant.now();
                var issuedAt = now.minusMillis(TEST_EXPIRATION_TIME_MILLIS + TEST_TOKEN_EXPIRED_OFFSET_MILLIS);
                var expiration = now.minusMillis(TEST_TOKEN_EXPIRED_OFFSET_MILLIS);
                return withIssuedAt(issuedAt).withExpiration(expiration);
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
        var now = Instant.now();
        var fourMinsAgo = Instant.now()
                                 .minusMillis(TEST_EXPIRATION_TIME_MILLIS - 30000);
        var startMillis = fourMinsAgo.toEpochMilli();
        var endMillis = now.toEpochMilli();
        var randomMillis = ThreadLocalRandom.current()
                                            .nextLong(startMillis, endMillis + 1);
        return Instant.ofEpochMilli(randomMillis);
    }

}
