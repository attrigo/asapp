/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

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

import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;
import com.bcn.asapp.authentication.infrastructure.security.CustomUserDetails;
import com.bcn.asapp.authentication.infrastructure.security.InvalidPrincipalException;
import com.bcn.asapp.authentication.infrastructure.security.RoleNotFoundException;

/**
 * Tests {@link CredentialsAuthenticatorAdapter} Spring Security delegation and domain type translation.
 * <p>
 * Coverage:
 * <li>Delegates authentication to Spring Security framework</li>
 * <li>Translates authenticated principal to domain UserAuthentication</li>
 * <li>Propagates authentication failures from Spring Security</li>
 * <li>Handles invalid principal type with domain exception</li>
 */
@ExtendWith(MockitoExtension.class)
class CredentialsAuthenticatorAdapterTests {

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private CredentialsAuthenticatorAdapter credentialsAuthenticatorAdapter;

    @Nested
    class Authenticate {

        @Test
        void ReturnsAuthenticatedUser_ValidCredentials() {
            // Given
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");
            var userIdValue = UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631");
            var authorities = AuthorityUtils.createAuthorityList(USER.name());
            var userDetails = new CustomUserDetails(userIdValue, username.value(), password.value(), authorities);
            var authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            var userId = UserId.of(userIdValue);

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authenticationToken);

            // When
            var actual = credentialsAuthenticatorAdapter.authenticate(username, password);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("authenticated user").isNotNull();
                softly.assertThat(actual.isAuthenticated()).as("authenticated").isTrue();
                softly.assertThat(actual.userId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.username()).as("username").isEqualTo(username);
                softly.assertThat(actual.role()).as("role").isEqualTo(USER);
                softly.assertThat(actual.password()).as("password").isNull();
                // @formatter:on
            });

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        void ThrowsBadCredentialsException_AuthenticationFails() {
            // Given
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");

            willThrow(new BadCredentialsException("Invalid credentials")).given(authenticationManager)
                                                                         .authenticate(any(UsernamePasswordAuthenticationToken.class));

            // When
            var actual = catchThrowable(() -> credentialsAuthenticatorAdapter.authenticate(username, password));

            // Then
            assertThat(actual).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication failed due to")
                              .hasCauseInstanceOf(BadCredentialsException.class);

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        void ThrowsBadCredentialsException_PrincipalNotCustomUserDetails() {
            // Given
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");
            var authorities = AuthorityUtils.createAuthorityList(USER.name());
            var authenticationToken = new UsernamePasswordAuthenticationToken("invalid_principal", null, authorities);

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authenticationToken);

            // When
            var actual = catchThrowable(() -> credentialsAuthenticatorAdapter.authenticate(username, password));

            // Then
            assertThat(actual).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication failed due to")
                              .hasCauseInstanceOf(InvalidPrincipalException.class);

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        void ThrowsBadCredentialsException_EmptyAuthorities() {
            // Given
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");
            var userId = UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631");
            var userDetails = new CustomUserDetails(userId, username.value(), password.value(), AuthorityUtils.NO_AUTHORITIES);
            var authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, AuthorityUtils.NO_AUTHORITIES);

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authenticationToken);

            // When
            var actual = catchThrowable(() -> credentialsAuthenticatorAdapter.authenticate(username, password));

            // Then
            assertThat(actual).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication failed due to")
                              .hasCauseInstanceOf(RoleNotFoundException.class);

            then(authenticationManager).should(times(1))
                                       .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

    }

}
