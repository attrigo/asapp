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
package com.bcn.asapp.uaa.security.authentication;

import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.uaa.security.core.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.uaa.security.core.JwtType.REFRESH_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.bcn.asapp.uaa.security.core.InvalidJwtException;
import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.testutil.JwtFaker;

class JwtDecoderTests {

    private static final String UT_JWT_SECRET = "Cnpr50yQ04Q5y7GFUvR3ODWLYRlPjeAgOy7Y0Woo6PCqiViiOxxS3vo1FOyjro7T";

    private JwtFaker jwtFaker;

    private JwtDecoder jwtDecoder;

    @BeforeEach
    void beforeEach() {
        this.jwtFaker = new JwtFaker();

        this.jwtDecoder = new JwtDecoder(UT_JWT_SECRET);
    }

    @Nested
    class Decode {

        @Test
        @DisplayName("GIVEN token is null WHEN decode the token THEN throws InvalidJwtException")
        void TokenIsNull_Decode_ThrowsInvalidJwtException() {
            // Given
            Executable executable = () -> jwtDecoder.decode(null);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT claims are null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN token is empty WHEN decode the token THEN throws InvalidJwtException")
        void TokenIsEmpty_Decode_ThrowsInvalidJwtException() {
            // Given
            Executable executable = () -> jwtDecoder.decode("");

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT claims are null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN token is invalid WHEN decode the token THEN throws InvalidJwtException")
        void TokenIsInvalid_Decode_ThrowsInvalidJwtException() {
            // Given
            var invalidJwt = jwtFaker.fakeJwtInvalid();

            Executable executable = () -> jwtDecoder.decode(invalidJwt);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT is malformed", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN token has invalid signature WHEN decode the token THEN throws InvalidJwtException")
        void TokenSignatureIsInvalid_Decode_ThrowsInvalidJwtException() {
            // Given
            var jwtWithInvalidSignature = jwtFaker.fakeJwtWithInvalidSignature(ACCESS_TOKEN);

            Executable executable = () -> jwtDecoder.decode(jwtWithInvalidSignature);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Invalid JWT signature", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN token has expired WHEN decode the token THEN throws InvalidJwtException")
        void TokenHasExpired_Decode_ThrowsInvalidJwtException() {
            // Given
            var expiredJwt = jwtFaker.fakeJwtExpired(ACCESS_TOKEN);

            Executable executable = () -> jwtDecoder.decode(expiredJwt);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT is expired", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN token is not signed WHEN decode the token THEN throws InvalidJwtException")
        void TokenIsNotSigned_Decode_ThrowsInvalidJwtException() {
            // Given
            var notSignedJwt = jwtFaker.fakeJwtNotSigned(ACCESS_TOKEN);

            Executable executable = () -> jwtDecoder.decode(notSignedJwt);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT is not supported", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN token has not type WHEN decode the token THEN throws InvalidJwtException")
        void TokenHasNotType_Decode_ThrowsInvalidJwtException() {
            // Given
            var jwtWithoutType = jwtFaker.fakeJwtWithoutType();

            Executable executable = () -> jwtDecoder.decode(jwtWithoutType);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Error verifying JWT token", exceptionThrown.getMessage());
            assertEquals("Invalid JWT type, expected at+jwt or rt+jwt but was null", exceptionThrown.getCause()
                                                                                                    .getMessage());
        }

        @Test
        @DisplayName("GIVEN token has invalid type WHEN decode the token THEN throws InvalidJwtException")
        void TokenHasInvalidType_Decode_ThrowsInvalidJwtException() {
            // Given
            var jwtWithInvalidType = jwtFaker.fakeJwtWithInvalidType();

            Executable executable = () -> jwtDecoder.decode(jwtWithInvalidType);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Error verifying JWT token", exceptionThrown.getMessage());
            assertEquals("Invalid JWT type, expected at+jwt or rt+jwt but was INVALID TYPE", exceptionThrown.getCause()
                                                                                                            .getMessage());
        }

        @Test
        @DisplayName("GIVEN token has not username WHEN decode the token THEN throws InvalidJwtException")
        void TokenHasNotUsername_Decode_ReturnsTrue() {
            // Given
            var jwtWithoutUsername = jwtFaker.fakeJwtWithoutUsername(JwtType.ACCESS_TOKEN);

            Executable executable = () -> jwtDecoder.decode(jwtWithoutUsername);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Error verifying JWT token", exceptionThrown.getMessage());
            assertEquals("JWT does not contain the mandatory claims", exceptionThrown.getCause()
                                                                                     .getMessage());
        }

        @Test
        @DisplayName("GIVEN token has not authorities WHEN decode the token THEN throws InvalidJwtException")
        void TokenHasNotAuthorities_Decode_ReturnsTrue() {
            // Given
            var jwtWithoutAuthorities = jwtFaker.fakeJwtWithoutAuthorities(JwtType.ACCESS_TOKEN);

            Executable executable = () -> jwtDecoder.decode(jwtWithoutAuthorities);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Error verifying JWT token", exceptionThrown.getMessage());
            assertEquals("JWT does not contain the mandatory claims", exceptionThrown.getCause()
                                                                                     .getMessage());
        }

        @Test
        @DisplayName("GIVEN access token is valid WHEN decode the token THEN returns the DecodedJwt")
        void AccessTokenIsValid_Decode_ReturnsDecodedJwt() {
            // Given
            var accessTokenJwt = jwtFaker.fakeJwt(ACCESS_TOKEN);

            var actualDecodedJwt = jwtDecoder.decode(accessTokenJwt);

            // Then
            assertEquals(accessTokenJwt, actualDecodedJwt.getJwt());
            assertEquals("at+jwt", actualDecodedJwt.getType());
            assertEquals("TEST USERNAME", actualDecodedJwt.getSubject());
            assertTrue(actualDecodedJwt.getClaim(ROLE_CLAIM_NAME, String.class)
                                       .isPresent());
            assertEquals("USER", actualDecodedJwt.getClaim(ROLE_CLAIM_NAME, String.class)
                                                 .get());
            assertTrue(actualDecodedJwt.getClaim(TOKEN_USE_CLAIM_NAME, String.class)
                                       .isPresent());
            assertEquals("access", actualDecodedJwt.getClaim(TOKEN_USE_CLAIM_NAME, String.class)
                                                   .get());
            assertNotNull(actualDecodedJwt.getIssuedAt());
            assertNotNull(actualDecodedJwt.getExpiresAt());
        }

        @Test
        @DisplayName("GIVEN refresh token is valid WHEN decode the token THEN returns the DecodedJwt")
        void RefreshTokenIsValid_Decode_ReturnsDecodedJwt() {
            // Given
            var refreshTokenJwt = jwtFaker.fakeJwt(REFRESH_TOKEN);

            var actualDecodedJwt = jwtDecoder.decode(refreshTokenJwt);

            // Then
            assertEquals(refreshTokenJwt, actualDecodedJwt.getJwt());
            assertEquals("rt+jwt", actualDecodedJwt.getType());
            assertEquals("TEST USERNAME", actualDecodedJwt.getSubject());
            assertTrue(actualDecodedJwt.getClaim(ROLE_CLAIM_NAME, String.class)
                                       .isPresent());
            assertEquals("USER", actualDecodedJwt.getClaim(ROLE_CLAIM_NAME, String.class)
                                                 .get());
            assertTrue(actualDecodedJwt.getClaim(TOKEN_USE_CLAIM_NAME, String.class)
                                       .isPresent());
            assertEquals("refresh", actualDecodedJwt.getClaim(TOKEN_USE_CLAIM_NAME, String.class)
                                                    .get());
            assertNotNull(actualDecodedJwt.getIssuedAt());
            assertNotNull(actualDecodedJwt.getExpiresAt());
        }

    }

}
