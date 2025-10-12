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

package com.bcn.asapp.authentication.domain.user;

import java.util.Objects;

/**
 * Represents a user entity within the authentication system.
 * <p>
 * This aggregate root encapsulates user identity, credentials, and role information.
 * <p>
 * Users can exist in two states: inactive (transient, without ID) and active (persistent, with ID).
 * <p>
 * Equality is based on username for inactive users and ID for active users.
 *
 * @since 0.2.0
 * @author attrigo
 */
public final class User {

    private final UserId id;

    private Username username;

    private EncodedPassword password;

    private Role role;

    /**
     * Constructor a new inactive {@code User} instance and validates its integrity.
     *
     * @param username the user's username
     * @param password the user's encoded password
     * @param role     the user's role
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    private User(Username username, EncodedPassword password, Role role) {
        validateUsernameIsNotNull(username);
        validatePasswordIsNotNull(password);
        validateRoleIsNotNull(role);
        this.id = null;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /**
     * Constructor a new active {@code User} instance and validates its integrity.
     *
     * @param id       the user's unique identifier
     * @param username the user's username
     * @param role     the user's role
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    private User(UserId id, Username username, Role role) {
        validateIdIsNotNull(id);
        validateUsernameIsNotNull(username);
        validateRoleIsNotNull(role);
        this.id = id;
        this.username = username;
        this.password = null;
        this.role = role;
    }

    /**
     * Factory method to create an inactive user without a persistent ID.
     * <p>
     * Typically used when registering a new user before persistence.
     *
     * @param username the user's username
     * @param password the user's encoded password
     * @param role     the user's role
     * @return a new inactive {@code User} instance
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public static User inactiveUser(Username username, EncodedPassword password, Role role) {
        return new User(username, password, role);
    }

    /**
     * Factory method to create an active user with a persistent ID.
     * <p>
     * Typically used when reconstituting a user from the database.
     *
     * @param id       the user's unique identifier
     * @param username the user's username
     * @param role     the user's role
     * @return a new active {@code User} instance
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public static User activeUser(UserId id, Username username, Role role) {
        return new User(id, username, role);
    }

    /**
     * Updates the user's information.
     * <p>
     * For active users (with ID), the password is also updated.
     * <p>
     * For inactive users, the password is ignored as it's already set during creation.
     *
     * @param username the new username
     * @param password the new encoded password
     * @param role     the new role
     * @throws IllegalArgumentException if username or role is {@code null}, or if password is {@code null} for active users
     */
    public void update(Username username, EncodedPassword password, Role role) {
        validateUsernameIsNotNull(username);
        validateRoleIsNotNull(role);
        this.username = username;
        this.role = role;
        if (this.id != null) {
            validatePasswordIsNotNull(password);
            this.password = password;
        }
    }

    /**
     * Determines equality based on user state.
     * <p>
     * Inactive users are equal if their usernames match.
     * <p>
     * Active users are equal if their IDs match.
     *
     * @param object the object to compare
     * @return {@code true} if equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        User other = (User) object;
        if (this.id == null) {
            return Objects.equals(this.username, other.username);
        } else {
            return Objects.equals(this.id, other.id);
        }
    }

    /**
     * Generates hash code based on user state.
     * <p>
     * Uses username for inactive users and ID for active users.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        if (this.id == null) {
            return Objects.hashCode(this.username);
        } else {
            return Objects.hashCode(this.id);
        }
    }

    /**
     * Returns the user's unique identifier.
     *
     * @return the {@link UserId}, or {@code null} for inactive users
     */
    public UserId getId() {
        return this.id;
    }

    /**
     * Returns the user's username.
     *
     * @return the {@link Username}
     */
    public Username getUsername() {
        return this.username;
    }

    /**
     * Returns the user's encoded password.
     *
     * @return the {@link EncodedPassword}, or {@code null} for active users
     */
    public EncodedPassword getPassword() {
        return this.password;
    }

    /**
     * Returns the user's role.
     *
     * @return the {@link Role}
     */
    public Role getRole() {
        return this.role;
    }

    /**
     * Validates that the user ID is not {@code null}.
     *
     * @param id the ID to validate
     * @throws IllegalArgumentException if the ID is {@code null}
     */
    private static void validateIdIsNotNull(UserId id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
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
     * Validates that the password is not {@code null}.
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if the password is {@code null}
     */
    private static void validatePasswordIsNotNull(EncodedPassword password) {
        if (password == null) {
            throw new IllegalArgumentException("Password must not be null");
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

}
