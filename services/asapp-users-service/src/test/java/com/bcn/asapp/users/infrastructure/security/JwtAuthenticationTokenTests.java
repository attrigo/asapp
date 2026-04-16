/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.users.infrastructure.security;

import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ROLE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * Tests {@link JwtAuthenticationToken} construction from decoded JWT with authority mapping.
 * <p>
 * Coverage:
 * <li>Rejects null decoded JWT at construction</li>
 * <li>Constructs authenticated token without authorities when role claim missing</li>
 * <li>Constructs authenticated token with principal, authorities, and encoded token</li>
 * <li>Provides access to principal (subject), credentials (null), and JWT string</li>
 */
class JwtAuthenticationTokenTests {

    @Nested
    class CreateJwtAuthenticatedToken {

        @Test
        void ReturnsJwtAuthenticatedToken_ValidParameters() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var principal = "user@asapp.com";
            var role = "USER";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, principal, claims);

            // When
            var actual = JwtAuthenticationToken.authenticated(decodedJwt);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("JWT authentication token").isNotNull();
                softly.assertThat(actual.isAuthenticated()).as("authenticated").isTrue();
                softly.assertThat(actual.getJwt()).as("JWT").isEqualTo(encodedAccessToken);
                softly.assertThat(actual.getPrincipal()).as("principal").isEqualTo(principal);
                softly.assertThat(actual.getCredentials()).as("credentials").isNull();
                softly.assertThat(actual.getAuthorities()).as("authorities").hasSize(1).extracting(GrantedAuthority::getAuthority).containsExactly(role);
                // @formatter:on
            });
        }

        @Test
        void ReturnsJwtAuthenticatedTokenWithoutAuthorities_MissingRoleClaim() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var principal = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE);
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, principal, claims);

            // When
            var actual = JwtAuthenticationToken.authenticated(decodedJwt);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("JWT authentication token").isNotNull();
                softly.assertThat(actual.isAuthenticated()).as("authenticated").isTrue();
                softly.assertThat(actual.getJwt()).as("JWT").isEqualTo(encodedAccessToken);
                softly.assertThat(actual.getPrincipal()).as("principal").isEqualTo(principal);
                softly.assertThat(actual.getCredentials()).as("credentials").isNull();
                softly.assertThat(actual.getAuthorities()).as("authorities").isEmpty();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullJwt() {
            // When
            var actual = catchThrowable(() -> JwtAuthenticationToken.authenticated(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Decoded JWT must not be null");
        }

    }

    @Nested
    class GetCredentials {

        @Test
        void ReturnsNull_ValidJwt() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var principal = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, principal, claims);
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
        void ReturnsPrincipalAsSubject_ValidJwt() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var principal = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, principal, claims);
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
        void ReturnsJwt_ValidJwt() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, "user@asapp.com", claims);
            var jwtAuthenticationToken = JwtAuthenticationToken.authenticated(decodedJwt);

            // When
            var actual = jwtAuthenticationToken.getJwt();

            // Then
            assertThat(actual).isEqualTo(encodedAccessToken);
        }

    }

}
