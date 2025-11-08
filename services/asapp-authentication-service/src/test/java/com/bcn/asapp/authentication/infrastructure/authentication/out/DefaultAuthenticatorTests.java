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

package com.bcn.asapp.authentication.infrastructure.authentication.out;

import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;
import com.bcn.asapp.authentication.infrastructure.security.CustomUserDetails;
import com.bcn.asapp.authentication.infrastructure.security.InvalidPrincipalException;
import com.bcn.asapp.authentication.infrastructure.security.RoleNotFoundException;

@ExtendWith(MockitoExtension.class)
class DefaultAuthenticatorTests {

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private DefaultAuthenticator defaultAuthenticator;

    private final UUID userId = UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631");

    private final Username username = Username.of("user@asapp.com");

    private final RawPassword password = RawPassword.of("TEST@09_password?!");

    private final Role role = USER;

    @Nested
    class Authenticate {

        @Test
        void ThenThrowsBadCredentialsException_GivenAuthenticationFails() {
            // Given
            willThrow(new BadCredentialsException("Invalid credentials")).given(authenticationManager)
                                                                         .authenticate(any(UsernamePasswordAuthenticationToken.class));

            var authenticationRequest = UserAuthentication.unAuthenticated(username, password);

            // When
            var thrown = catchThrowable(() -> defaultAuthenticator.authenticate(authenticationRequest));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication failed due to");

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        void ThenThrowsBadCredentialsException_GivenPrincipalIsNotCustomUserDetails() {
            // Given
            var authenticationToken = new UsernamePasswordAuthenticationToken("invalid_principal", null, AuthorityUtils.createAuthorityList(role.name()));
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authenticationToken);

            var authenticationRequest = UserAuthentication.unAuthenticated(username, password);

            // When
            var thrown = catchThrowable(() -> defaultAuthenticator.authenticate(authenticationRequest));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication failed due to")
                              .hasCauseInstanceOf(InvalidPrincipalException.class);

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        void ThenThrowsBadCredentialsException_GivenAuthoritiesAreEmpty() {
            // Given
            var userDetails = new CustomUserDetails(userId, username.value(), password.value(), AuthorityUtils.NO_AUTHORITIES);
            var authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, AuthorityUtils.NO_AUTHORITIES);
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authenticationToken);

            var authenticationRequest = UserAuthentication.unAuthenticated(username, password);

            // When
            var thrown = catchThrowable(() -> defaultAuthenticator.authenticate(authenticationRequest));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication failed due to")
                              .hasCauseInstanceOf(RoleNotFoundException.class);

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        void ThenAuthenticatesUser_GivenAuthenticationRequestIsValid() {
            // Given
            var userDetails = new CustomUserDetails(userId, username.value(), password.value(), AuthorityUtils.createAuthorityList(role.name()));
            var authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, AuthorityUtils.createAuthorityList(role.name()));
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authenticationToken);

            var authenticationRequest = UserAuthentication.unAuthenticated(username, password);

            // When
            var actual = defaultAuthenticator.authenticate(authenticationRequest);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.isAuthenticated()).isTrue();
            assertThat(actual.userId()).isEqualTo(UserId.of(userId));
            assertThat(actual.username()).isEqualTo(username);
            assertThat(actual.role()).isEqualTo(role);
            assertThat(actual.password()).isNull();

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));

        }

    }

}
