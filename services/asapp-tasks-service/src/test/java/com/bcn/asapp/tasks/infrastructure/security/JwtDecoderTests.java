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

package com.bcn.asapp.tasks.infrastructure.security;

import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ROLE_CLAIM_NAME;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.TOKEN_USE_CLAIM_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

class JwtDecoderTests {

    private final String subject = "user@asapp.com";

    private final String tokenUseClaim = ACCESS_TOKEN_USE_CLAIM_VALUE;

    private final String roleClaim = "USER";

    private final Instant issuedAtValue = Instant.now()
                                                 .truncatedTo(ChronoUnit.SECONDS);

    private final Instant expirationValue = issuedAtValue.plus(15, ChronoUnit.MINUTES);

    private final Map<String, Object> accessTokenClaimsMap = Map.of(TOKEN_USE_CLAIM_NAME, tokenUseClaim, ROLE_CLAIM_NAME, roleClaim);

    private final SecretKey secretKey = Keys.hmacShaKeyFor(new byte[32]);

    private JwtDecoder jwtDecoder;

    @BeforeEach
    void beforeEach() {
        var jwtSecret = Base64.getEncoder()
                              .encodeToString(secretKey.getEncoded());
        jwtDecoder = new JwtDecoder(jwtSecret);
    }

    @Nested
    class DecodeToken {

        @Test
        void ThenThrowsException_GivenMalformedToken() {
            // When
            var thrown = catchThrowable(() -> jwtDecoder.decode("invalid.jwt.token"));

            // Then
            assertThat(thrown).isInstanceOf(MalformedJwtException.class);
        }

        @Test
        void ThenThrowsException_GivenTokenHasInvalidSignature() {
            // Given
            var differentSecretKey = Keys.hmacShaKeyFor("different-secret-key-with-at-least-32-bytes".getBytes());

            var tokenWithInvalidSignature = Jwts.builder()
                                                .header()
                                                .type(ACCESS_TOKEN_TYPE)
                                                .and()
                                                .subject(subject)
                                                .issuedAt(Date.from(issuedAtValue))
                                                .expiration(Date.from(expirationValue))
                                                .claims(accessTokenClaimsMap)
                                                .signWith(differentSecretKey)
                                                .compact();

            // When
            var thrown = catchThrowable(() -> jwtDecoder.decode(tokenWithInvalidSignature));

            // Then
            assertThat(thrown).isInstanceOf(SignatureException.class);
        }

        @Test
        void ThenThrowsException_GivenTokenHasExpired() {
            // Given
            var issuedAt = Instant.now()
                                  .minus(1, ChronoUnit.HOURS);
            var expiresAt = Instant.now()
                                   .minus(30, ChronoUnit.MINUTES);

            var expiredToken = Jwts.builder()
                                   .header()
                                   .type(ACCESS_TOKEN_TYPE)
                                   .and()
                                   .subject(subject)
                                   .issuedAt(Date.from(issuedAt))
                                   .expiration(Date.from(expiresAt))
                                   .claims(accessTokenClaimsMap)
                                   .signWith(secretKey)
                                   .compact();

            // When
            var thrown = catchThrowable(() -> jwtDecoder.decode(expiredToken));

            // Then
            assertThat(thrown).isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        void ThenReturnsJwt_GivenAccessTokenIsValid() {
            // Given
            var accessToken = Jwts.builder()
                                  .header()
                                  .type(ACCESS_TOKEN_TYPE)
                                  .and()
                                  .subject(subject)
                                  .issuedAt(Date.from(issuedAtValue))
                                  .expiration(Date.from(expirationValue))
                                  .claims(accessTokenClaimsMap)
                                  .signWith(secretKey)
                                  .compact();

            // When
            var actual = jwtDecoder.decode(accessToken);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(accessToken);
            assertThat(actual.type()).isEqualTo(ACCESS_TOKEN_TYPE);
            assertThat(actual.subject()).isEqualTo(subject);
            assertThat(actual.roleClaim()).isEqualTo(roleClaim);
            assertThat(actual.isAccessToken()).isTrue();

        }

    }

}
