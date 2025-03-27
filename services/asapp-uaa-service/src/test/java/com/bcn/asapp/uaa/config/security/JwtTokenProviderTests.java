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
package com.bcn.asapp.uaa.config.security;

import static com.bcn.asapp.uaa.util.JwtAssertions.assertJwtAuthorities;
import static com.bcn.asapp.uaa.util.JwtAssertions.assertJwtUsername;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.bcn.asapp.uaa.auth.Role;

class JwtTokenProviderTests {

    private static final String UT_JWT_SECRET = "Cnpr50yQ04Q5y7GFUvR3ODWLYRlPjeAgOy7Y0Woo6PCqiViiOxxS3vo1FOyjro7T";

    private static final Long UT_JWT_EXPIRATION_TIME = 3600000L;

    private JwtTokenProvider jwtTokenProvider;

    private String fakeUsername;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.jwtTokenProvider = new JwtTokenProvider(UT_JWT_SECRET, UT_JWT_EXPIRATION_TIME);

        this.fakeUsername = "UT username";
        this.fakePassword = "UT password";
    }

    @Nested
    class GenerateToken {

        @Test
        @DisplayName("GIVEN authentication is null WHEN generate token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationIsNull_GenerateToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When & Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.generateToken(null));

            assertEquals("Authentication must not be null", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication username is null WHEN generate token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationUserNameIsNull_GenerateToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When & Then
            var authenticationWithoutUsername = new UsernamePasswordAuthenticationToken(null, fakePassword);

            var exceptionThrown = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.generateToken(authenticationWithoutUsername));

            assertEquals("Authentication name must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication username is empty WHEN generate token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationUserNameIsEmpty_GenerateToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When & Then
            var authenticationWithEmptyUsername = new UsernamePasswordAuthenticationToken("", fakePassword);

            var exceptionThrown = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.generateToken(authenticationWithEmptyUsername));

            assertEquals("Authentication name must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication authorities is null WHEN generate token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationAuthoritiesIsNull_GenerateToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When & Then
            var authenticationWithoutAuthorities = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, null);

            var exceptionThrown = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.generateToken(authenticationWithoutAuthorities));

            assertEquals("Authentication authorities must not be empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication authorities is empty WHEN generate token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationAuthoritiesIsEmpty_GenerateToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When & Then
            var authenticationWithEmptyAuthorities = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of());

            var exceptionThrown = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.generateToken(authenticationWithEmptyAuthorities));

            assertEquals("Authentication authorities must not be empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication has invalid role WHEN generate token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationHasInvalidRole_GenerateToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When & Then
            var invalidAuthorities = List.of(new SimpleGrantedAuthority("NOT_VALID_ROLE"));
            var authenticationWithInvalidAuthorities = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, invalidAuthorities);

            var exceptionThrown = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.generateToken(authenticationWithInvalidAuthorities));

            assertEquals("Authentication authority is not valid", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication with USER role WHEN generate token THEN generates the token And return the token")
        void AuthenticationWithUserRole_GenerateToken_GeneratesTokenAndReturnsToken() {
            // When
            var authorities = List.of(new SimpleGrantedAuthority(Role.USER.name()));
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, authorities);

            var jwt = jwtTokenProvider.generateToken(authentication);

            // Then
            assertNotNull(jwt);
            assertJwtUsername(jwt, fakeUsername);
            assertJwtAuthorities(jwt, Role.USER);
        }

        @Test
        @DisplayName("GIVEN authentication with ADMIN role WHEN generate token THEN generates the token And return the token")
        void AuthenticationWithAdminRole_GenerateToken_GeneratesTokenAndReturnsToken() {
            // When
            var authorities = List.of(new SimpleGrantedAuthority(Role.ADMIN.name()));
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, authorities);

            var jwt = jwtTokenProvider.generateToken(authentication);

            // Then
            assertNotNull(jwt);
            assertJwtUsername(jwt, fakeUsername);
            assertJwtAuthorities(jwt, Role.ADMIN);
        }

    }

}
