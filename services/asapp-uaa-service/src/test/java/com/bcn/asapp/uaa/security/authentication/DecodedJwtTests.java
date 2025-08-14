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

import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_ACCESS_CLAIM_VALUE;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_REFRESH_CLAIM_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultHeader;

import com.bcn.asapp.uaa.security.core.InvalidJwtException;
import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.testutil.JwtFaker;

class DecodedJwtTests {

    private String accessJwt;

    private String refreshJwt;

    private Header accessTokenHeader;

    private Claims accessTokenPayload;

    private Header refreshTokenHeader;

    private Claims refreshTokenPayload;

    @BeforeEach
    void beforeEach() {
        var jwtFaker = new JwtFaker();

        var accessTokenPayloadMap = Map.of("sub", "TEST USERNAME", ROLE_CLAIM_NAME, "USER", TOKEN_USE_CLAIM_NAME, TOKEN_USE_ACCESS_CLAIM_VALUE, "iat",
                1687600000L, "exp", 1687650000L);
        var refreshTokenPayloadMap = Map.of("sub", "TEST USERNAME", ROLE_CLAIM_NAME, "USER", TOKEN_USE_CLAIM_NAME, TOKEN_USE_REFRESH_CLAIM_VALUE, "iat",
                1687600000L, "exp", 1687650000L);

        this.accessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
        this.refreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
        this.accessTokenHeader = new DefaultHeader(Map.of("typ", ACCESS_TOKEN_TYPE));
        this.accessTokenPayload = new DefaultClaims(accessTokenPayloadMap);
        this.refreshTokenHeader = new DefaultHeader(Map.of("typ", REFRESH_TOKEN_TYPE));
        this.refreshTokenPayload = new DefaultClaims(refreshTokenPayloadMap);
    }

    @Nested
    class Constructor {

        @Test
        @DisplayName("GIVEN Jwt is null WHEN create an instance of DecodedJwt THEN throws InvalidJwtException")
        void JwtIsNull_Constructor_ThrowsInvalidJwtException() {
            // Given
            Executable executable = () -> new DecodedJwt(null, accessTokenHeader, accessTokenPayload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT could not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt is empty WHEN create an instance of DecodedJwt  THEN throws InvalidJwtException")
        void JwtIsEmpty_Constructor_ThrowsInvalidJwtException() {
            // Given
            var jwt = "";

            Executable executable = () -> new DecodedJwt(jwt, accessTokenHeader, accessTokenPayload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT could not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt header is null WHEN create an instance of DecodedJwt  THEN throws InvalidJwtException")
        void HeaderIsNull_Constructor_ThrowsInvalidJwtException() {
            // Given
            Executable executable = () -> new DecodedJwt(accessJwt, null, accessTokenPayload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Header could not be null", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt header type is null WHEN create an instance of DecodedJwt  THEN throws InvalidJwtException")
        void HeaderTypeIsNull_Constructor_ThrowsInvalidJwtException() {
            // Given
            var header = new DefaultHeader(Map.of());

            Executable executable = () -> new DecodedJwt(accessJwt, header, accessTokenPayload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Invalid JWT type, expected at+jwt or rt+jwt but was null", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt header type is empty WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void HeaderTypeIsEmpty_Constructor_ThrowsInvalidJwtException() {
            // Given
            var header = new DefaultHeader(Map.of("typ", ""));

            Executable executable = () -> new DecodedJwt(accessJwt, header, accessTokenPayload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Invalid JWT type, expected at+jwt or rt+jwt but was null", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt header type is invalid WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void HeaderTypeIsInvalid_Constructor_ThrowsInvalidJwtException() {
            // Given
            var header = new DefaultHeader(Map.of("typ", "TEST TYPE"));

            Executable executable = () -> new DecodedJwt(accessJwt, header, accessTokenPayload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Invalid JWT type, expected at+jwt or rt+jwt but was TEST TYPE", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt payload is null WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void PayloadIsNull_Constructor_ThrowsInvalidJwtException() {
            // Given
            Executable executable = () -> new DecodedJwt(accessJwt, accessTokenHeader, null);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Payload could not be null", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt payload subject is null WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void PayloadSubjectIsNull_Constructor_ThrowsInvalidJwtException() {
            // Given
            var claims = Map.of(ROLE_CLAIM_NAME, "USER", TOKEN_USE_CLAIM_NAME, TOKEN_USE_ACCESS_CLAIM_VALUE, "iat", 1687600000L, "exp", 1687650000L);
            var payload = new DefaultClaims(claims);

            Executable executable = () -> new DecodedJwt(accessJwt, accessTokenHeader, payload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT does not contain the mandatory claims", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt payload subject is empty WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void PayloadSubjectIsEmpty_Constructor_ThrowsInvalidJwtException() {
            // Given
            var claims = Map.of("sub", "", ROLE_CLAIM_NAME, "USER", TOKEN_USE_CLAIM_NAME, TOKEN_USE_ACCESS_CLAIM_VALUE, "iat", 1687600000L, "exp", 1687650000L);
            var payload = new DefaultClaims(claims);

            Executable executable = () -> new DecodedJwt(accessJwt, accessTokenHeader, payload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT does not contain the mandatory claims", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt payload authorities is null WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void PayloadAuthoritiesIsNull_Constructor_ThrowsInvalidJwtException() {
            // Given
            var claims = Map.of("sub", "TEST USERNAME", TOKEN_USE_CLAIM_NAME, TOKEN_USE_ACCESS_CLAIM_VALUE, "iat", 1687600000L, "exp", 1687650000L);
            var payload = new DefaultClaims(claims);

            Executable executable = () -> new DecodedJwt(accessJwt, accessTokenHeader, payload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT does not contain the mandatory claims", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt payload authorities is empty WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void PayloadAuthoritiesIsEmpty_Constructor_ThrowsInvalidJwtException() {
            // Given
            var claims = Map.of("sub", "TEST USERNAME", ROLE_CLAIM_NAME, "", TOKEN_USE_CLAIM_NAME, TOKEN_USE_ACCESS_CLAIM_VALUE, "iat", 1687600000L, "exp",
                    1687650000L);
            var payload = new DefaultClaims(claims);

            Executable executable = () -> new DecodedJwt(accessJwt, accessTokenHeader, payload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT does not contain the mandatory claims", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt payload token use claim is null WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void PayloadTokenUseClaimIsNull_Constructor_ThrowsInvalidJwtException() {
            // Given
            var claims = Map.of("sub", "TEST USERNAME", ROLE_CLAIM_NAME, "USER", "iat", 1687600000L, "exp", 1687650000L);
            var payload = new DefaultClaims(claims);

            Executable executable = () -> new DecodedJwt(accessJwt, accessTokenHeader, payload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT does not contain the mandatory claims", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt payload token use claim is empty WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void PayloadTokenUseClaimIsEmpty_Constructor_ThrowsInvalidJwtException() {
            // Given
            var claims = Map.of("sub", "TEST USERNAME", ROLE_CLAIM_NAME, "USER", TOKEN_USE_CLAIM_NAME, "", "iat", 1687600000L, "exp", 1687650000L);
            var payload = new DefaultClaims(claims);

            Executable executable = () -> new DecodedJwt(accessJwt, accessTokenHeader, payload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT does not contain the mandatory claims", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt payload issued at is null WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void PayloadIssuedAtIsNull_Constructor_ThrowsInvalidJwtException() {
            // Given
            var claims = Map.of("sub", "TEST USERNAME", ROLE_CLAIM_NAME, "USER", TOKEN_USE_CLAIM_NAME, TOKEN_USE_ACCESS_CLAIM_VALUE, "exp", 1687650000L);
            var payload = new DefaultClaims(claims);

            Executable executable = () -> new DecodedJwt(accessJwt, accessTokenHeader, payload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT does not contain the mandatory claims", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt payload expires at is null WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void PayloadExpiresAtIsNull_Constructor_ThrowsInvalidJwtException() {
            // Given
            var claims = Map.of("sub", "TEST USERNAME", ROLE_CLAIM_NAME, "USER", TOKEN_USE_CLAIM_NAME, TOKEN_USE_ACCESS_CLAIM_VALUE, "iat", 1687600000L);
            var payload = new DefaultClaims(claims);

            Executable executable = () -> new DecodedJwt(accessJwt, accessTokenHeader, payload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("JWT does not contain the mandatory claims", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN Jwt payload token use claim is invalid WHEN create an instance of DecodedJwt THEN  throws InvalidJwtException")
        void PayloadTokenUseClaimIsInvalid_Constructor_ThrowsInvalidJwtException() {
            // Given
            var claims = Map.of("sub", "TEST USERNAME", ROLE_CLAIM_NAME, "USER", TOKEN_USE_CLAIM_NAME, "TEST CLAIM", "iat", 1687600000L, "exp", 1687650000L);
            var payload = new DefaultClaims(claims);

            Executable executable = () -> new DecodedJwt(accessJwt, accessTokenHeader, payload);

            // Then
            var exceptionThrown = assertThrows(InvalidJwtException.class, executable);
            assertEquals("Invalid JWT token use claim, expected access or refresh but was TEST CLAIM", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN access token is valid WHEN create an instance of DecodedJwt THEN  instantiates the DecodedJwt")
        void AccessTokenIsValid_Constructor_InstantiatesDecodedJwt() {
            // Given
            var acualDecodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            // Then
            assertNotNull(acualDecodedJwt);
            assertEquals(accessJwt, acualDecodedJwt.getJwt());
            assertEquals("at+jwt", acualDecodedJwt.getType());
            assertEquals("TEST USERNAME", acualDecodedJwt.getSubject());
            assertTrue(acualDecodedJwt.getClaim(ROLE_CLAIM_NAME, String.class)
                                      .isPresent());
            assertEquals("USER", acualDecodedJwt.getClaim(ROLE_CLAIM_NAME, String.class)
                                                .get());
            assertTrue(acualDecodedJwt.getClaim(TOKEN_USE_CLAIM_NAME, String.class)
                                      .isPresent());
            assertEquals("access", acualDecodedJwt.getClaim(TOKEN_USE_CLAIM_NAME, String.class)
                                                  .get());
            assertEquals(Instant.ofEpochSecond(1687600000L), acualDecodedJwt.getIssuedAt());
            assertEquals(Instant.ofEpochSecond(1687650000L), acualDecodedJwt.getExpiresAt());
        }

        @Test
        @DisplayName("GIVEN refresh token is valid WHEN create an instance of DecodedJwt THEN  instantiates the DecodedJwt")
        void RefreshTokenIsValid_Constructor_InstantiatesDecodedJwt() {
            // Given
            var acualDecodedJwt = new DecodedJwt(refreshJwt, refreshTokenHeader, refreshTokenPayload);

            // Then
            assertNotNull(acualDecodedJwt);
            assertEquals(refreshJwt, acualDecodedJwt.getJwt());
            assertEquals("rt+jwt", acualDecodedJwt.getType());
            assertEquals("TEST USERNAME", acualDecodedJwt.getSubject());
            assertTrue(acualDecodedJwt.getClaim(ROLE_CLAIM_NAME, String.class)
                                      .isPresent());
            assertEquals("USER", acualDecodedJwt.getClaim(ROLE_CLAIM_NAME, String.class)
                                                .get());
            assertTrue(acualDecodedJwt.getClaim(TOKEN_USE_CLAIM_NAME, String.class)
                                      .isPresent());
            assertEquals("refresh", acualDecodedJwt.getClaim(TOKEN_USE_CLAIM_NAME, String.class)
                                                   .get());
            assertEquals(Instant.ofEpochSecond(1687600000L), acualDecodedJwt.getIssuedAt());
            assertEquals(Instant.ofEpochSecond(1687650000L), acualDecodedJwt.getExpiresAt());
        }

    }

    @Nested
    class IsAccessToken {

        @Test
        @DisplayName("GIVEN Jwt header type is not an access token and payload token use claim is not an access token WHEN check is access token THEN returns false")
        void HeaderTypeIsNotAccessTokenAndPayloadTokenUseClaimIsNotAccessToken_IsAccessToken_ReturnsFalse() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, refreshTokenHeader, refreshTokenPayload);

            var actualIsAccessToken = decodedJwt.isAccessToken();

            // Then
            assertFalse(actualIsAccessToken);
        }

        @Test
        @DisplayName("GIVEN Jwt header type is an access token and payload token use claim is not an access token WHEN check is access token THEN returns false")
        void HeaderTypeIsAccessTokenAndPayloadTokenUseClaimIsNotAccessToken_IsAccessToken_ReturnsFalse() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, refreshTokenPayload);

            var actualIsAccessToken = decodedJwt.isAccessToken();

            // Then
            assertFalse(actualIsAccessToken);
        }

        @Test
        @DisplayName("GIVEN Jwt header type is not an access token and payload token use claim is an access token WHEN check is access token THEN returns false")
        void HeaderTypeIsNotAccessTokenAndPayloadTokenUseClaimIsAccessToken_IsAccessToken_ReturnsFalse() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, refreshTokenHeader, accessTokenPayload);

            var actualIsAccessToken = decodedJwt.isAccessToken();

            // Then
            assertFalse(actualIsAccessToken);
        }

        @Test
        @DisplayName("GIVEN Jwt header type is an access token and payload token use claim is an access token WHEN check is access token THEN returns true")
        void HeaderTypeIsAccessTokenAndPayloadTokenUseClaimIsAccessToken_IsAccessToken_ReturnsTrue() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            var actualIsAccessToken = decodedJwt.isAccessToken();

            // Then
            assertTrue(actualIsAccessToken);
        }

    }

    @Nested
    class IsRefreshToken {

        @Test
        @DisplayName("GIVEN Jwt header type is not a refresh token and payload token use claim is not a refresh token WHEN check is refresh token THEN returns false")
        void HeaderTypeIsNotRefreshTokenAndPayloadTokenUseClaimIsNotRefreshToken_IsRefreshToken_ReturnsFalse() {
            // Given
            var decodedJwt = new DecodedJwt(refreshJwt, accessTokenHeader, accessTokenPayload);

            var actualIsRefreshToken = decodedJwt.isRefreshToken();

            // Then
            assertFalse(actualIsRefreshToken);
        }

        @Test
        @DisplayName("GIVEN Jwt header type is n refresh token and payload token use claim is not a refresh token WHEN check is refresh token THEN returns false")
        void HeaderTypeIsRefreshTokenAndPayloadTokenUseClaimIsNotRefreshToken_IsRefreshToken_ReturnsFalse() {
            // Given
            var decodedJwt = new DecodedJwt(refreshJwt, refreshTokenHeader, accessTokenPayload);

            var actualIsRefreshToken = decodedJwt.isRefreshToken();

            // Then
            assertFalse(actualIsRefreshToken);
        }

        @Test
        @DisplayName("GIVEN Jwt header type is not a refresh token and payload token use claim is a refresh token WHEN check is refresh token THEN returns false")
        void HeaderTypeIsNotRefreshTokenAndPayloadTokenUseClaimIsRefreshToken_IsRefreshToken_ReturnsFalse() {
            // Given
            var decodedJwt = new DecodedJwt(refreshJwt, accessTokenHeader, refreshTokenPayload);

            var actualIsRefreshToken = decodedJwt.isRefreshToken();

            // Then
            assertFalse(actualIsRefreshToken);
        }

        @Test
        @DisplayName("GIVEN Jwt header type is a refresh token and payload token use claim is a refresh token WHEN check is refresh token THEN returns true")
        void HeaderTypeIsRefreshTokenAndPayloadTokenUseClaimIsRefreshToken_IsRefreshToken_ReturnsTrue() {
            // Given
            var decodedJwt = new DecodedJwt(refreshJwt, refreshTokenHeader, refreshTokenPayload);

            var actualIsRefreshToken = decodedJwt.isRefreshToken();

            // Then
            assertTrue(actualIsRefreshToken);
        }

    }

    @Nested
    class GetJwt {

        @Test
        @DisplayName("WHEN get the Jwt field THEN returns the Jwt")
        void GetJwt_ReturnsJwt() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            var actualJwt = decodedJwt.getJwt();

            // Then
            assertNotNull(actualJwt);
            assertEquals(accessJwt, actualJwt);
        }

    }

    @Nested
    class GetType {

        @Test
        @DisplayName("WHEN get the type field THEN returns the type")
        void GetType_ReturnsType() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            var actualType = decodedJwt.getType();

            // Then
            assertNotNull(actualType);
            assertEquals(ACCESS_TOKEN_TYPE, actualType);
        }

    }

    @Nested
    class GetSubject {

        @Test
        @DisplayName("WHEN get the subject field THEN returns the subject")
        void GetSubject_ReturnsSubject() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            var actualSubject = decodedJwt.getSubject();

            // Then
            assertNotNull(actualSubject);
            assertEquals("TEST USERNAME", actualSubject);
        }

    }

    @Nested
    class GetRole {

        @Test
        @DisplayName("WHEN get the role field THEN returns the role")
        void GetRole_ReturnsRole() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            var actualRole = decodedJwt.getRole();

            // Then
            assertNotNull(actualRole);
            assertEquals("USER", actualRole.name());
        }

    }

    @Nested
    class GetClaim {

        @Test
        @DisplayName("GIVEN claim not exists WHEN get the claim THEN returns an empty Optional")
        void ClaimNotExists_GetClaim_ReturnsEmptyOptional() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            var actualClaim = decodedJwt.getClaim("TEST CLAIM", String.class);

            // Then
            assertTrue(actualClaim.isEmpty());
        }

        @Test
        @DisplayName("GIVEN claim exists but type does not match WHEN get the claim THEN returns an empty Optional")
        void ClaimExistsAndTypeDoesNotMatch_GetClaim_ReturnsEmptyOptional() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            var actualClaim = decodedJwt.getClaim(ROLE_CLAIM_NAME, Integer.class);

            // Then
            assertTrue(actualClaim.isEmpty());
        }

        @Test
        @DisplayName("GIVEN claim exists WHEN get the claim THEN returns the claim")
        void ClaimExists_GetC1aim_ReturnsTheClaim() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            var actualClaim = decodedJwt.getClaim(ROLE_CLAIM_NAME, String.class);

            // Then
            assertTrue(actualClaim.isPresent());
            assertEquals("USER", actualClaim.get());
        }

    }

    @Nested
    class GetIssuedAt {

        @Test
        @DisplayName("WHEN get the issued at field THEN returns the issued at date")
        void GetIssuedAt_ReturnsIssuedAt() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            var actualIssuedAt = decodedJwt.getIssuedAt();

            // Then
            assertNotNull(actualIssuedAt);
            assertEquals(Instant.ofEpochSecond(1687600000L), actualIssuedAt);
        }

    }

    @Nested
    class GetExpiresAt {

        @Test
        @DisplayName("WHEN get the expires at field THEN returns the expires at date")
        void GetExpiresAt_ReturnsExpiresAt() {
            // Given
            var decodedJwt = new DecodedJwt(accessJwt, accessTokenHeader, accessTokenPayload);

            var actualExpiresAt = decodedJwt.getExpiresAt();

            // Then
            assertNotNull(actualExpiresAt);
            assertEquals(Instant.ofEpochSecond(1687650000L), actualExpiresAt);
        }

    }

}
