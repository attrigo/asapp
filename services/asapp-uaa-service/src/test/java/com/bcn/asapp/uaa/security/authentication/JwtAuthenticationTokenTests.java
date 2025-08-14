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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.testutil.JwtFaker;

class JwtAuthenticationTokenTests {

    private JwtFaker jwtFaker;

    private String username;

    private List<SimpleGrantedAuthority> authorities;

    private String jwt;

    @BeforeEach
    void beforeEach() {
        this.jwtFaker = new JwtFaker();

        this.username = "TEST USERNAME";
        this.authorities = List.of(new SimpleGrantedAuthority("USER"));
        this.jwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
    }

    @Nested
    class Constructor {

        @Test
        @DisplayName("Given subject is null and Jwt is present WHEN create an instance of JwtAuthenticationToken THEN returns un-authenticated JwtAuthenticationToken without subject")
        void SubjectIsNullAndJwtIsPresent_Constructor_ReturnsUnauthenticatedJwtAuthenticationTokenWithoutSubject() {
            // When
            var actualAuthentication = new JwtAuthenticationToken(null, jwt);

            // Then
            assertTrue(actualAuthentication.getAuthorities()
                                           .isEmpty());
            assertEquals("", actualAuthentication.getName());
            assertEquals(jwt, actualAuthentication.getJwt());
            assertFalse(actualAuthentication.isAuthenticated());
        }

        @Test
        @DisplayName("Given subject is empty and Jwt is present WHEN create an instance of JwtAuthenticationToken THEN returns un-authenticated JwtAuthenticationToken without subject")
        void SubjectIsEmptyAndJwtIsPresent_Constructor_ReturnsUnauthenticatedJwtAuthenticationTokenWithoutSubject() {
            // When
            var subject = "";

            var actualAuthentication = new JwtAuthenticationToken(subject, jwt);

            // Then
            assertTrue(actualAuthentication.getAuthorities()
                                           .isEmpty());
            assertEquals(subject, actualAuthentication.getName());
            assertEquals(jwt, actualAuthentication.getJwt());
            assertFalse(actualAuthentication.isAuthenticated());
        }

        @Test
        @DisplayName("Given subject is present and Jwt is null WHEN create an instance of JwtAuthenticationToken THEN throws IllegalArgumentException")
        void SubjectIsPresentAndJwtIsNull_Constructor_ThrowsIllegalArgumentException() {
            // When
            Executable executable = () -> new JwtAuthenticationToken(username, null);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("JWT must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("Given subject is present and Jwt is empty WHEN create an instance of JwtAuthenticationToken THEN throws IllegalArgumentException")
        void SubjectIsPresentAndJwtIsEmpty_Constructor_ThrowsIllegalArgumentException() {
            // When
            var jwt = "";

            Executable executable = () -> new JwtAuthenticationToken(username, jwt);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("JWT must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("Given subject is present and Jwt is present WHEN create an instance of JwtAuthenticationToken THEN returns un-authenticated JwtAuthenticationToken")
        void SubjectAndJwtArePresent_Constructor_ReturnsUnauthenticatedJwtAuthenticationToken() {
            // When
            var actualAuthentication = new JwtAuthenticationToken(username, jwt);

            // Then
            assertTrue(actualAuthentication.getAuthorities()
                                           .isEmpty());
            assertEquals(username, actualAuthentication.getName());
            assertEquals(jwt, actualAuthentication.getJwt());
            assertFalse(actualAuthentication.isAuthenticated());
        }

        @Test
        @DisplayName("Given subject is present and authorities are present and Jwt is present WHEN create an instance of JwtAuthenticationToken THEN returns authenticated JwtAuthenticationToken without subject")
        void SubjectIsNullAndAuthoritiesArePresentAndJwtIsPresent_Constructor_ReturnsAuthenticatedJwtAuthenticationTokenWithoutSubject() {
            // When
            var actualAuthentication = new JwtAuthenticationToken(null, authorities, jwt);

            // Then
            assertEquals(1L, actualAuthentication.getAuthorities()
                                                 .size());
            assertThat(actualAuthentication.getAuthorities(), contains(authorities.getFirst()));
            assertEquals("", actualAuthentication.getName());
            assertEquals(jwt, actualAuthentication.getJwt());
            assertTrue(actualAuthentication.isAuthenticated());
        }

        @Test
        @DisplayName("Given subject is empty and authorities are present and Jwt is present WHEN create an instance of JwtAuthenticationToken THEN returns authenticated JwtAuthenticationToken without subject")
        void SubjectIsEmptyAndAuthoritiesArePresentAndJwtIsPresent_Constructor_ReturnsAuthenticatedJwtAuthenticationTokenWithoutSubject() {
            // When
            var subject = "";

            var actualAuthentication = new JwtAuthenticationToken(subject, authorities, jwt);

            // Then
            assertEquals(1L, actualAuthentication.getAuthorities()
                                                 .size());
            assertThat(actualAuthentication.getAuthorities(), contains(authorities.getFirst()));
            assertEquals(subject, actualAuthentication.getName());
            assertEquals(jwt, actualAuthentication.getJwt());
            assertTrue(actualAuthentication.isAuthenticated());
        }

        @Test
        @DisplayName("Given subject is present and authorities are null and Jwt is present WHEN create an instance of JwtAuthenticationToken THEN returns authenticated JwtAuthenticationToken without authorities")
        void SubjectIsPresentAndAuthoritiesIsNullAndJwtIsPresent_Constructor_ReturnsAuthenticatedJwtAuthenticationTokenWithoutAuthorities() {
            // When
            var actualAuthentication = new JwtAuthenticationToken(username, null, jwt);

            // Then
            assertTrue(actualAuthentication.getAuthorities()
                                           .isEmpty());
            assertEquals(username, actualAuthentication.getName());
            assertEquals(jwt, actualAuthentication.getJwt());
            assertTrue(actualAuthentication.isAuthenticated());
        }

        @Test
        @DisplayName("Given subject is present and some authority is null and Jwt is present WHEN create an instance of JwtAuthenticationToken THEN throws IllegalArgumentException")
        void SubjectIsPresentAndSomeAuthorityIsNullAndJwtIsPresent_Constructor_ThrowsIllegalArgumentException() {
            // When
            var authorities = new ArrayList<SimpleGrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority("USER"));
            authorities.add(null);

            Executable executable = () -> new JwtAuthenticationToken(username, authorities, jwt);

            // Then
            assertThrows(IllegalArgumentException.class, executable);
        }

        @Test
        @DisplayName("Given subject is present and authorities are present and Jwt is null WHEN create an instance of JwtAuthenticationToken THEN throws IllegalArgumentException")
        void SubjectIsPresentAndAuthoritiesArePresentAndJwtIsNull_Constructor_ThrowsIllegalArgumentException() {
            // When
            Executable executable = () -> new JwtAuthenticationToken(username, authorities, null);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("JWT must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("Given subject is present and authorities are present and Jwt is empty WHEN create an instance of JwtAuthenticationToken THEN throws IllegalArgumentException")
        void SubjectIsPresentAndAuthoritiesArePresentAndJwtIsEmpty_Constructor_ThrowsIllegalArgumentException() {
            // When
            var jwt = "";

            Executable executable = () -> new JwtAuthenticationToken(username, authorities, jwt);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("JWT must not be null or empty", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("Given subject is present and authorities are present and Jwt are present WHEN create an instance of JwtAuthenticationToken THEN returns authenticated JwtAuthenticationToken")
        void SubjectIsPresentAndAuthoritiesArePresentAndJwtArePresent_Constructor_ReturnsAuthenticatedJwtAuthenticationToken() {
            // When
            var actualAuthentication = new JwtAuthenticationToken(username, authorities, jwt);

            // Then
            assertEquals(1L, actualAuthentication.getAuthorities()
                                                 .size());
            assertThat(actualAuthentication.getAuthorities(), contains(authorities.getFirst()));
            assertEquals(username, actualAuthentication.getName());
            assertEquals(jwt, actualAuthentication.getJwt());
            assertTrue(actualAuthentication.isAuthenticated());
        }

    }

    @Nested
    class Unauthenticated {

        @Test
        @DisplayName("Given decoded JWT is null WHEN create unauthenticated instance of JwtAuthenticationToken THEN throws IllegalArgumentException")
        void DecodedJwtIsNull_Unauthenticated_ThrowsIllegalArgumentException() {
            // When
            Executable executable = () -> JwtAuthenticationToken.unauthenticated(null);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Decoded JWT must not be null", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("Given decoded JWT is present WHEN create unauthenticated instance of JwtAuthenticationToken THEN returns un-authenticated JwtAuthenticationToken")
        void DecodedJwtIsValid_Unauthenticated_ReturnsUnauthenticatedJwtAuthenticationToken() {
            // When
            var decodedJwt = jwtFaker.fakeDecodedJwt(jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN));

            var actualAuthentication = JwtAuthenticationToken.unauthenticated(decodedJwt);

            // Then
            assertTrue(actualAuthentication.getAuthorities()
                                           .isEmpty());
            assertEquals(decodedJwt.getSubject(), actualAuthentication.getName());
            assertEquals(decodedJwt.getJwt(), actualAuthentication.getJwt());
            assertFalse(actualAuthentication.isAuthenticated());
        }

    }

    @Nested
    class Authenticated {

        @Test
        @DisplayName("Given decoded JWT is null WHEN create authenticated instance of JwtAuthenticationToken THEN throws IllegalArgumentException")
        void DecodedJwtIsNull_Authenticated_ThrowsIllegalArgumentException() {
            // When
            Executable executable = () -> JwtAuthenticationToken.authenticated(null);

            // Then
            var exceptionThrown = assertThrows(IllegalArgumentException.class, executable);
            assertEquals("Decoded JWT must not be null", exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("Given decoded JWT is present WHEN create authenticated instance of JwtAuthenticationToken THEN returns authenticated JwtAuthenticationToken")
        void DecodedJwtIsValid_Authenticated_ReturnsAuthenticatedJwtAuthenticationToken() {
            // When
            var decodedJwt = jwtFaker.fakeDecodedJwt(jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN));

            var actualAuthentication = JwtAuthenticationToken.authenticated(decodedJwt);

            // Then
            assertEquals(1L, actualAuthentication.getAuthorities()
                                                 .size());
            assertThat(actualAuthentication.getAuthorities(), contains(authorities.getFirst()));
            assertEquals(decodedJwt.getSubject(), actualAuthentication.getName());
            assertEquals(decodedJwt.getJwt(), actualAuthentication.getJwt());
            assertTrue(actualAuthentication.isAuthenticated());
        }

    }

    @Nested
    class GetCredentials {

        @Test
        @DisplayName("WHEN get credentials field THEN returns null")
        void GetCredentials_ReturnsNull() {
            // Given
            var actualAuthentication = new JwtAuthenticationToken(username, authorities, jwt);

            var actualCredentials = actualAuthentication.getCredentials();

            // Then
            assertNull(actualCredentials);
        }

    }

    @Nested
    class GetPrincipal {

        @Test
        @DisplayName("WHEN get principal field THEN returns the principal")
        void GetPrincipal_ReturnsPrincipal() {
            // Given
            var actualAuthentication = new JwtAuthenticationToken(username, authorities, jwt);

            var actualPrincipal = actualAuthentication.getPrincipal();

            // Then
            assertEquals(username, actualPrincipal);
        }

    }

    @Nested
    class GetJwt {

        @Test
        @DisplayName("WHEN get jwt field THEN returns the jwt")
        void GetJwt_ReturnsJwt() {
            // Given
            var actualAuthentication = new JwtAuthenticationToken(username, authorities, jwt);

            var actualJwt = actualAuthentication.getJwt();

            // Then
            assertEquals(jwt, actualJwt);
        }

    }

}
