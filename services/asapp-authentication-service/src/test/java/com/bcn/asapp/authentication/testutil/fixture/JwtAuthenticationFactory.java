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

package com.bcn.asapp.authentication.testutil.fixture;

import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_ACCESS_TOKEN_CLAIMS;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_AUTHENTICATION_ID;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_REFRESH_TOKEN_CLAIMS;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_SUBJECT;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_USER_ID;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.EXPIRATION_TIME_MILLIS;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.TOKEN_EXPIRED_OFFSET_MILLIS;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.generateRandomIssueAt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtClaimsEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtEntity;

/**
 * Provides test data builders for JwtAuthentication domain entities and JdbcJwtAuthenticationEntity instances with fluent API.
 *
 * @since 0.2.0
 */
public final class JwtAuthenticationFactory {

    private JwtAuthenticationFactory() {}

    public static JwtAuthentication anAuthenticatedJwtAuthentication() {
        return aJwtAuthenticationBuilder().build();
    }

    public static JwtAuthentication anUnauthenticatedJwtAuthentication() {
        return aJwtAuthenticationBuilder().withAuthenticationId(null)
                                          .build();
    }

    public static JdbcJwtAuthenticationEntity aJdbcJwtAuthentication() {
        return aJwtAuthenticationBuilder().buildJdbc();
    }

    public static Builder aJwtAuthenticationBuilder() {
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
            this.authenticationId = DEFAULT_AUTHENTICATION_ID;
            this.userId = DEFAULT_USER_ID;

            this.atSubject = DEFAULT_SUBJECT;
            this.atClaims = DEFAULT_ACCESS_TOKEN_CLAIMS;
            this.atIssued = generateRandomIssueAt();
            this.atExpiration = atIssued.plusMillis(EXPIRATION_TIME_MILLIS);

            this.rtSubject = DEFAULT_SUBJECT;
            this.rtClaims = DEFAULT_REFRESH_TOKEN_CLAIMS;
            this.rtIssued = generateRandomIssueAt();
            this.rtExpiration = rtIssued.plusMillis(EXPIRATION_TIME_MILLIS);
        }

        public Builder withUserId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder withAuthenticationId(UUID authenticationId) {
            this.authenticationId = authenticationId;
            return this;
        }

        public Builder withTokenValues(String accessTokenValue, String refreshTokenValue) {
            this.atEncodedToken = accessTokenValue;
            this.rtEncodedToken = refreshTokenValue;
            return this;
        }

        // Access Token modifiers
        public Builder withAccessTokenValue(String value) {
            this.atEncodedToken = value;
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
            var issued = now.minusMillis(EXPIRATION_TIME_MILLIS + TOKEN_EXPIRED_OFFSET_MILLIS);
            var expiration = now.minusMillis(TOKEN_EXPIRED_OFFSET_MILLIS);
            return withAccessTokenIssued(issued).withAccessTokenExpiration(expiration);
        }

        // Refresh Token modifiers
        public Builder withRefreshTokenValue(String value) {
            this.rtEncodedToken = value;
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
            var issued = now.minusMillis(EXPIRATION_TIME_MILLIS + TOKEN_EXPIRED_OFFSET_MILLIS);
            var expiration = now.minusMillis(TOKEN_EXPIRED_OFFSET_MILLIS);
            return withRefreshTokenIssued(issued).withRefreshTokenExpiration(expiration);
        }

        // Builders
        public JwtAuthentication build() {
            atEncodedToken = EncodedTokenFactory.anEncodedTokenBuilder()
                                                .withType(ACCESS_TOKEN.type())
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

            rtEncodedToken = EncodedTokenFactory.anEncodedTokenBuilder()
                                                .withType(REFRESH_TOKEN.type())
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

        public JdbcJwtAuthenticationEntity buildJdbc() {
            atEncodedToken = EncodedTokenFactory.anEncodedTokenBuilder()
                                                .withType(ACCESS_TOKEN_TYPE)
                                                .withSubject(atSubject)
                                                .withClaims(atClaims)
                                                .withIssuedAt(atIssued)
                                                .withExpiration(atExpiration)
                                                .build();
            var atClaimsEntity = new JdbcJwtClaimsEntity(atClaims);
            var accessTokenEntity = new JdbcJwtEntity(atEncodedToken, ACCESS_TOKEN_TYPE, atSubject, atClaimsEntity, atIssued, atExpiration);

            rtEncodedToken = EncodedTokenFactory.anEncodedTokenBuilder()
                                                .withType(REFRESH_TOKEN_TYPE)
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
