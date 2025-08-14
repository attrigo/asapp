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

package com.bcn.asapp.uaa.security.authentication.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.jsonwebtoken.JwtException;

import com.bcn.asapp.uaa.security.authentication.DecodedJwt;
import com.bcn.asapp.uaa.security.authentication.InvalidAccessTokenException;
import com.bcn.asapp.uaa.security.authentication.JwtDecoder;
import com.bcn.asapp.uaa.security.authentication.matcher.JwtSessionMatcher;
import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.testutil.JwtFaker;

@ExtendWith(SpringExtension.class)
class AccessTokenVerifierTests {

    @Mock
    private JwtDecoder jwtDecoderMock;

    @Mock
    private JwtSessionMatcher jwtSessionMatcherMock;

    @InjectMocks
    private AccessTokenVerifier accessTokenVerifier;

    private JwtFaker jwtFaker;

    @BeforeEach
    void beforeEach() {
        this.jwtFaker = new JwtFaker();
    }

    @Nested
    class Verify {

        @Test
        @DisplayName("Given access token is invalid WHEN verify an access token THEN throws InvalidAccessTokenException")
        void AccessTokenIsInvalid_Verify_ThrowsInvalidAccessTokenException() {
            // Given
            given(jwtDecoderMock.decode(anyString())).willThrow(new JwtException("TEST EXCEPTION"));

            // When
            var jwt = jwtFaker.fakeJwtInvalid();

            Executable executable = () -> accessTokenVerifier.verify(jwt);

            // Then
            var exceptionThrown = assertThrows(InvalidAccessTokenException.class, executable);
            assertEquals("Access token is not valid", exceptionThrown.getMessage());

            then(jwtDecoderMock).should(times(1))
                                .decode(anyString());
            then(jwtSessionMatcherMock).should(never())
                                       .match(any(DecodedJwt.class));
        }

        @Test
        @DisplayName("Given token is not an access token WHEN verify an access token THEN throws InvalidAccessTokenException")
        void TokenIsNotAccessToken_Verify_ThrowsInvalidAccessTokenException() {
            // Given
            var fakeDecodedJwt = jwtFaker.fakeDecodedJwt(jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN));
            given(jwtDecoderMock.decode(anyString())).willReturn(fakeDecodedJwt);

            // When
            var jwt = fakeDecodedJwt.getJwt();

            Executable executable = () -> accessTokenVerifier.verify(jwt);

            // Then
            var exceptionThrown = assertThrows(InvalidAccessTokenException.class, executable);
            assertEquals("Access token is not valid", exceptionThrown.getMessage());
            assertEquals("JWT " + jwt + " is not a " + JwtType.ACCESS_TOKEN, exceptionThrown.getCause()
                                                                                            .getMessage());

            then(jwtDecoderMock).should(times(1))
                                .decode(anyString());
            then(jwtSessionMatcherMock).should(never())
                                       .match(any(DecodedJwt.class));
        }

        @Test
        @DisplayName("Given access token does not match WHEN verify an access token THEN throws InvalidAccessTokenException")
        void AccessTokenNotMatch_Verify_ThrowsInvalidAccessTokenException() {
            // Given
            var fakeDecodedJwt = jwtFaker.fakeDecodedJwt(jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN));
            given(jwtDecoderMock.decode(anyString())).willReturn(fakeDecodedJwt);

            given(jwtSessionMatcherMock.match(any(DecodedJwt.class))).willReturn(false);

            // When
            var jwt = fakeDecodedJwt.getJwt();

            Executable executable = () -> accessTokenVerifier.verify(jwt);

            // Then
            var exceptionThrown = assertThrows(InvalidAccessTokenException.class, executable);
            assertEquals("Access token is not valid", exceptionThrown.getMessage());
            assertEquals("JWT does not match for user " + fakeDecodedJwt.getSubject(), exceptionThrown.getCause()
                                                                                                      .getMessage());

            then(jwtDecoderMock).should(times(1))
                                .decode(anyString());
            then(jwtSessionMatcherMock).should(times(1))
                                       .match(any(DecodedJwt.class));
        }

        @Test
        @DisplayName("Given access token matches WHEN verify an access token THEN returns the authentication token")
        void AccessTokenMatches_Verify_ReturnsTheAuthenticationToken() {
            // Given
            var fakeDecodedJwt = jwtFaker.fakeDecodedJwt(jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN));
            given(jwtDecoderMock.decode(anyString())).willReturn(fakeDecodedJwt);

            given(jwtSessionMatcherMock.match(any(DecodedJwt.class))).willReturn(true);

            // When
            var jwt = fakeDecodedJwt.getJwt();

            var actualAuthentication = accessTokenVerifier.verify(jwt);

            // Then
            assertNotNull(actualAuthentication);
            assertEquals(fakeDecodedJwt.getSubject(), actualAuthentication.getName());
            assertEquals(fakeDecodedJwt.getJwt(), actualAuthentication.getJwt());
            assertNotNull(actualAuthentication.getAuthorities());
            assertNotNull(actualAuthentication.getPrincipal());

            then(jwtDecoderMock).should(times(1))
                                .decode(anyString());
            then(jwtSessionMatcherMock).should(times(1))
                                       .match(any(DecodedJwt.class));
        }

    }

}
