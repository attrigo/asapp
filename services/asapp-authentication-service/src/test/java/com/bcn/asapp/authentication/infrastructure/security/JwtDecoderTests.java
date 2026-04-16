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

package com.bcn.asapp.authentication.infrastructure.security;

import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenMother.anEncodedTokenBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;

/**
 * Tests {@link JwtDecoder} token validation, signature verification, and claim extraction.
 * <p>
 * Coverage:
 * <li>Rejects malformed token structure</li>
 * <li>Rejects tokens with invalid cryptographic signature</li>
 * <li>Rejects tokens when signature verification throws a cryptographic error</li>
 * <li>Rejects expired tokens based on expiration timestamp</li>
 * <li>Rejects tokens missing the expiration claim</li>
 * <li>Decodes valid tokens extracting type, subject, and claims</li>
 */
class JwtDecoderTests {

    private final SecretKey secretKey = new SecretKeySpec(new byte[32], "HmacSHA256");

    private JwtDecoder jwtDecoder;

    @BeforeEach
    void beforeEach() throws JOSEException {
        jwtDecoder = new JwtDecoder(new MACVerifier(secretKey));
    }

    @Nested
    class Decode {

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
                softly.assertThat(actual.isRefreshToken()).as("is refresh token").isFalse();
                softly.assertThat(actual.roleClaim()).as("role claim").isEqualTo(USER.name());
                // @formatter:on
            });
        }

        @Test
        void ReturnsDecodedJwt_ValidRefreshToken() {
            // Given
            var encodedRefreshToken = anEncodedTokenBuilder().refreshToken()
                                                             .withSecretKey(secretKey)
                                                             .build();

            // When
            var actual = jwtDecoder.decode(encodedRefreshToken);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("decoded JWT").isNotNull();
                softly.assertThat(actual.encodedToken()).as("encoded token").isEqualTo(encodedRefreshToken);
                softly.assertThat(actual.type()).as("type").isEqualTo(REFRESH_TOKEN_TYPE);
                softly.assertThat(actual.subject()).as("subject").isEqualTo("user@asapp.com");
                softly.assertThat(actual.isAccessToken()).as("is access token").isFalse();
                softly.assertThat(actual.isRefreshToken()).as("is refresh token").isTrue();
                softly.assertThat(actual.roleClaim()).as("role claim").isEqualTo(USER.name());
                // @formatter:on
            });
        }

        @Test
        void ThrowsJwtDecodeException_MalformedToken() {
            // When
            var actual = catchThrowable(() -> jwtDecoder.decode("invalid_token"));

            // Then
            assertThat(actual).isInstanceOf(JwtDecodeException.class)
                              .hasMessage("Malformed JWT token");
        }

        @Test
        void ThrowsJwtDecodeException_InvalidSignatureToken() {
            // Given
            var secretKey = new SecretKeySpec("invalid-secret-key-with-at-least-32-bytes".getBytes(), "HmacSHA256");
            var encodedAccessToken = anEncodedTokenBuilder().accessToken()
                                                            .withSecretKey(secretKey)
                                                            .build();

            // When
            var actual = catchThrowable(() -> jwtDecoder.decode(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(JwtDecodeException.class)
                              .hasMessage("JWT signature verification failed");
        }

        @Test
        void ThrowsJwtDecodeException_SignatureVerificationFails() throws JOSEException {
            // Given
            var macVerifier = mock(MACVerifier.class);
            var decoder = new JwtDecoder(macVerifier);
            var encodedAccessToken = anEncodedTokenBuilder().accessToken()
                                                            .withSecretKey(secretKey)
                                                            .build();

            given(macVerifier.verify(any(), any(), any())).willThrow(new JOSEException("algorithm not supported"));

            // When
            var actual = catchThrowable(() -> decoder.decode(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(JwtDecodeException.class)
                              .hasMessage("algorithm not supported");
        }

        @Test
        void ThrowsJwtDecodeException_ExpiredToken() {
            // Given
            var encodedAccessToken = anEncodedTokenBuilder().accessToken()
                                                            .withSecretKey(secretKey)
                                                            .expired()
                                                            .build();

            // When
            var actual = catchThrowable(() -> jwtDecoder.decode(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(JwtDecodeException.class)
                              .hasMessage("JWT has expired");
        }

        @Test
        void ThrowsJwtDecodeException_TokenWithoutExpiration() {
            // Given
            var encodedAccessToken = anEncodedTokenBuilder().accessToken()
                                                            .withSecretKey(secretKey)
                                                            .withExpiration(null)
                                                            .build();

            // When
            var actual = catchThrowable(() -> jwtDecoder.decode(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(JwtDecodeException.class)
                              .hasMessage("JWT has expired");
        }

    }

}
