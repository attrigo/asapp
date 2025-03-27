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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.uaa.auth.UserCredentialsDTO;
import com.bcn.asapp.uaa.config.security.JwtTokenProvider;

@ExtendWith(SpringExtension.class)
class AuthServiceImplTests {

    @Mock
    private AuthenticationManager authenticationManagerMock;

    @Mock
    private JwtTokenProvider jwtTokenProviderMock;

    @InjectMocks
    private AuthServiceImpl authService;

    private String fakeUsername;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.fakeUsername = "UT username";
        this.fakePassword = "UT password";
    }

    @Nested
    class Login {

        @Test
        @DisplayName("GIVEN user credentials are not valid WHEN login a user THEN does not authenticate the user And throws an AuthenticationException")
        void UserCredentialsAreNotValid_Login_DoesNotAuthenticateUserAndThrowsAuthenticationException() {
            // Given
            given(authenticationManagerMock.authenticate(any(UsernamePasswordAuthenticationToken.class))).willThrow(
                    new BadCredentialsException("UT Exception"));

            // When
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);

            assertThrows(BadCredentialsException.class, () -> authService.login(userCredentialsToLogin));

            // Then
            then(authenticationManagerMock).should(times(1))
                                           .authenticate(any(UsernamePasswordAuthenticationToken.class));
            then(jwtTokenProviderMock).should(never())
                                      .generateToken(any(Authentication.class));
        }

        @Test
        @DisplayName("GIVEN user credentials are valid WHEN login a user THEN authenticates the user And returns the generated authentication")
        void UserCredentialsAreValid_Login_AuthenticatesUserAndReturnsTheGeneratedAuthentication() {
            // Given
            var fakeAuthentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);
            given(authenticationManagerMock.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(fakeAuthentication);

            // When
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);

            var actualAuthentication = authService.login(userCredentialsToLogin);

            // Then
            assertNotNull(actualAuthentication);

            then(authenticationManagerMock).should(times(1))
                                           .authenticate(any(UsernamePasswordAuthenticationToken.class));
            then(jwtTokenProviderMock).should(times(1))
                                      .generateToken(any(UsernamePasswordAuthenticationToken.class));
        }

    }

}
