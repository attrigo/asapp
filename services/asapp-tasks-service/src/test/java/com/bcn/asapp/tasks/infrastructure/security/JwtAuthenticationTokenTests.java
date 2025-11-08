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
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ROLE_CLAIM_NAME;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.tasks.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class JwtAuthenticationTokenTests {

    private final String principal = "user@asapp.com";

    private final String role = "USER";

    private final String token = defaultTestEncodedAccessToken();

    @Nested
    class CreateJwtAuthenticatedToken {

        @Test
        void ThenThrowsIllegalArgumentException_GivenJwtIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthenticationToken.authenticated(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Decoded token must not be null");
        }

        @Test
        void ThenCreatesAuthenticatedToken_GivenParametersAreValid() {
            // Given
            var claims = Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, role);
            var decodedToken = new DecodedToken(token, ACCESS_TOKEN_TYPE, principal, claims);

            // When
            var actual = JwtAuthenticationToken.authenticated(decodedToken);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.isAuthenticated()).isTrue();
            assertThat(actual.getToken()).isEqualTo(token);
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
            var claims = Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, role);
            var decodedToken = new DecodedToken(token, ACCESS_TOKEN_TYPE, principal, claims);
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedToken);

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
            var claims = Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, role);
            var decodedToken = new DecodedToken(token, ACCESS_TOKEN_TYPE, principal, claims);
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedToken);

            // When
            var actual = jwtAuthenticationToken.getPrincipal();

            // Then
            assertThat(actual).isEqualTo(principal);
        }

    }

    @Nested
    class GetToken {

        @Test
        void ThenReturnsToken_GivenJwtIsValid() {
            // Given
            var claims = Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, role);
            var decodedToken = new DecodedToken(token, ACCESS_TOKEN_TYPE, principal, claims);
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedToken);

            // When
            var actual = jwtAuthenticationToken.getToken();

            // Then
            assertThat(actual).isEqualTo(token);
        }

    }

}
