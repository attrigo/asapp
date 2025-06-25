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
package com.bcn.asapp.uaa.auth.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.uaa.auth.RefreshTokenDTO;
import com.bcn.asapp.uaa.auth.UserCredentialsDTO;
import com.bcn.asapp.uaa.security.authentication.InvalidRefreshTokenException;
import com.bcn.asapp.uaa.security.authentication.JwtAuthenticationToken;
import com.bcn.asapp.uaa.security.authentication.issuer.JwtIssuer;
import com.bcn.asapp.uaa.security.authentication.verifier.JwtVerifier;
import com.bcn.asapp.uaa.security.core.AccessToken;
import com.bcn.asapp.uaa.security.core.JwtAuthentication;
import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.testutil.JwtFaker;

@ExtendWith(SpringExtension.class)
class AuthServiceImplTests {

    @Mock
    private AuthenticationManager authenticationManagerMock;

    @Mock
    private JwtVerifier refreshTokenVerifierMock;

    @Mock
    private JwtIssuer jwtIssuerMock;

    @InjectMocks
    private AuthServiceImpl authService;

    private JwtFaker jwtFaker;

    private String fakeUsername;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.jwtFaker = new JwtFaker();

        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
    }

    @Nested
    class Authenticate {

        @Test
        @DisplayName("GIVEN user credentials are not valid WHEN authenticate a user THEN does not authenticate the user And throws an BadCredentialsException")
        void UserCredentialsAreNotValid_Authenticate_DoesNotAuthenticateUserAndThrowsAuthenticationException() {
            // Given
            given(authenticationManagerMock.authenticate(any(UsernamePasswordAuthenticationToken.class))).willThrow(
                    new BadCredentialsException("TEST EXCEPTION"));

            // When
            var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);

            Executable executable = () -> authService.authenticate(userCredentialsToAuthenticate);

            // Then
            assertThrows(BadCredentialsException.class, executable);

            then(authenticationManagerMock).should(times(1))
                                           .authenticate(any(UsernamePasswordAuthenticationToken.class));
            then(jwtIssuerMock).should(never())
                               .issueAuthentication(any(Authentication.class));
        }

        @Test
        @DisplayName("GIVEN user credentials are valid WHEN authenticate a user THEN authenticates the user And returns the generated authentication")
        void UserCredentialsAreValid_Authenticate_AuthenticatesUserAndReturnsGeneratedAuthentication() {
            // Given
            var fakeAuthentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);
            given(authenticationManagerMock.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(fakeAuthentication);

            var fakeAccessToken = new AccessToken(UUID.randomUUID(), UUID.randomUUID(), jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN), Instant.now(), Instant.now());
            var fakeRefreshToken = new com.bcn.asapp.uaa.security.core.RefreshToken(UUID.randomUUID(), UUID.randomUUID(),
                    jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN), Instant.now(), Instant.now());
            var fakeAuthenticationDTO = new JwtAuthentication(fakeAccessToken, fakeRefreshToken);
            given(jwtIssuerMock.issueAuthentication(any(Authentication.class))).willReturn(fakeAuthenticationDTO);

            // When
            var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);

            var actualAuthentication = authService.authenticate(userCredentialsToAuthenticate);

            // Then
            assertNotNull(SecurityContextHolder.getContext()
                                               .getAuthentication());
            assertNotNull(actualAuthentication);
            assertNotNull(actualAuthentication.accessToken());
            assertNotNull(actualAuthentication.refreshToken());
            assertNotNull(actualAuthentication.accessToken()
                                              .jwt());
            assertNotNull(actualAuthentication.refreshToken()
                                              .jwt());

            then(authenticationManagerMock).should(times(1))
                                           .authenticate(any(UsernamePasswordAuthenticationToken.class));
            then(jwtIssuerMock).should(times(1))
                               .issueAuthentication(any(UsernamePasswordAuthenticationToken.class));
        }

    }

    @Nested
    class RefreshToken {

        @Test
        @DisplayName("GIVEN refresh token is invalid WHEN refresh a token THEN does not refresh the token And throws InvalidRefreshTokenException")
        void RefreshTokenIsInvalid_RefreshToken_DoesNotRefreshTokenAndThrowsInvalidJwtException() {
            // Given
            given(refreshTokenVerifierMock.verify(anyString())).willThrow(new InvalidRefreshTokenException("TEST EXCEPTION"));

            // When
            var refreshTokenToRefresh = new RefreshTokenDTO(jwtFaker.fakeJwtInvalid());

            Executable executable = () -> authService.refreshToken(refreshTokenToRefresh);

            // Then
            assertThrows(InvalidRefreshTokenException.class, executable);

            then(refreshTokenVerifierMock).should(times(1))
                                          .verify(anyString());
            then(jwtIssuerMock).should(never())
                               .issueAuthentication(any(JwtAuthenticationToken.class));
        }

        @Test
        @DisplayName("GIVEN refresh token is valid WHEN refresh a token THEN refresh the token And returns a new authentication")
        void RefreshTokenIsValid_RefreshToken_GeneratesAndReturnsNewAuthentication() {
            // Given
            var fakeDecodedJwt = jwtFaker.fakeDecodedJwt(jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN));
            var fakeAuthentication = JwtAuthenticationToken.authenticated(fakeDecodedJwt);
            given(refreshTokenVerifierMock.verify(anyString())).willReturn(fakeAuthentication);

            var fakeAccessToken = new AccessToken(UUID.randomUUID(), UUID.randomUUID(), jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN), Instant.now(), Instant.now());
            var fakeRefreshToken = new com.bcn.asapp.uaa.security.core.RefreshToken(UUID.randomUUID(), UUID.randomUUID(),
                    jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN), Instant.now(), Instant.now());
            var fakeAuthenticationDTO = new JwtAuthentication(fakeAccessToken, fakeRefreshToken);
            given(jwtIssuerMock.issueAuthentication(any(Authentication.class))).willReturn(fakeAuthenticationDTO);

            // When
            var refreshTokenToRefresh = new RefreshTokenDTO(jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN));

            var actualAuthentication = authService.refreshToken(refreshTokenToRefresh);

            // Then
            assertNotNull(actualAuthentication);
            assertNotNull(actualAuthentication.accessToken());
            assertNotNull(actualAuthentication.refreshToken());
            assertNotNull(actualAuthentication.accessToken()
                                              .jwt());
            assertNotNull(actualAuthentication.refreshToken()
                                              .jwt());

            then(refreshTokenVerifierMock).should(times(1))
                                          .verify(anyString());
            then(jwtIssuerMock).should(times(1))
                               .issueAuthentication(any(JwtAuthenticationToken.class));
        }

    }

}
