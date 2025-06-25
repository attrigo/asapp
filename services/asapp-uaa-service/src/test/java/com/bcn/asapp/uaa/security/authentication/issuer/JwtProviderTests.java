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
package com.bcn.asapp.uaa.security.authentication.issuer;

import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtExpiresAt;
import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtIssuedAt;
import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtRole;
import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtType;
import static com.bcn.asapp.uaa.testutil.JwtAssertions.assertJwtUsername;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.security.core.Role;

class JwtProviderTests {

    private static final String UT_JWT_SECRET = "Cnpr50yQ04Q5y7GFUvR3ODWLYRlPjeAgOy7Y0Woo6PCqiViiOxxS3vo1FOyjro7T";

    private static final Long UT_JWT_EXPIRATION_TIME = 300000L;

    private JwtProvider jwtProvider;

    private String fakeUsername;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.jwtProvider = new JwtProvider(UT_JWT_SECRET, UT_JWT_EXPIRATION_TIME, UT_JWT_EXPIRATION_TIME);

        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
    }

    @Nested
    class GenerateAccessToken {

        @Test
        @DisplayName("GIVEN authentication is null WHEN generate access token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationIsNull_GenerateAccessToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            Executable executable = () -> jwtProvider.generateAccessToken(null);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication must not be null", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication username is null WHEN generate access token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationUserNameIsNull_GenerateAccessToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            var authenticationWithoutUsername = new UsernamePasswordAuthenticationToken(null, fakePassword);

            Executable executable = () -> jwtProvider.generateAccessToken(authenticationWithoutUsername);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication username must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication username is empty WHEN generate access token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationUserNameIsEmpty_GenerateAccessToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            var authenticationWithEmptyUsername = new UsernamePasswordAuthenticationToken("", fakePassword);

            Executable executable = () -> jwtProvider.generateAccessToken(authenticationWithEmptyUsername);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication username must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication authorities is null WHEN generate access token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationAuthoritiesIsNull_GenerateAccessToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            var authenticationWithoutAuthorities = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, null);

            Executable executable = () -> jwtProvider.generateAccessToken(authenticationWithoutAuthorities);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication authorities must not be empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication authorities is empty WHEN generate access token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationAuthoritiesIsEmpty_GenerateAccessToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            var authenticationWithEmptyAuthorities = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of());

            Executable executable = () -> jwtProvider.generateAccessToken(authenticationWithEmptyAuthorities);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication authorities must not be empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication has invalid role WHEN generate access token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationHasInvalidRole_GenerateAccessToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            var invalidAuthorities = List.of(new SimpleGrantedAuthority("NOT_VALID_ROLE"));
            var authenticationWithInvalidAuthorities = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, invalidAuthorities);

            Executable executable = () -> jwtProvider.generateAccessToken(authenticationWithInvalidAuthorities);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication authority is not valid", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication with USER role WHEN generate access token THEN generates the token And return the token")
        void AuthenticationWithUserRole_GenerateAccessToken_GeneratesTokenAndReturnsToken() {
            // When
            var authorities = List.of(new SimpleGrantedAuthority(Role.USER.name()));
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, authorities);

            var actualAccessToken = jwtProvider.generateAccessToken(authentication);

            // Then
            assertNotNull(actualAccessToken);
            assertNull(actualAccessToken.id());
            assertNull(actualAccessToken.userId());
            assertNotNull(actualAccessToken.createdAt());
            assertNotNull(actualAccessToken.expiresAt());
            assertJwtType(actualAccessToken.jwt(), JwtType.ACCESS_TOKEN);
            assertJwtUsername(actualAccessToken.jwt(), fakeUsername);
            assertJwtRole(actualAccessToken.jwt(), Role.USER);
            assertJwtIssuedAt(actualAccessToken.jwt());
            assertJwtExpiresAt(actualAccessToken.jwt());
        }

        @Test
        @DisplayName("GIVEN authentication with ADMIN role WHEN generate access token THEN generates the token And return the token")
        void AuthenticationWithAdminRole_GenerateAccessToken_GeneratesTokenAndReturnsToken() {
            // When
            var authorities = List.of(new SimpleGrantedAuthority(Role.ADMIN.name()));
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, authorities);

            var actualAccessToken = jwtProvider.generateAccessToken(authentication);

            // Then
            assertNotNull(actualAccessToken);
            assertNull(actualAccessToken.id());
            assertNull(actualAccessToken.userId());
            assertNotNull(actualAccessToken.createdAt());
            assertNotNull(actualAccessToken.expiresAt());
            assertJwtType(actualAccessToken.jwt(), JwtType.ACCESS_TOKEN);
            assertJwtUsername(actualAccessToken.jwt(), fakeUsername);
            assertJwtRole(actualAccessToken.jwt(), Role.ADMIN);
            assertJwtIssuedAt(actualAccessToken.jwt());
            assertJwtExpiresAt(actualAccessToken.jwt());
        }

    }

    @Nested
    class GenerateRefreshToken {

        @Test
        @DisplayName("GIVEN authentication is null WHEN generate refresh token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationIsNull_GenerateRefreshToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            Executable executable = () -> jwtProvider.generateRefreshToken(null);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication must not be null", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication username is null WHEN generate refresh token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationUserNameIsNull_GenerateRefreshToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            var authenticationWithoutUsername = new UsernamePasswordAuthenticationToken(null, fakePassword);

            Executable executable = () -> jwtProvider.generateRefreshToken(authenticationWithoutUsername);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication username must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication username is empty WHEN generate refresh token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationUserNameIsEmpty_GenerateRefreshToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            var authenticationWithEmptyUsername = new UsernamePasswordAuthenticationToken("", fakePassword);

            Executable executable = () -> jwtProvider.generateRefreshToken(authenticationWithEmptyUsername);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication username must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication authorities is null WHEN generate refresh token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationAuthoritiesIsNull_GenerateRefreshToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            var authenticationWithoutAuthorities = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, null);

            Executable executable = () -> jwtProvider.generateRefreshToken(authenticationWithoutAuthorities);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication authorities must not be empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication authorities is empty WHEN generate refresh token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationAuthoritiesIsEmpty_GenerateRefreshToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            var authenticationWithEmptyAuthorities = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of());

            Executable executable = () -> jwtProvider.generateRefreshToken(authenticationWithEmptyAuthorities);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication authorities must not be empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication has invalid role WHEN generate refresh token THEN does not generate the token And throws IllegalArgumentException")
        void AuthenticationHasInvalidRole_GenerateRefreshToken_DoesNotGenerateTokenAndThrowsIllegalArgumentException() {
            // When
            var invalidAuthorities = List.of(new SimpleGrantedAuthority("NOT_VALID_ROLE"));
            var authenticationWithInvalidAuthorities = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, invalidAuthorities);

            Executable executable = () -> jwtProvider.generateRefreshToken(authenticationWithInvalidAuthorities);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Authentication authority is not valid", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN authentication with USER role WHEN generate refresh token THEN generates And return the refresh token")
        void AuthenticationWithUserRole_GenerateRefreshToken_GeneratesAndReturnsRefreshToken() {
            // When
            var authorities = List.of(new SimpleGrantedAuthority(Role.USER.name()));
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, authorities);

            var actualRefreshToken = jwtProvider.generateRefreshToken(authentication);

            // Then
            assertNotNull(actualRefreshToken);
            assertNull(actualRefreshToken.id());
            assertNull(actualRefreshToken.userId());
            assertNotNull(actualRefreshToken.createdAt());
            assertNotNull(actualRefreshToken.expiresAt());
            assertJwtType(actualRefreshToken.jwt(), JwtType.REFRESH_TOKEN);
            assertJwtUsername(actualRefreshToken.jwt(), fakeUsername);
            assertJwtRole(actualRefreshToken.jwt(), Role.USER);
            assertJwtIssuedAt(actualRefreshToken.jwt());
            assertJwtExpiresAt(actualRefreshToken.jwt());
        }

        @Test
        @DisplayName("GIVEN authentication with ADMIN role WHEN generate refresh token THEN generates And return the refresh token")
        void AuthenticationWithAdminRole_GenerateRefreshToken_GeneratesAndReturnsRefreshToken() {
            // When
            var authorities = List.of(new SimpleGrantedAuthority(Role.ADMIN.name()));
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, authorities);

            var actualRefreshToken = jwtProvider.generateRefreshToken(authentication);

            // Then
            assertNotNull(actualRefreshToken);
            assertNull(actualRefreshToken.id());
            assertNull(actualRefreshToken.userId());
            assertNotNull(actualRefreshToken.createdAt());
            assertNotNull(actualRefreshToken.expiresAt());
            assertJwtType(actualRefreshToken.jwt(), JwtType.REFRESH_TOKEN);
            assertJwtUsername(actualRefreshToken.jwt(), fakeUsername);
            assertJwtRole(actualRefreshToken.jwt(), Role.ADMIN);
            assertJwtIssuedAt(actualRefreshToken.jwt());
            assertJwtExpiresAt(actualRefreshToken.jwt());
        }

    }

}
