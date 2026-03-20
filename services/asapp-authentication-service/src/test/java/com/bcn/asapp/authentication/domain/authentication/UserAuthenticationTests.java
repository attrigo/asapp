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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

/**
 * Tests {@link UserAuthentication} state factories, required fields, and role assignment.
 * <p>
 * Coverage:
 * <li>Creates unauthenticated user with username and password, null ID and role</li>
 * <li>Creates authenticated user with ID, username, and role, null password</li>
 * <li>Validates username required for both authenticated and unauthenticated states</li>
 * <li>Validates password required for unauthenticated state</li>
 * <li>Validates ID and role required for authenticated state</li>
 * <li>Provides factory methods for unauthenticated and authenticated states</li>
 */
class UserAuthenticationTests {

    @Nested
    class CreateUnAuthenticatedUserWithConstructor {

        @Test
        void ReturnsUnAuthenticatedUser_ValidParameters() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");
            var role = USER;

            // When
            var actual = new UserAuthentication(userId, username, password, role, false);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.userId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.username()).as("username").isEqualTo(username);
                softly.assertThat(actual.password()).as("password").isEqualTo(password);
                softly.assertThat(actual.role()).as("role").isEqualTo(role);
                softly.assertThat(actual.isAuthenticated()).as("is authenticated").isFalse();
                // @formatter:on
            });
        }

        @Test
        void ReturnsUnAuthenticatedUser_NullId() {
            // Given
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");
            var role = USER;

            // When
            var actual = new UserAuthentication(null, username, password, role, false);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.userId()).as("user ID").isNull();
                softly.assertThat(actual.username()).as("username").isEqualTo(username);
                softly.assertThat(actual.password()).as("password").isEqualTo(password);
                softly.assertThat(actual.role()).as("role").isEqualTo(role);
                softly.assertThat(actual.isAuthenticated()).as("is authenticated").isFalse();
                // @formatter:on
            });
        }

        @Test
        void ReturnsUnAuthenticatedUser_NullRole() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");

            // When
            var actual = new UserAuthentication(userId, username, password, null, false);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.userId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.username()).as("username").isEqualTo(username);
                softly.assertThat(actual.password()).as("password").isEqualTo(password);
                softly.assertThat(actual.role()).as("role").isNull();
                softly.assertThat(actual.isAuthenticated()).as("is authenticated").isFalse();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullPassword() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var username = Username.of("user@asapp.com");
            var role = USER;

            // When
            var actual = catchThrowable(() -> new UserAuthentication(userId, username, null, role, false));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Password must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullUsername() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var password = RawPassword.of("TEST@09_password?!");
            var role = USER;

            // When
            var actual = catchThrowable(() -> new UserAuthentication(userId, null, password, role, false));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

    }

    @Nested
    class CreateAuthenticatedUserWithConstructor {

        @Test
        void ReturnsAuthenticatedUser_ValidParameters() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");
            var role = USER;

            // When
            var actual = new UserAuthentication(userId, username, password, role, true);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.userId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.username()).as("username").isEqualTo(username);
                softly.assertThat(actual.password()).as("password").isEqualTo(password);
                softly.assertThat(actual.role()).as("role").isEqualTo(role);
                softly.assertThat(actual.isAuthenticated()).as("is authenticated").isTrue();
                // @formatter:on
            });
        }

        @Test
        void ReturnsAuthenticatedUser_NullPassword() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var username = Username.of("user@asapp.com");
            var role = USER;

            // When
            var actual = new UserAuthentication(userId, username, null, role, true);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.userId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.username()).as("username").isEqualTo(username);
                softly.assertThat(actual.password()).as("password").isNull();
                softly.assertThat(actual.role()).as("role").isEqualTo(role);
                softly.assertThat(actual.isAuthenticated()).as("is authenticated").isTrue();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // Given
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");
            var role = USER;

            // When
            var actual = catchThrowable(() -> new UserAuthentication(null, username, password, role, true));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullRole() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");

            // When
            var actual = catchThrowable(() -> new UserAuthentication(userId, username, password, null, true));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Role must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullUsername() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var password = RawPassword.of("TEST@09_password?!");
            var role = USER;

            // When
            var actual = catchThrowable(() -> new UserAuthentication(userId, null, password, role, true));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

    }

    @Nested
    class CreateUnAuthenticatedUser {

        @Test
        void ReturnsUnAuthenticatedUser_ValidParameters() {
            // Given
            var username = Username.of("user@asapp.com");
            var password = RawPassword.of("TEST@09_password?!");

            // When
            var actual = UserAuthentication.unAuthenticated(username, password);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.userId()).as("user ID").isNull();
                softly.assertThat(actual.username()).as("username").isEqualTo(username);
                softly.assertThat(actual.password()).as("password").isEqualTo(password);
                softly.assertThat(actual.role()).as("role").isNull();
                softly.assertThat(actual.isAuthenticated()).as("is authenticated").isFalse();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullPassword() {
            // Given
            var username = Username.of("user@asapp.com");

            // When
            var actual = catchThrowable(() -> UserAuthentication.unAuthenticated(username, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Password must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullUsername() {
            // Given
            var password = RawPassword.of("TEST@09_password?!");

            // When
            var actual = catchThrowable(() -> UserAuthentication.unAuthenticated(null, password));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

    }

    @Nested
    class CreateAuthenticatedUser {

        @Test
        void ReturnsActiveUser_ValidParameters() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var username = Username.of("user@asapp.com");
            var role = USER;

            // When
            var actual = UserAuthentication.authenticated(userId, username, role);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.userId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.username()).as("username").isEqualTo(username);
                softly.assertThat(actual.password()).as("password").isNull();
                softly.assertThat(actual.role()).as("role").isEqualTo(role);
                softly.assertThat(actual.isAuthenticated()).as("is authenticated").isTrue();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // Given
            var username = Username.of("user@asapp.com");
            var role = USER;

            // When
            var actual = catchThrowable(() -> UserAuthentication.authenticated(null, username, role));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullRole() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var username = Username.of("user@asapp.com");

            // When
            var actual = catchThrowable(() -> UserAuthentication.authenticated(userId, username, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Role must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullUsername() {
            // Given
            var userId = UserId.of(UUID.fromString("7f3d8e2a-91c4-4b6f-a8d2-5e9f1c3b7a4d"));
            var role = USER;

            // When
            var actual = catchThrowable(() -> UserAuthentication.authenticated(userId, null, role));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Username must not be null");
        }

    }

}
