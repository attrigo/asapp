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

import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ROLE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.users.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static com.bcn.asapp.users.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedRefreshToken;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
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

@ExtendWith(MockitoExtension.class)
class JwtVerifierTests {

    @Mock
    private JwtDecoder jwtDecoder;

    @InjectMocks
    private JwtVerifier jwtVerifier;

    @Nested
    class VerifyAccessToken {

        @Test
        void ThenThrowsInvalidJwtException_GivenDecoderFails() {
            // Given
            var accessToken = defaultTestEncodedAccessToken();
            willThrow(new RuntimeException("Decoder failed")).given(jwtDecoder)
                                                             .decode(accessToken);

            // When
            var thrown = catchThrowable(() -> jwtVerifier.verifyAccessToken(accessToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Access token is not valid");

            then(jwtDecoder).should(times(1))
                            .decode(accessToken);
        }

        @Test
        void ThenThrowsInvalidJwtException_GivenTokenIsNotAccessToken() {
            // Given
            var refreshToken = defaultTestEncodedRefreshToken();
            var refreshTokenClaims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedRefreshToken = new DecodedToken(refreshToken, REFRESH_TOKEN_TYPE, "user@asapp.com", refreshTokenClaims);
            given(jwtDecoder.decode(refreshToken)).willReturn(decodedRefreshToken);

            // When
            var thrown = catchThrowable(() -> jwtVerifier.verifyAccessToken(refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Access token is not valid")
                              .hasCauseInstanceOf(UnexpectedJwtTypeException.class);

            then(jwtDecoder).should(times(1))
                            .decode(refreshToken);
        }

        @Test
        void ThenVerifiesAccessToken_GivenAccessTokenIsValid() {
            // Given
            var accessToken = defaultTestEncodedAccessToken();
            var accessTokenClaims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedAccessToken = new DecodedToken(accessToken, ACCESS_TOKEN_TYPE, "user@asapp.com", accessTokenClaims);
            given(jwtDecoder.decode(accessToken)).willReturn(decodedAccessToken);

            // When
            var actual = jwtVerifier.verifyAccessToken(accessToken);

            // Then
            assertThat(actual).isEqualTo(decodedAccessToken);
            assertThat(actual.isAccessToken()).isTrue();

            then(jwtDecoder).should(times(1))
                            .decode(accessToken);
        }

    }

}
