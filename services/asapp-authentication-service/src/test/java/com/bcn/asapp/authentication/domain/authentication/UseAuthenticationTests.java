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

package com.bcn.asapp.authentication.domain.authentication;

import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

class UseAuthenticationTests {

    private final UserId userId = UserId.of(UUID.randomUUID());

    private final Username username = Username.of("user@asapp.com");

    private final RawPassword password = RawPassword.of("password");

    private final Role role = USER;

    @Nested
    class CreateUnAuthenticateUserWithConstructor {

        @Test
        void ThenReturnsUnAuthenticatedUser_GivenIdIsNull() {
            // When
            var actual = new UserAuthentication(null, username, password, role, false);

            // Then
            assertThat(actual.userId()).isNull();
            assertThat(actual.username()).isEqualTo(username);
            assertThat(actual.password()).isEqualTo(password);
            assertThat(actual.role()).isEqualTo(role);
            assertThat(actual.isAuthenticated()).isFalse();
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNull() {
            // When
            var thrown = catchThrowable(() -> new UserAuthentication(userId, null, password, role, false));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenPasswordIsNull() {
            // When
            var thrown = catchThrowable(() -> new UserAuthentication(userId, username, null, role, false));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Password must not be null");
        }

        @Test
        void ThenReturnsUnAuthenticatedUser_GivenRoleIsNull() {
            // When
            var actual = new UserAuthentication(userId, username, password, null, false);

            // Then
            assertThat(actual.userId()).isEqualTo(userId);
            assertThat(actual.username()).isEqualTo(username);
            assertThat(actual.password()).isEqualTo(password);
            assertThat(actual.role()).isNull();
            assertThat(actual.isAuthenticated()).isFalse();
        }

        @Test
        void ThenReturnsUnAuthenticatedUser_GivenParametersAreValid() {
            // When
            var actual = new UserAuthentication(userId, username, password, role, false);

            // Then
            assertThat(actual.userId()).isEqualTo(userId);
            assertThat(actual.username()).isEqualTo(username);
            assertThat(actual.password()).isEqualTo(password);
            assertThat(actual.role()).isEqualTo(role);
            assertThat(actual.isAuthenticated()).isFalse();
        }

    }

    @Nested
    class CreateAuthenticateUserWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> new UserAuthentication(null, username, password, role, true));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNull() {
            // When
            var thrown = catchThrowable(() -> new UserAuthentication(userId, null, password, role, true));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

        @Test
        void ThenReturnsUnAuthenticatedUser_GivenPasswordIsNull() {
            // When
            var actual = new UserAuthentication(userId, username, null, role, true);

            // Then
            assertThat(actual.userId()).isEqualTo(userId);
            assertThat(actual.username()).isEqualTo(username);
            assertThat(actual.password()).isNull();
            assertThat(actual.role()).isEqualTo(role);
            assertThat(actual.isAuthenticated()).isTrue();
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRoleIsNull() {
            // When
            var thrown = catchThrowable(() -> new UserAuthentication(userId, username, password, null, true));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Role must not be null");
        }

        @Test
        void ThenReturnsUnAuthenticatedUser_GivenParametersAreValid() {
            // When
            var actual = new UserAuthentication(userId, username, password, role, true);

            // Then
            assertThat(actual.userId()).isEqualTo(userId);
            assertThat(actual.username()).isEqualTo(username);
            assertThat(actual.password()).isEqualTo(password);
            assertThat(actual.role()).isEqualTo(role);
            assertThat(actual.isAuthenticated()).isTrue();
        }

    }

    @Nested
    class CreateUnAuthenticatedUser {

        @Test
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNull() {
            // When
            var thrown = catchThrowable(() -> UserAuthentication.unAuthenticated(null, password));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenPasswordIsNull() {
            // When
            var thrown = catchThrowable(() -> UserAuthentication.unAuthenticated(username, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Password must not be null");
        }

        @Test
        void ThenReturnsUnAuthenticatedUser_GivenParametersAreValid() {
            // When
            var actual = UserAuthentication.unAuthenticated(username, password);

            // Then
            assertThat(actual.userId()).isNull();
            assertThat(actual.username()).isEqualTo(username);
            assertThat(actual.password()).isEqualTo(password);
            assertThat(actual.role()).isNull();
            assertThat(actual.isAuthenticated()).isFalse();
        }

    }

    @Nested
    class CreateAuthenticatedUser {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> UserAuthentication.authenticated(null, username, role));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUsernameIsNull() {
            // When
            var thrown = catchThrowable(() -> UserAuthentication.authenticated(userId, null, role));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRoleIsNull() {
            // When
            var thrown = catchThrowable(() -> UserAuthentication.authenticated(userId, username, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Role must not be null");
        }

        @Test
        void ThenReturnsActiveUser_GivenParametersAreValid() {
            // When
            var actual = UserAuthentication.authenticated(userId, username, role);

            // Then
            assertThat(actual.userId()).isEqualTo(userId);
            assertThat(actual.username()).isEqualTo(username);
            assertThat(actual.password()).isNull();
            assertThat(actual.role()).isEqualTo(role);
        }

    }

}
