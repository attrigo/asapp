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
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenFactory.anEncodedTokenBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
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

/**
 * Tests {@link JwtDecoder} token validation, signature verification, and claim extraction.
 * <p>
 * Coverage:
 * <li>Rejects malformed token structure</li>
 * <li>Rejects tokens with invalid cryptographic signature</li>
 * <li>Rejects expired tokens based on expiration timestamp</li>
 * <li>Decodes valid tokens extracting type, subject, and claims</li>
 */
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
        void ReturnsDecodedJwt_ValidAccessToken() {
            // Given
            var encodedAccessToken = anEncodedTokenBuilder().accessToken()
                                                            .withSecretKey(secretKey)
                                                            .build();

            // When
            var actual = jwtDecoder.decode(encodedAccessToken);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("decoded JWT").isNotNull();
                softly.assertThat(actual.encodedToken()).as("encoded token").isEqualTo(encodedAccessToken);
                softly.assertThat(actual.type()).as("type").isEqualTo(ACCESS_TOKEN_TYPE);
                softly.assertThat(actual.subject()).as("subject").isEqualTo("user@asapp.com");
                softly.assertThat(actual.isAccessToken()).as("is access token").isTrue();
                softly.assertThat(actual.roleClaim()).as("role claim").isEqualTo("USER");
                // @formatter:on
            });
        }

        @Test
        void ThrowsMalformedJwtException_MalformedToken() {
            // When
            var actual = catchThrowable(() -> jwtDecoder.decode("invalid_token"));

            // Then
            assertThat(actual).isInstanceOf(MalformedJwtException.class)
                              .hasMessageContaining("JWT");
        }

        @Test
        void ThrowsSignatureException_InvalidSignatureToken() {
            // Given
            var secretKey = Keys.hmacShaKeyFor("invalid-secret-key-with-at-least-32-bytes".getBytes());
            var encodedAccessToken = anEncodedTokenBuilder().accessToken()
                                                            .withSecretKey(secretKey)
                                                            .build();

            // When
            var actual = catchThrowable(() -> jwtDecoder.decode(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(SignatureException.class)
                              .hasMessageContaining("signature");
        }

        @Test
        void ThrowsExpiredJwtException_ExpiredToken() {
            // Given
            var encodedAccessToken = anEncodedTokenBuilder().accessToken()
                                                            .withSecretKey(secretKey)
                                                            .expired()
                                                            .build();

            // When
            var actual = catchThrowable(() -> jwtDecoder.decode(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(ExpiredJwtException.class)
                              .hasMessageContaining("expired");
        }

    }

}
