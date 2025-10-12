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

import static com.bcn.asapp.authentication.domain.authentication.Jwt.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.Subject;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationTokenTests {

    private final EncodedToken encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.encoded");

    private final Subject subject = Subject.of("user@asapp.com");

    private final JwtClaims claims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, USER.name()));

    private final Issued issued = Issued.now();

    private final Expiration expiration = Expiration.of(issued, 1000L);

    private final Jwt jwt = Jwt.of(encodedToken, ACCESS_TOKEN, subject, claims, issued, expiration);

    @Nested
    class CreateAuthenticatedToken {

        @Test
        void ThenThrowsIllegalArgumentException_GivenJwtIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthenticationToken.authenticated(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT must not be null");
        }

        @Test
        void ThenCreatesAuthenticatedTokenWithoutAuthorities_GivenJwtNotContainsRoleClaim() {
            // Given
            var claimsWithoutRole = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE));
            var jwtWithoutRoleClaim = Jwt.of(encodedToken, ACCESS_TOKEN, subject, claimsWithoutRole, issued, expiration);

            // When
            var actual = JwtAuthenticationToken.authenticated(jwtWithoutRoleClaim);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.isAuthenticated()).isTrue();
            assertThat(actual.getJwt()).isEqualTo(encodedToken.value());
            assertThat(actual.getPrincipal()).isEqualTo(subject);
            assertThat(actual.getCredentials()).isNull();
            assertThat(actual.getAuthorities()).isEmpty();
        }

        @Test
        void ThenCreatesAuthenticatedToken_GivenParametersAreValid() {
            // When
            var actual = JwtAuthenticationToken.authenticated(jwt);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.isAuthenticated()).isTrue();
            assertThat(actual.getJwt()).isEqualTo(encodedToken.value());
            assertThat(actual.getPrincipal()).isEqualTo(subject);
            assertThat(actual.getCredentials()).isNull();
            assertThat(actual.getAuthorities()).hasSize(1)
                                               .extracting(GrantedAuthority::getAuthority)
                                               .containsExactly(USER.name());
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
    class GetJwt {

        @Test
        void ThenReturnsJwt_GivenJwtIsValid() {
            // Given
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(jwt);

            // When
            var actual = jwtAuthenticationToken.getJwt();

            // Then
            assertThat(actual).isEqualTo(encodedToken.value());
        }

    }

}
