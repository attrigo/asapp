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
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.ROLE_CLAIM_NAME;
import static com.bcn.asapp.users.infrastructure.security.DecodedToken.TOKEN_USE_CLAIM_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class JwtAuthenticationTokenTests {

    private final String encodedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.encoded";

    private final String type = ACCESS_TOKEN_TYPE;

    private final String subject = "user@asapp.com";

    private final Map<String, String> claims = Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, "USER");

    private final DecodedToken jwt = new DecodedToken(encodedToken, type, subject, claims);

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
            // When
            var actual = JwtAuthenticationToken.authenticated(jwt);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.isAuthenticated()).isTrue();
            assertThat(actual.getToken()).isEqualTo(encodedToken);
            assertThat(actual.getPrincipal()).isEqualTo(subject);
            assertThat(actual.getCredentials()).isNull();
            assertThat(actual.getAuthorities()).hasSize(1)
                                               .extracting(GrantedAuthority::getAuthority)
                                               .containsExactly("USER");
        }

    }

    @Nested
    class GetCredentials {

        @Test
        void ThenReturnsNull_GivenJwtIsValid() {
            // Given
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(jwt);

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
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(jwt);

            // When
            var actual = jwtAuthenticationToken.getPrincipal();

            // Then
            assertThat(actual).isEqualTo(subject);
        }

    }

    @Nested
    class GetToken {

        @Test
        void ThenReturnsToken_GivenJwtIsValid() {
            // Given
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(jwt);

            // When
            var actual = jwtAuthenticationToken.getToken();

            // Then
            assertThat(actual).isEqualTo(encodedToken);
        }

    }

}
