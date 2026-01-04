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

package com.bcn.asapp.users.infrastructure.security;

import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.testutil.TestFactory.TestEncodedTokenFactory.testEncodedTokenBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Base64;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

class JwtDecoderTests {

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
        void ThrowsMalformedJwtException_MalformedToken() {
            // When
            var thrown = catchThrowable(() -> jwtDecoder.decode("invalid_token"));

            // Then
            assertThat(thrown).isInstanceOf(MalformedJwtException.class);
        }

        @Test
        void ThrowsSignatureException_InvalidSignatureToken() {
            // Given
            var differentSecretKey = Keys.hmacShaKeyFor("different-secret-key-with-at-least-32-bytes".getBytes());
            var tokenWithInvalidSignature = testEncodedTokenBuilder().accessToken()
                                                                     .withSecretKey(differentSecretKey)
                                                                     .build();

            // When
            var thrown = catchThrowable(() -> jwtDecoder.decode(tokenWithInvalidSignature));

            // Then
            assertThat(thrown).isInstanceOf(SignatureException.class);
        }

        @Test
        void ThrowsExpiredJwtException_ExpiredToken() {
            // Given
            var expiredToken = testEncodedTokenBuilder().accessToken()
                                                        .withSecretKey(secretKey)
                                                        .expired()
                                                        .build();

            // When
            var thrown = catchThrowable(() -> jwtDecoder.decode(expiredToken));

            // Then
            assertThat(thrown).isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        void ReturnsDecodedJwt_ValidAccessToken() {
            // Given
            var accessToken = testEncodedTokenBuilder().accessToken()
                                                       .withSecretKey(secretKey)
                                                       .build();

            // When
            var actual = jwtDecoder.decode(accessToken);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(accessToken);
            assertThat(actual.type()).isEqualTo(ACCESS_TOKEN_TYPE);
            assertThat(actual.subject()).isEqualTo("user@asapp.com");
            assertThat(actual.roleClaim()).isEqualTo("USER");
            assertThat(actual.isAccessToken()).isTrue();

        }

    }

}
