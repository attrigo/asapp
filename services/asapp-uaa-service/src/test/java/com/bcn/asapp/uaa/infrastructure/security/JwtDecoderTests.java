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

package com.bcn.asapp.uaa.infrastructure.security;

import static com.bcn.asapp.uaa.domain.authentication.Jwt.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.uaa.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.uaa.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.uaa.domain.user.Role.USER;
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

import com.bcn.asapp.uaa.domain.authentication.EncodedToken;
import com.bcn.asapp.uaa.domain.authentication.Expiration;
import com.bcn.asapp.uaa.domain.authentication.Issued;
import com.bcn.asapp.uaa.domain.authentication.JwtClaims;
import com.bcn.asapp.uaa.domain.authentication.Subject;

class JwtDecoderTests {

    private final String subjectValue = "user@asapp.com";

    private final Instant issuedAtValue = Instant.now()
                                                 .truncatedTo(ChronoUnit.SECONDS);

    private final Instant expirationValue = issuedAtValue.plus(15, ChronoUnit.MINUTES);

    private final Map<String, Object> accessTokenClaimsMap = Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, USER.name());

    private final Map<String, Object> refreshTokenClaimsMap = Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, USER.name());

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
        void ThenReturnsJwt_GivenAccessTokenIsValid() {
            // Given
            var accessToken = Jwts.builder()
                                  .header()
                                  .type(ACCESS_TOKEN.type())
                                  .and()
                                  .subject(subjectValue)
                                  .issuedAt(Date.from(issuedAtValue))
                                  .expiration(Date.from(expirationValue))
                                  .claims(accessTokenClaimsMap)
                                  .signWith(secretKey)
                                  .compact();

            var encodedToken = EncodedToken.of(accessToken);

            // When
            var actual = jwtDecoder.decode(encodedToken);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(ACCESS_TOKEN);
            assertThat(actual.subject()).extracting(Subject::value)
                                        .isEqualTo(subjectValue);
            assertThat(actual.issued()).extracting(Issued::value)
                                       .isEqualTo(issuedAtValue);
            assertThat(actual.expiration()).extracting(Expiration::value)
                                           .isEqualTo(expirationValue);
            assertThat(actual.claims()).extracting(JwtClaims::value)
                                       .isEqualTo(accessTokenClaimsMap);
            assertThat(actual.isAccessToken()).isTrue();
            assertThat(actual.isRefreshToken()).isFalse();
        }

        @Test
        void ThenReturnsJwt_GivenRefreshTokenIsValid() {
            // Given
            var refreshToken = Jwts.builder()
                                   .header()
                                   .type(REFRESH_TOKEN.type())
                                   .and()
                                   .subject(subjectValue)
                                   .issuedAt(Date.from(issuedAtValue))
                                   .expiration(Date.from(expirationValue))
                                   .claims(refreshTokenClaimsMap)
                                   .signWith(secretKey)
                                   .compact();

            var encodedToken = EncodedToken.of(refreshToken);

            // When
            var actual = jwtDecoder.decode(encodedToken);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(REFRESH_TOKEN);
            assertThat(actual.subject()).extracting(Subject::value)
                                        .isEqualTo(subjectValue);
            assertThat(actual.issued()).extracting(Issued::value)
                                       .isEqualTo(issuedAtValue);
            assertThat(actual.expiration()).extracting(Expiration::value)
                                           .isEqualTo(expirationValue);
            assertThat(actual.claims()).extracting(JwtClaims::value)
                                       .isEqualTo(refreshTokenClaimsMap);
            assertThat(actual.isAccessToken()).isFalse();
            assertThat(actual.isRefreshToken()).isTrue();
        }

        @Test
        void ThenThrowsException_GivenMalformedToken() {
            // Given
            var encodedToken = EncodedToken.of("invalid.jwt.token");

            // When
            var thrown = catchThrowable(() -> jwtDecoder.decode(encodedToken));

            // Then
            assertThat(thrown).isInstanceOf(MalformedJwtException.class);
        }

        @Test
        void ThenThrowsException_GivenTokenHasInvalidSignature() {
            // Given
            var differentSecretKey = Keys.hmacShaKeyFor("different-secret-key-with-at-least-32-bytes".getBytes());

            var tokenWithInvalidSignature = Jwts.builder()
                                                .header()
                                                .type(ACCESS_TOKEN.type())
                                                .and()
                                                .subject(subjectValue)
                                                .issuedAt(Date.from(issuedAtValue))
                                                .expiration(Date.from(expirationValue))
                                                .claims(accessTokenClaimsMap)
                                                .signWith(differentSecretKey)
                                                .compact();

            var encodedToken = EncodedToken.of(tokenWithInvalidSignature);

            // When
            var thrown = catchThrowable(() -> jwtDecoder.decode(encodedToken));

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
                                   .type(ACCESS_TOKEN.type())
                                   .and()
                                   .subject(subjectValue)
                                   .issuedAt(Date.from(issuedAt))
                                   .expiration(Date.from(expiresAt))
                                   .claims(accessTokenClaimsMap)
                                   .signWith(secretKey)
                                   .compact();

            var encodedToken = EncodedToken.of(expiredToken);

            // When
            var thrown = catchThrowable(() -> jwtDecoder.decode(encodedToken));

            // Then
            assertThat(thrown).isInstanceOf(ExpiredJwtException.class);
        }

    }

}
