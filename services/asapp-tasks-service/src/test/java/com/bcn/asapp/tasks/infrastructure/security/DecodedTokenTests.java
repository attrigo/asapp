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

package com.bcn.asapp.tasks.infrastructure.security;

import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ROLE_CLAIM_NAME;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.TOKEN_USE_CLAIM_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class DecodedTokenTests {

    private String encodedToken;

    private String type;

    private String subject;

    private Map<String, String> claims;

    @BeforeEach
    void beforeEach() {
        encodedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.encoded";
        type = ACCESS_TOKEN_TYPE;
        subject = "user@asapp.com";
        claims = Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, "USER");
    }

    @Nested
    class CreateDecodedToken {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenEncodedTokenIsNull(String encodedToken) {
            // When
            var thrown = catchThrowable(() -> new DecodedToken(encodedToken, type, subject, claims));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Encoded token must not be blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenTypeIsNull(String type) {
            // When
            var thrown = catchThrowable(() -> new DecodedToken(encodedToken, type, subject, claims));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Type must not be blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenSubjectIsNull(String subject) {
            // When
            var thrown = catchThrowable(() -> new DecodedToken(encodedToken, type, subject, claims));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Subject must not be blank");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsNull() {
            // When
            var thrown = catchThrowable(() -> new DecodedToken(encodedToken, type, subject, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Claim must not be empty");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsEmpty() {
            // When
            var thrown = catchThrowable(() -> new DecodedToken(encodedToken, type, subject, Map.of()));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Claim must not be empty");
        }

        @Test
        void ThenCreatesDecodedToken_GivenAllParametersAreValid() {
            // When
            var result = new DecodedToken(encodedToken, type, subject, claims);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.encodedToken()).isEqualTo(encodedToken);
            assertThat(result.type()).isEqualTo(type);
            assertThat(result.subject()).isEqualTo(subject);
            assertThat(result.claims()).isEqualTo(claims);
        }

    }

    @Nested
    class CheckIsAccessToken {

        @Test
        void ThenReturnsFalse_GivenTypeIsNotAccessToken() {
            // Given
            var decodedToken = new DecodedToken(encodedToken, REFRESH_TOKEN_TYPE, subject, claims);

            // When
            var result = decodedToken.isAccessToken();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void ThenReturnsFalse_GivenTokenUseClaimIsNotAccess() {
            // Given
            var refreshClaims = Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, "USER");
            var decodedToken = new DecodedToken(encodedToken, type, subject, refreshClaims);

            // When
            var result = decodedToken.isAccessToken();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void ThenReturnsFalse_GivenTypeIsAccessTokenButTokenUseClaimIsRefresh() {
            // Given
            var mixedClaims = Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, "USER");
            var decodedToken = new DecodedToken(encodedToken, ACCESS_TOKEN_TYPE, subject, mixedClaims);

            // When
            var result = decodedToken.isAccessToken();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void ThenReturnsTrue_GivenTypeAndTokenUseClaimAreAccessToken() {
            // Given
            var decodedToken = new DecodedToken(encodedToken, ACCESS_TOKEN_TYPE, subject, claims);

            // When
            var result = decodedToken.isAccessToken();

            // Then
            assertThat(result).isTrue();
        }

    }

    @Nested
    class GetRoleClaim {

        @Test
        void ThenReturnsNull_GivenRoleClaimIsNotPresent() {
            // Given
            var claimsWithoutRole = Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE);
            var decodedToken = new DecodedToken(encodedToken, type, subject, claimsWithoutRole);

            // When
            var result = decodedToken.roleClaim();

            // Then
            assertThat(result).isNull();
        }

        @Test
        void ThenReturnsRoleClaim_GivenRoleClaimIsPresent() {
            // Given
            var decodedToken = new DecodedToken(encodedToken, type, subject, claims);

            // When
            var result = decodedToken.roleClaim();

            // Then
            assertThat(result).isEqualTo("USER");
        }

    }

}
