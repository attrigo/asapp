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

import static com.bcn.asapp.users.infrastructure.security.DecodedToken.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.ROLE_CLAIM_NAME;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.TOKEN_USE_CLAIM_NAME;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
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

    private String encodedAccessToken;

    private DecodedToken decodedAccessToken;

    private DecodedToken decodedRefreshToken;

    @BeforeEach
    void beforeEach() {
        encodedAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.accessToken";
        String encodedRefreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refreshToken";

        var subject = "user@asapp.com";
        var accessTokenClaims = Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, "USER");
        var refreshTokenClaims = Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, "USER");

        decodedAccessToken = new DecodedToken(encodedAccessToken, ACCESS_TOKEN_TYPE, subject, accessTokenClaims);
        decodedRefreshToken = new DecodedToken(encodedRefreshToken, REFRESH_TOKEN_TYPE, subject, refreshTokenClaims);
    }

    @Nested
    class VerifyAccessToken {

        @Test
        void ThenThrowsInvalidJwtException_GivenDecoderFails() {
            // Given
            willThrow(new RuntimeException("Decoder failed")).given(jwtDecoder)
                                                             .decode(encodedAccessToken);

            // When
            var thrown = catchThrowable(() -> jwtVerifier.verifyAccessToken(encodedAccessToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Access token is not valid");

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessToken);
        }

        @Test
        void ThenThrowsInvalidJwtException_GivenTokenIsNotAccessToken() {
            // Given
            given(jwtDecoder.decode(encodedAccessToken)).willReturn(decodedRefreshToken);

            // When
            var thrown = catchThrowable(() -> jwtVerifier.verifyAccessToken(encodedAccessToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Access token is not valid")
                              .hasCauseInstanceOf(UnexpectedJwtTypeException.class);

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessToken);
        }

        @Test
        void ThenVerifiesAccessToken_GivenAccessTokenIsValid() {
            // Given
            given(jwtDecoder.decode(encodedAccessToken)).willReturn(decodedAccessToken);

            // When
            var result = jwtVerifier.verifyAccessToken(encodedAccessToken);

            // Then
            assertThat(result).isEqualTo(decodedAccessToken);
            assertThat(result.isAccessToken()).isTrue();

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessToken);
        }

    }

}
