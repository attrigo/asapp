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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class DecodedJwtTests {

    private final String encodedToken = defaultTestEncodedAccessToken();

    private final String subject = "user@asapp.com";

    private final String roleClaim = "USER";

    private final Map<String, Object> claims = Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, roleClaim);

    @Nested
    class CreateDecodedJwt {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenEncodedTokenIsNull(String encodedToken) {
            // When
            var thrown = catchThrowable(() -> new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, claims));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Encoded token must not be blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenTypeIsNull(String type) {
            // When
            var thrown = catchThrowable(() -> new DecodedJwt(encodedToken, type, subject, claims));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Type must not be blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenSubjectIsNull(String subject) {
            // When
            var thrown = catchThrowable(() -> new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, claims));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Subject must not be blank");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsNull() {
            // When
            var thrown = catchThrowable(() -> new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Claims must not be empty");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsEmpty() {
            // When
            var thrown = catchThrowable(() -> new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, Map.of()));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Claims must not be empty");
        }

        @Test
        void ThenCreatesDecodedJwt_GivenAllParametersAreValid() {
            // When
            var actual = new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, claims);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(ACCESS_TOKEN_TYPE);
            assertThat(actual.subject()).isEqualTo(subject);
            assertThat(actual.claims()).isEqualTo(claims);
        }

    }

    @Nested
    class CheckIsAccessToken {

        @Test
        void ThenReturnsFalse_GivenTypeIsNotAccessToken() {
            // Given
            var decodedJwt = new DecodedJwt(encodedToken, REFRESH_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.isAccessToken();

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsFalse_GivenTokenUseClaimIsNotAccess() {
            // Given
            var refreshTokenClaims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, roleClaim);
            var decodedJwt = new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, refreshTokenClaims);

            // When
            var actual = decodedJwt.isAccessToken();

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsFalse_GivenTypeIsAccessTokenButTokenUseClaimIsRefresh() {
            // Given
            var refreshTokenClaims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, roleClaim);
            var decodedJwt = new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, refreshTokenClaims);

            // When
            var actual = decodedJwt.isAccessToken();

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsTrue_GivenTypeAndTokenUseClaimAreAccessToken() {
            // Given
            var decodedJwt = new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.isAccessToken();

            // Then
            assertThat(actual).isTrue();
        }

    }

    @Nested
    class GetRoleClaim {

        @Test
        void ThenReturnsNull_GivenRoleClaimIsNotPresent() {
            // Given
            var claimsWithoutRole = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE);
            var decodedJwt = new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, claimsWithoutRole);

            // When
            var actual = decodedJwt.roleClaim();

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ThenReturnsNull_GivenRoleClaimIsNotString() {
            // Given
            var claimsWithNonStringRole = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, 123);
            var decodedJwt = new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, claimsWithNonStringRole);

            // When
            var actual = decodedJwt.roleClaim();

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ThenReturnsRoleClaim_GivenRoleClaimIsPresent() {
            // Given
            var decodedJwt = new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.roleClaim();

            // Then
            assertThat(actual).isEqualTo(roleClaim);
        }

    }

}
