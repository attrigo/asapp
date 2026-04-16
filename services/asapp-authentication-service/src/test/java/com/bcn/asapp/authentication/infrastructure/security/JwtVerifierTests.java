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

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenMother.encodedRefreshToken;
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

import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.InvalidJwtException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.out.TokenStore;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;

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
    private TokenStore tokenStore;

    @InjectMocks
    private JwtVerifier jwtVerifier;

    @Nested
    class VerifyAccessToken {

        @Test
        void ReturnsDecodedJwt_ValidAccessToken() {
            // Given
            var encodedAccessTokenValue = encodedAccessToken();
            var encodedAccessToken = EncodedToken.of(encodedAccessTokenValue);
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessTokenValue, ACCESS_TOKEN_TYPE, "user@asapp.com", claims);

            given(jwtDecoder.decode(encodedAccessTokenValue)).willReturn(decodedJwt);
            given(tokenStore.accessTokenExists(encodedAccessToken)).willReturn(true);

            // When
            var actual = jwtVerifier.verifyAccessToken(encodedAccessToken);

            // Then
            assertThat(actual).isEqualTo(decodedJwt);
            assertThat(actual.isAccessToken()).isTrue();

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessTokenValue);
            then(tokenStore).should(times(1))
                            .accessTokenExists(encodedAccessToken);
        }

        @Test
        void ThrowsInvalidJwtException_DecoderFails() {
            // Given
            var encodedAccessTokenValue = encodedAccessToken();
            var encodedAccessToken = EncodedToken.of(encodedAccessTokenValue);

            willThrow(new RuntimeException("Decoder failed")).given(jwtDecoder)
                                                             .decode(encodedAccessTokenValue);

            // When
            var actual = catchThrowable(() -> jwtVerifier.verifyAccessToken(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Access token is not valid")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessTokenValue);
        }

        @Test
        void ThrowsUnexpectedJwtTypeException_NonAccessToken() {
            // Given
            var encodedRefreshTokenValue = encodedRefreshToken();
            var encodedRefreshToken = EncodedToken.of(encodedRefreshTokenValue);
            var claims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedRefreshTokenValue, REFRESH_TOKEN_TYPE, "user@asapp.com", claims);

            given(jwtDecoder.decode(encodedRefreshTokenValue)).willReturn(decodedJwt);

            // When
            var actual = catchThrowable(() -> jwtVerifier.verifyAccessToken(encodedRefreshToken));

            // Then
            assertThat(actual).isInstanceOf(UnexpectedJwtTypeException.class)
                              .hasMessageContaining("is not an access token");

            then(jwtDecoder).should(times(1))
                            .decode(encodedRefreshTokenValue);
        }

        @Test
        void ThrowsAuthenticationNotFoundException_AccessTokenNotInStore() {
            // Given
            var encodedAccessTokenValue = encodedAccessToken();
            var encodedAccessToken = EncodedToken.of(encodedAccessTokenValue);
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessTokenValue, ACCESS_TOKEN_TYPE, "user@asapp.com", claims);

            given(jwtDecoder.decode(encodedAccessTokenValue)).willReturn(decodedJwt);
            given(tokenStore.accessTokenExists(encodedAccessToken)).willReturn(false);

            // When
            var actual = catchThrowable(() -> jwtVerifier.verifyAccessToken(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Authentication session not found in store for access token");

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessTokenValue);
            then(tokenStore).should(times(1))
                            .accessTokenExists(encodedAccessToken);
        }

    }

    @Nested
    class VerifyRefreshToken {

        @Test
        void ReturnsDecodedJwt_ValidRefreshToken() {
            // Given
            var encodedRefreshTokenValue = encodedRefreshToken();
            var encodedRefreshToken = EncodedToken.of(encodedRefreshTokenValue);
            var claims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedRefreshTokenValue, REFRESH_TOKEN_TYPE, "user@asapp.com", claims);

            given(jwtDecoder.decode(encodedRefreshTokenValue)).willReturn(decodedJwt);
            given(tokenStore.refreshTokenExists(encodedRefreshToken)).willReturn(true);

            // When
            var actual = jwtVerifier.verifyRefreshToken(encodedRefreshToken);

            // Then
            assertThat(actual).isEqualTo(decodedJwt);
            assertThat(actual.isRefreshToken()).isTrue();

            then(jwtDecoder).should(times(1))
                            .decode(encodedRefreshTokenValue);
            then(tokenStore).should(times(1))
                            .refreshTokenExists(encodedRefreshToken);
        }

        @Test
        void ThrowsInvalidJwtException_DecoderFails() {
            // Given
            var encodedRefreshTokenValue = encodedRefreshToken();
            var encodedRefreshToken = EncodedToken.of(encodedRefreshTokenValue);

            willThrow(new RuntimeException("Decoder failed")).given(jwtDecoder)
                                                             .decode(encodedRefreshTokenValue);

            // When
            var actual = catchThrowable(() -> jwtVerifier.verifyRefreshToken(encodedRefreshToken));

            // Then
            assertThat(actual).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Refresh token is not valid")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(jwtDecoder).should(times(1))
                            .decode(encodedRefreshTokenValue);
        }

        @Test
        void ThrowsUnexpectedJwtTypeException_NonRefreshToken() {
            // Given
            var encodedAccessTokenValue = encodedAccessToken();
            var encodedAccessToken = EncodedToken.of(encodedAccessTokenValue);
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessTokenValue, ACCESS_TOKEN_TYPE, "user@asapp.com", claims);

            given(jwtDecoder.decode(encodedAccessTokenValue)).willReturn(decodedJwt);

            // When
            var actual = catchThrowable(() -> jwtVerifier.verifyRefreshToken(encodedAccessToken));

            // Then
            assertThat(actual).isInstanceOf(UnexpectedJwtTypeException.class)
                              .hasMessageContaining("is not a refresh token");

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessTokenValue);
        }

        @Test
        void ThrowsAuthenticationNotFoundException_RefreshTokenNotInStore() {
            // Given
            var encodedRefreshTokenValue = encodedRefreshToken();
            var encodedRefreshToken = EncodedToken.of(encodedRefreshTokenValue);
            var claims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedRefreshTokenValue, REFRESH_TOKEN_TYPE, "user@asapp.com", claims);

            given(jwtDecoder.decode(encodedRefreshTokenValue)).willReturn(decodedJwt);
            given(tokenStore.refreshTokenExists(encodedRefreshToken)).willReturn(false);

            // When
            var actual = catchThrowable(() -> jwtVerifier.verifyRefreshToken(encodedRefreshToken));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Authentication session not found in store for refresh token");

            then(jwtDecoder).should(times(1))
                            .decode(encodedRefreshTokenValue);
            then(tokenStore).should(times(1))
                            .refreshTokenExists(encodedRefreshToken);
        }

    }

}
