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

package com.bcn.asapp.authentication.infrastructure.security;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedRefreshToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

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
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;

@ExtendWith(MockitoExtension.class)
class JwtVerifierTests {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtStore jwtStore;

    @InjectMocks
    private JwtVerifier jwtVerifier;

    @Nested
    class VerifyAccessToken {

        @Test
        void ThrowsInvalidJwtException_DecoderFails() {
            // Given
            var accessTokenString = defaultTestEncodedAccessToken();
            var accessToken = EncodedToken.of(accessTokenString);
            willThrow(new RuntimeException("Decoder failed")).given(jwtDecoder)
                                                             .decode(accessToken);

            // When
            var thrown = catchThrowable(() -> jwtVerifier.verifyAccessToken(accessToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Access token is not valid")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(jwtDecoder).should(times(1))
                            .decode(accessToken);
        }

        @Test
        void ThrowsUnexpectedJwtTypeException_TokenNotAccessToken() {
            // Given
            var refreshTokenString = defaultTestEncodedRefreshToken();
            var refreshToken = EncodedToken.of(refreshTokenString);
            var refreshTokenClaims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedRefreshToken = new DecodedJwt(refreshTokenString, REFRESH_TOKEN_TYPE, "user@asapp.com", refreshTokenClaims);
            given(jwtDecoder.decode(refreshToken)).willReturn(decodedRefreshToken);

            // When
            var thrown = catchThrowable(() -> jwtVerifier.verifyAccessToken(refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(UnexpectedJwtTypeException.class)
                              .hasMessageContaining("is not an access token");

            then(jwtDecoder).should(times(1))
                            .decode(refreshToken);
        }

        @Test
        void ThrowsAuthenticationNotFoundException_AccessTokenNotInStore() {
            // Given
            var accessTokenString = defaultTestEncodedAccessToken();
            var accessToken = EncodedToken.of(accessTokenString);
            var accessTokenClaims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedAccessToken = new DecodedJwt(accessTokenString, ACCESS_TOKEN_TYPE, "user@asapp.com", accessTokenClaims);
            given(jwtDecoder.decode(accessToken)).willReturn(decodedAccessToken);
            given(jwtStore.accessTokenExists(accessToken)).willReturn(false);

            // When
            var thrown = catchThrowable(() -> jwtVerifier.verifyAccessToken(accessToken));

            // Then
            assertThat(thrown).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Authentication session not found in store for access token");

            then(jwtDecoder).should(times(1))
                            .decode(accessToken);
            then(jwtStore).should(times(1))
                          .accessTokenExists(accessToken);
        }

        @Test
        void ReturnsDecodedJwt_ValidAccessToken() {
            // Given
            var accessTokenString = defaultTestEncodedAccessToken();
            var accessToken = EncodedToken.of(accessTokenString);
            var accessTokenClaims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedAccessToken = new DecodedJwt(accessTokenString, ACCESS_TOKEN_TYPE, "user@asapp.com", accessTokenClaims);
            given(jwtDecoder.decode(accessToken)).willReturn(decodedAccessToken);
            given(jwtStore.accessTokenExists(accessToken)).willReturn(true);

            // When
            var actual = jwtVerifier.verifyAccessToken(accessToken);

            // Then
            assertThat(actual).isEqualTo(decodedAccessToken);
            assertThat(actual.isAccessToken()).isTrue();

            then(jwtDecoder).should(times(1))
                            .decode(accessToken);
            then(jwtStore).should(times(1))
                          .accessTokenExists(accessToken);
        }

    }

    @Nested
    class VerifyRefreshToken {

        @Test
        void ThrowsInvalidJwtException_DecoderFails() {
            // Given
            var refreshTokenString = defaultTestEncodedRefreshToken();
            var refreshToken = EncodedToken.of(refreshTokenString);
            willThrow(new RuntimeException("Decoder failed")).given(jwtDecoder)
                                                             .decode(refreshToken);

            // When
            var thrown = catchThrowable(() -> jwtVerifier.verifyRefreshToken(refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Refresh token is not valid")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(jwtDecoder).should(times(1))
                            .decode(refreshToken);
        }

        @Test
        void ThrowsUnexpectedJwtTypeException_TokenNotRefreshToken() {
            // Given
            var accessTokenString = defaultTestEncodedAccessToken();
            var accessToken = EncodedToken.of(accessTokenString);
            var accessTokenClaims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedAccessToken = new DecodedJwt(accessTokenString, ACCESS_TOKEN_TYPE, "user@asapp.com", accessTokenClaims);
            given(jwtDecoder.decode(accessToken)).willReturn(decodedAccessToken);

            // When
            var thrown = catchThrowable(() -> jwtVerifier.verifyRefreshToken(accessToken));

            // Then
            assertThat(thrown).isInstanceOf(UnexpectedJwtTypeException.class)
                              .hasMessageContaining("is not a refresh token");

            then(jwtDecoder).should(times(1))
                            .decode(accessToken);
        }

        @Test
        void ThrowsAuthenticationNotFoundException_RefreshTokenNotInStore() {
            // Given
            var refreshTokenString = defaultTestEncodedRefreshToken();
            var refreshToken = EncodedToken.of(refreshTokenString);
            var refreshTokenClaims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedRefreshToken = new DecodedJwt(refreshTokenString, REFRESH_TOKEN_TYPE, "user@asapp.com", refreshTokenClaims);
            given(jwtDecoder.decode(refreshToken)).willReturn(decodedRefreshToken);
            given(jwtStore.refreshTokenExists(refreshToken)).willReturn(false);

            // When
            var thrown = catchThrowable(() -> jwtVerifier.verifyRefreshToken(refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Authentication session not found in store for refresh token");

            then(jwtDecoder).should(times(1))
                            .decode(refreshToken);
            then(jwtStore).should(times(1))
                          .refreshTokenExists(refreshToken);
        }

        @Test
        void ReturnsDecodedJwt_ValidRefreshToken() {
            // Given
            var refreshTokenString = defaultTestEncodedRefreshToken();
            var refreshToken = EncodedToken.of(refreshTokenString);
            var refreshTokenClaims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedRefreshToken = new DecodedJwt(refreshTokenString, REFRESH_TOKEN_TYPE, "user@asapp.com", refreshTokenClaims);
            given(jwtDecoder.decode(refreshToken)).willReturn(decodedRefreshToken);
            given(jwtStore.refreshTokenExists(refreshToken)).willReturn(true);

            // When
            var actual = jwtVerifier.verifyRefreshToken(refreshToken);

            // Then
            assertThat(actual).isEqualTo(decodedRefreshToken);
            assertThat(actual.isRefreshToken()).isTrue();

            then(jwtDecoder).should(times(1))
                            .decode(refreshToken);
            then(jwtStore).should(times(1))
                          .refreshTokenExists(refreshToken);
        }

    }

}
