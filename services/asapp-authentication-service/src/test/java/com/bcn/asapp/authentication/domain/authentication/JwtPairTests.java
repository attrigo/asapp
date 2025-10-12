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

package com.bcn.asapp.authentication.domain.authentication;

import static com.bcn.asapp.authentication.domain.authentication.Jwt.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JwtPairTests {

    private Jwt accessToken;

    private Jwt refreshToken;

    @BeforeEach
    void beforeEach() {
        var token = EncodedToken.of("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.encoded");
        var subject = Subject.of("user");
        var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE));
        var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE));
        var issued = Issued.now();
        var expiration = Expiration.of(issued, 1000L);

        accessToken = Jwt.of(token, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
        refreshToken = Jwt.of(token, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);
    }

    @Nested
    class CreateJwtPairWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenAccessTokenIsNull() {
            // When
            var thrown = catchThrowable(() -> new JwtPair(null, refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Access token must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRefreshTokeIsNull() {
            // When
            var thrown = catchThrowable(() -> new JwtPair(accessToken, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Refresh token must not be null");
        }

        @Test
        void ThenReturnsJwtClaims_GivenAccessTokenAndRefreshTokenAreValid() {
            // When
            var actual = new JwtPair(accessToken, refreshToken);

            // Then
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

    }

    @Nested
    class CreateJwtPairWithFactoryMethod {

        @Test
        void ThenThrowsIllegalArgumentException_GivenAccessTokenIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtPair.of(null, refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Access token must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRefreshTokeIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtPair.of(accessToken, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Refresh token must not be null");
        }

        @Test
        void ThenReturnsJwtClaims_GivenAccessTokenAndRefreshTokenAreValid() {
            // When
            var actual = JwtPair.of(accessToken, refreshToken);

            // Then
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

    }

}
