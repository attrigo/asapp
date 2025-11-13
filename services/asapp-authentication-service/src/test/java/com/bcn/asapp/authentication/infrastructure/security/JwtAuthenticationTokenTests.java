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
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class JwtAuthenticationTokenTests {

    private final String principal = "user@asapp.com";

    private final String role = USER.name();

    private final String token = defaultTestEncodedAccessToken();

    private DecodedJwt decodedJwt;

    @BeforeEach
    void beforeEach() {
        var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, role);
        decodedJwt = new DecodedJwt(token, ACCESS_TOKEN_TYPE, principal, claims);
    }

    @Nested
    class CreateJwtAuthenticatedToken {

        @Test
        void ThenThrowsIllegalArgumentException_GivenJwtIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthenticationToken.authenticated(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Decoded JWT must not be null");
        }

        @Test
        void ThenCreatesJwtAuthenticatedTokenWithoutAuthorities_GivenJwtNotContainsRoleClaim() {
            // Given
            var claimsWithoutRole = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE);
            var decodedJwtWithoutRoleClaim = new DecodedJwt(token, ACCESS_TOKEN_TYPE, principal, claimsWithoutRole);

            // When
            var actual = JwtAuthenticationToken.authenticated(decodedJwtWithoutRoleClaim);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.isAuthenticated()).isTrue();
            assertThat(actual.getJwt()).isEqualTo(token);
            assertThat(actual.getPrincipal()).isEqualTo(principal);
            assertThat(actual.getCredentials()).isNull();
            assertThat(actual.getAuthorities()).isEmpty();
        }

        @Test
        void ThenCreatesJwtAuthenticatedToken_GivenParametersAreValid() {
            // When
            var actual = JwtAuthenticationToken.authenticated(decodedJwt);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.isAuthenticated()).isTrue();
            assertThat(actual.getJwt()).isEqualTo(token);
            assertThat(actual.getPrincipal()).isEqualTo(principal);
            assertThat(actual.getCredentials()).isNull();
            assertThat(actual.getAuthorities()).hasSize(1)
                                               .extracting(GrantedAuthority::getAuthority)
                                               .containsExactly(role);
        }

    }

    @Nested
    class GetCredentials {

        @Test
        void ThenReturnsNull_GivenJwtIsValid() {
            // Given
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);

            // When
            var actual = jwtAuthenticationToken.getCredentials();

            // Then
            assertThat(actual).isNull();
        }

    }

    @Nested
    class GetPrincipal {

        @Test
        void ThenReturnsPrincipalAsSubject_GivenJwtIsValid() {
            // Given
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);

            // When
            var actual = jwtAuthenticationToken.getPrincipal();

            // Then
            assertThat(actual).isEqualTo(principal);
        }

    }

    @Nested
    class GetJwt {

        @Test
        void ThenReturnsJwt_GivenJwtIsValid() {
            // Given
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);

            // When
            var actual = jwtAuthenticationToken.getJwt();

            // Then
            assertThat(actual).isEqualTo(token);
        }

    }

}
