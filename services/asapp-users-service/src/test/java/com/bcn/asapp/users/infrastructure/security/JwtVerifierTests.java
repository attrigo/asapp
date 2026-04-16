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

package com.bcn.asapp.users.infrastructure.security;

import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ROLE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenMother.encodedRefreshToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link JwtVerifier} decode-then-verify pipeline and session validation.
 * <p>
 * Coverage:
 * <li>Decoding failures prevent verification workflow completion</li>
 * <li>Token type mismatches throw domain exception</li>
 * <li>Missing session in store throws authentication not found</li>
 * <li>Successful verification returns decoded JWT with validated session</li>
 */
@ExtendWith(MockitoExtension.class)
class JwtVerifierTests {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private RedisJwtStore redisJwtStore;

    @InjectMocks
    private JwtVerifier jwtVerifier;

    @Nested
    class VerifyAccessToken {

        @Test
        void ReturnsDecodedJwt_ValidAccessToken() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, "user@asapp.com", claims);

            given(jwtDecoder.decode(encodedAccessToken)).willReturn(decodedJwt);
            given(redisJwtStore.accessTokenExists(encodedAccessToken)).willReturn(true);

            // When
            var actual = jwtVerifier.verifyAccessToken(encodedAccessToken);

            // Then
            assertThat(actual).isEqualTo(decodedJwt);
            assertThat(actual.isAccessToken()).isTrue();

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessToken);
            then(redisJwtStore).should(times(1))
                               .accessTokenExists(encodedAccessToken);
        }

        @Test
        void ThrowsInvalidJwtException_DecoderFails() {
            // Given
            var encodedAccessToken = encodedAccessToken();

            willThrow(new RuntimeException("Decoder failed")).given(jwtDecoder)
                                                             .decode(encodedAccessToken);

            // When
            var actual = catchThrowable(() -> jwtVerifier.verifyAccessToken(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Access token is not valid")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessToken);
        }

        @Test
        void ThrowsUnexpectedJwtTypeException_NonAccessToken() {
            // Given
            var encodedRefreshToken = encodedRefreshToken();
            var claims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedRefreshToken, REFRESH_TOKEN_TYPE, "user@asapp.com", claims);

            given(jwtDecoder.decode(encodedRefreshToken)).willReturn(decodedJwt);

            // When
            var actual = catchThrowable(() -> jwtVerifier.verifyAccessToken(encodedRefreshToken));

            // Then
            assertThat(actual).isInstanceOf(UnexpectedJwtTypeException.class)
                              .hasMessageContaining("is not an access token");

            then(jwtDecoder).should(times(1))
                            .decode(encodedRefreshToken);
        }

        @Test
        void ThrowsAuthenticationNotFoundException_AccessTokenNotInStore() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, "user@asapp.com", claims);

            given(jwtDecoder.decode(encodedAccessToken)).willReturn(decodedJwt);
            given(redisJwtStore.accessTokenExists(encodedAccessToken)).willReturn(false);

            // When
            var actual = catchThrowable(() -> jwtVerifier.verifyAccessToken(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Authentication session not found in store for access token");

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessToken);
            then(redisJwtStore).should(times(1))
                               .accessTokenExists(encodedAccessToken);
        }

    }

}
