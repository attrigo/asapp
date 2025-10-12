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

import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

/**
 * Represents user authentication information with state-based validation.
 * <p>
 * This value object can exist in two states: unauthenticated (with credentials) or authenticated (with user identity and role).
 * <p>
 * Field requirements vary based on the authentication state.
 *
 * @param userId          the user's unique identifier (required when authenticated)
 * @param username        the username (always required)
 * @param password        the raw password (required when unauthenticated)
 * @param role            the user's role (required when authenticated)
 * @param isAuthenticated the authentication state flag
 * @since 0.2.0
 * @author attrigo
 */
public record UserAuthentication(
        UserId userId,
        Username username,
        RawPassword password,
        Role role,
        boolean isAuthenticated
) {

    /**
     * Constructs a new {@code UserAuthentication} instance and validates its integrity.
     * <p>
     * Validation rules depend on the authentication state:
     * <ul>
     * <li>Authenticated: userId and role must not be {@code null}, username required</li>
     * <li>Unauthenticated: password and username must not be {@code null}</li>
     * </ul>
     *
     * @param userId          the user's unique identifier
     * @param username        the username
     * @param password        the raw password
     * @param role            the user's role
     * @param isAuthenticated the authentication state
     * @throws IllegalArgumentException if validation fails based on state
     */
    public UserAuthentication {
        if (isAuthenticated) {
            validateUserIdIsNotNull(userId);
            validateRoleIsNotNull(role);
        } else {
            validatePasswordIsNotNull(password);
        }
        validateUsernameIsNotNull(username);
    }

    /**
     * Factory method to create an unauthenticated {@code UserAuthentication} instance.
     * <p>
     * Used when processing authentication requests before validation.
     *
     * @param username the username
     * @param password the raw password
     * @return a new unauthenticated {@code UserAuthentication} instance
     * @throws IllegalArgumentException if username or password is {@code null}
     */
    public static UserAuthentication unAuthenticated(Username username, RawPassword password) {
        return new UserAuthentication(null, username, password, null, false);
    }

    /**
     * Factory method to create an authenticated {@code UserAuthentication} instance.
     * <p>
     * Used when authentication is successful and user identity is confirmed.
     *
     * @param userId   the user's unique identifier
     * @param username the username
     * @param role     the user's role
     * @return a new authenticated {@code UserAuthentication} instance
     * @throws IllegalArgumentException if userId, username, or role is {@code null}
     */
    public static UserAuthentication authenticated(UserId userId, Username username, Role role) {
        return new UserAuthentication(userId, username, null, role, true);
    }

    /**
     * Validates that the user ID is not {@code null}.
     *
     * @param userId the user ID to validate
     * @throws IllegalArgumentException if the user ID is {@code null}
     */
    private static void validateUserIdIsNotNull(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }

    /**
     * Validates that the username is not {@code null}.
     *
     * @param username the username to validate
     * @throws IllegalArgumentException if the username is {@code null}
     */
    private static void validateUsernameIsNotNull(Username username) {
        if (username == null) {
            throw new IllegalArgumentException("Username must not be null");
        }
    }

    /**
     * Validates that the role is not {@code null}.
     *
     * @param role the role to validate
     * @throws IllegalArgumentException if the role is {@code null}
     */
    private static void validateRoleIsNotNull(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
    }

    /**
     * Validates that the password is not {@code null}.
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if the password is {@code null}
     */
    private static void validatePasswordIsNotNull(RawPassword password) {
        if (password == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
    }

}
