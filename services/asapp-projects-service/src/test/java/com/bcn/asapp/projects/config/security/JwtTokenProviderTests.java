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

package com.bcn.asapp.projects.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

import com.bcn.asapp.projects.testutil.JwtTestGenerator;

class JwtTokenProviderTests {

    private static final String UT_JWT_SECRET = "Cnpr50yQ04Q5y7GFUvR3ODWLYRlPjeAgOy7Y0Woo6PCqiViiOxxS3vo1FOyjro7T";

    private static final Long UT_JWT_EXPIRATION_TIME = 3600000L;

    private JwtTokenProvider jwtTokenProvider;

    private JwtTestGenerator jwtTestGenerator;

    private String fakeUsername;

    @BeforeEach
    void beforeEach() {
        this.jwtTokenProvider = new JwtTokenProvider(UT_JWT_SECRET);

        this.jwtTestGenerator = new JwtTestGenerator(UT_JWT_SECRET, UT_JWT_EXPIRATION_TIME);

        this.fakeUsername = "TEST USERNAME";
    }

    @Nested
    class GetUsername {

        @Test
        @DisplayName("GIVEN token is null WHEN get username from token THEN does not get the username And throws IllegalArgumentException")
        void TokenIsNull_GetUsername_DoesNotGetUsernameAndThrowsIllegalArgumentException() {
            // When & Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.getUsername(null));

            assertEquals("Token must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN token is empty WHEN get username from token THEN does not get the username And throws IllegalArgumentException")
        void TokenIsEmpty_GetUsername_DoesNotGetUsernameAndThrowsIllegalArgumentException() {
            // When & Then
            var emptyToken = "";

            var exceptionThrown = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.getUsername(emptyToken));

            assertEquals("Token must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN token is invalid WHEN get username from token THEN does not get the username And throws MalformedJwtException")
        void TokenIsInvalid_GetUsername_DoesNotGetUsernameAndThrowsMalformedJwtException() {
            // When & Then
            var invalidToken = "INVALID_TOKEN";

            assertThrows(MalformedJwtException.class, () -> jwtTokenProvider.getUsername(invalidToken));
        }

        @Test
        @DisplayName("GIVEN token signature is invalid WHEN get username from token THEN does not get the username And throws SignatureException")
        void TokenSignatureIsInvalid_GetUsername_DoesNotGetUsernameAndThrowsSignatureException() {
            // When & Then
            var tokenSignatureInvalid = jwtTestGenerator.generateJwtWithInvalidSignature();

            assertThrows(SignatureException.class, () -> jwtTokenProvider.getUsername(tokenSignatureInvalid));
        }

        @Test
        @DisplayName("GIVEN token has expired WHEN get username from token THEN does not get the username And throws ExpiredJwtException")
        void TokenHasExpired_GetUsername_DoesNotGetUsernameAndThrowsExpiredJwtException() {
            // When & Then
            var tokenExpired = jwtTestGenerator.generateJwtExpired();

            assertThrows(ExpiredJwtException.class, () -> jwtTokenProvider.getUsername(tokenExpired));
        }

        @Test
        @DisplayName("GIVEN token has not been protected WHEN get username from token THEN does not get the username And throws UnsupportedJwtException")
        void TokenIsNotSigned_GetUsername_DoesNotGetUsernameAndThrowsUnsupportedJwtException() {
            // When & Then
            var tokenNotSigned = jwtTestGenerator.generateJwtNotSigned();

            assertThrows(UnsupportedJwtException.class, () -> jwtTokenProvider.getUsername(tokenNotSigned));
        }

        @Test
        @DisplayName("GIVEN token does not have username WHEN get username from token THEN does not get the username And returns empty")
        void TokenHasNotUsername_GetUsername_DoesNotGetUsernameAndReturnsEmpty() {
            // When
            var tokenWithoutUsername = jwtTestGenerator.generateJwtWithoutUsername();

            var actualUsername = jwtTokenProvider.getUsername(tokenWithoutUsername);

            // Then
            assertFalse(actualUsername.isPresent());
        }

        @Test
        @DisplayName("GIVEN token is valid And has username WHEN get username from token THEN gets the username And returns the username")
        void TokenHasUsername_GetUsername_GetsUsernameAndReturnUsername() {
            // When
            var tokenWithUsername = jwtTestGenerator.generateJwt();

            var actualUsername = jwtTokenProvider.getUsername(tokenWithUsername);

            // Then
            assertTrue(actualUsername.isPresent());
            assertEquals(fakeUsername, actualUsername.get());
        }

    }

    @Nested
    class GetAuthorities {

        @Test
        @DisplayName("GIVEN token is null WHEN get authorities from token THEN does not get the authorities And throws IllegalArgumentException")
        void TokenIsNull_GetAuthorities_DoesNotGetAuthoritiesAndThrowsIllegalArgumentException() {
            // When & Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.getAuthorities(null));

            assertEquals("Token must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN token is empty WHEN get authorities from token THEN does not get the authorities And throws IllegalArgumentException")
        void TokenIsEmpty_GetAuthorities_DoesNotGetAuthoritiesAndThrowsIllegalArgumentException() {
            // When & Then
            var emptyToken = "";

            var exceptionThrown = assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.getAuthorities(emptyToken));

            assertEquals("Token must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN token is invalid WHEN get authorities from token THEN does not get the authorities And throws MalformedJwtException")
        void TokenIsInvalid_GetAuthorities_DoesNotGetAuthoritiesAndThrowsMalformedJwtException() {
            // When & Then
            var invalidToken = "INVALID_TOKEN";

            assertThrows(MalformedJwtException.class, () -> jwtTokenProvider.getAuthorities(invalidToken));
        }

        @Test
        @DisplayName("GIVEN token signature is invalid WHEN get authorities from token THEN does not get the authorities And throws SignatureException")
        void TokenSignatureIsInvalid_GetAuthorities_DoesNotGetAuthoritiesAndThrowsSignatureException() {
            // When & Then
            var tokenSignatureInvalid = jwtTestGenerator.generateJwtWithInvalidSignature();

            assertThrows(SignatureException.class, () -> jwtTokenProvider.getAuthorities(tokenSignatureInvalid));
        }

        @Test
        @DisplayName("GIVEN token has expired WHEN get authorities from token THEN does not get the authorities And throws ExpiredJwtException")
        void TokenHasExpired_GetAuthorities_DoesNotGetAuthoritiesAndThrowsExpiredJwtException() {
            // When & Then
            var tokenExpired = jwtTestGenerator.generateJwtExpired();

            assertThrows(ExpiredJwtException.class, () -> jwtTokenProvider.getAuthorities(tokenExpired));
        }

        @Test
        @DisplayName("GIVEN token has not been protected WHEN get authorities from token THEN does not get the authorities And throws UnsupportedJwtException")
        void TokenIsNotSigned_GetAuthorities_DoesNotGetAuthoritiesAndThrowsUnsupportedJwtException() {
            // When & Then
            var tokenNotSigned = jwtTestGenerator.generateJwtNotSigned();

            assertThrows(UnsupportedJwtException.class, () -> jwtTokenProvider.getAuthorities(tokenNotSigned));
        }

        @Test
        @DisplayName("GIVEN token does not have authorities WHEN get authorities from token THEN does not get the authorities And returns empty list")
        void TokenHasNotAuthorities_GetAuthorities_DoesNotGetAuthoritiesAndReturnsEmpty() {
            // When
            var tokenWithoutAuthorities = jwtTestGenerator.generateJwtWithoutAuthorities();

            var actualAuthorities = jwtTokenProvider.getAuthorities(tokenWithoutAuthorities);

            // Then
            assertTrue(actualAuthorities.isEmpty());
        }

        @Test
        @DisplayName("GIVEN token has authorities WHEN get authorities from token THEN gets the authorities And returns the authorities")
        void TokenHasAuthorities_GetAuthorities_GetAuthoritiesAndReturnsAuthorities() {
            // When
            var tokenWithAuthorities = jwtTestGenerator.generateJwt();

            var actualAuthorities = jwtTokenProvider.getAuthorities(tokenWithAuthorities);

            // Then
            assertFalse(actualAuthorities.isEmpty());
            assertEquals(1, actualAuthorities.size());
            assertEquals("USER", actualAuthorities.getFirst());
        }

    }

    @Nested
    class ValidateToken {

        @Test
        @DisplayName("GIVEN token is null WHEN validate token THEN returns false")
        void TokenIsNull_ValidateToken_ReturnsFalse() {
            // When
            var actualValidation = jwtTokenProvider.validateToken(null);

            // Then
            assertFalse(actualValidation);
        }

        @Test
        @DisplayName("GIVEN token is empty WHEN validate token THEN returns false")
        void TokenIsEmpty_ValidateToken_ReturnsFalse() {
            // When
            var emptyToken = "";

            var actualValidation = jwtTokenProvider.validateToken(emptyToken);

            // Then
            assertFalse(actualValidation);
        }

        @Test
        @DisplayName("GIVEN token is invalid WHEN validate token THEN returns false")
        void TokenIsInvalid_ValidateToken_ReturnsFalse() {
            // When
            var invalidToken = "INVALID_TOKEN";

            var actualValidation = jwtTokenProvider.validateToken(invalidToken);

            // Then
            assertFalse(actualValidation);
        }

        @Test
        @DisplayName("GIVEN token signature is invalid WHEN validate token THEN returns false")
        void TokenSignatureIsInvalid_ValidateToken_ReturnsFalse() {
            // When
            var tokenSignatureInvalid = jwtTestGenerator.generateJwtWithInvalidSignature();

            var actualValidation = jwtTokenProvider.validateToken(tokenSignatureInvalid);

            // Then
            assertFalse(actualValidation);
        }

        @Test
        @DisplayName("GIVEN token has expired WHEN validate token THEN returns false")
        void TokenHasExpired_ValidateToken_ReturnsFalse() {
            // When
            var tokenExpired = jwtTestGenerator.generateJwtExpired();

            var actualValidation = jwtTokenProvider.validateToken(tokenExpired);

            // Then
            assertFalse(actualValidation);
        }

        @Test
        @DisplayName("GIVEN token has not been protected WHEN validate token THEN returns false")
        void TokenIsNotSigned_ValidateToken_ReturnsFalse() {
            // When
            var tokenNotSigned = jwtTestGenerator.generateJwtNotSigned();

            var actualValidation = jwtTokenProvider.validateToken(tokenNotSigned);

            // Then
            assertFalse(actualValidation);
        }

        @Test
        @DisplayName("GIVEN token does not have username WHEN validate token THEN returns true")
        void TokenHasNotUsername_ValidateToken_ReturnsTrue() {
            // When
            var tokenWithoutSubject = jwtTestGenerator.generateJwtWithoutUsername();

            var actualValidation = jwtTokenProvider.validateToken(tokenWithoutSubject);

            // Then
            assertTrue(actualValidation);
        }

        @Test
        @DisplayName("GIVEN token does not have authorities WHEN validate token THEN returns true")
        void TokenHasNotAuthorities_ValidateToken_ReturnsTrue() {
            // When
            var tokenWithoutAuthorities = jwtTestGenerator.generateJwtWithoutAuthorities();

            var actualValidation = jwtTokenProvider.validateToken(tokenWithoutAuthorities);

            // Then
            assertTrue(actualValidation);
        }

        @Test
        @DisplayName("GIVEN token have username and authorities WHEN validate token THEN returns true")
        void TokenHasUsernameAndAuthorities_ValidateToken_ReturnsTrue() {
            // When
            var tokenWithUsernameAndAuthorities = jwtTestGenerator.generateJwt();

            var actualValidation = jwtTokenProvider.validateToken(tokenWithUsernameAndAuthorities);

            // Then
            assertTrue(actualValidation);
        }

    }

}
