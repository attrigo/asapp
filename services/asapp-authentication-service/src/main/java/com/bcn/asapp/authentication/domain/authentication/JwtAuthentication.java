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

import java.util.Objects;

import com.bcn.asapp.authentication.domain.user.UserId;

/**
 * Represents a JWT authentication entity within the authentication system.
 * <p>
 * This aggregate root encapsulates a user's authentication session with access and refresh tokens.
 * <p>
 * JWT authentications can exist in two states: unauthenticated (transient, without ID) and authenticated (persistent, with ID).
 * <p>
 * Equality is based on ID; unauthenticated instances are not considered equal to any other instance.
 *
 * @since 0.2.0
 * @author attrigo
 */
public final class JwtAuthentication {

    private final JwtAuthenticationId id;

    private final UserId userId;

    private JwtPair jwtPair;

    /**
     * Constructs a new unauthenticated {@code JwtAuthentication} instance and validates its integrity.
     *
     * @param userId  the JWT authentication's user unique identifier
     * @param jwtPair the JWT pair
     * @throws IllegalArgumentException if userId or jwtPair is {@code null}
     */
    private JwtAuthentication(UserId userId, JwtPair jwtPair) {
        validateUserIdIsNotNull(userId);
        validateJwtPairIsNotNull(jwtPair);
        this.id = null;
        this.userId = userId;
        this.jwtPair = jwtPair;
    }

    /**
     * Constructs a new authenticated {@code JwtAuthentication} instance and validates its integrity.
     *
     * @param id      the JWT authentication's unique identifier
     * @param userId  the JWT authentication's user unique identifier
     * @param jwtPair the JWT pair
     * @throws IllegalArgumentException if id, userId, or jwtPair is {@code null}
     */
    private JwtAuthentication(JwtAuthenticationId id, UserId userId, JwtPair jwtPair) {
        validateIdIsNotNull(id);
        validateUserIdIsNotNull(userId);
        validateJwtPairIsNotNull(jwtPair);
        this.id = id;
        this.userId = userId;
        this.jwtPair = jwtPair;
    }

    /**
     * Factory method to create an unauthenticated JWT authentication without a persistent ID.
     * <p>
     * Typically used when creating a new authentication session before persistence.
     *
     * @param userId  the JWT authentication's user unique identifier
     * @param jwtPair the JWT pair containing both access and refresh tokens
     * @return a new unauthenticated {@code JwtAuthentication} instance
     * @throws IllegalArgumentException if userId or jwtPair is {@code null}
     */
    public static JwtAuthentication unAuthenticated(UserId userId, JwtPair jwtPair) {
        return new JwtAuthentication(userId, jwtPair);
    }

    /**
     * Factory method to create an authenticated JWT authentication with a persistent ID.
     * <p>
     * Typically used when reconstituting an authentication session from the repository.
     *
     * @param id      the JWT authentication's unique identifier
     * @param userId  the JWT authentication's user unique identifier
     * @param jwtPair the JWT pair containing both access and refresh tokens
     * @return a new authenticated {@code JwtAuthentication} instance
     * @throws IllegalArgumentException if id, userId, or jwtPair is {@code null}
     */
    public static JwtAuthentication authenticated(JwtAuthenticationId id, UserId userId, JwtPair jwtPair) {
        return new JwtAuthentication(id, userId, jwtPair);
    }

    /**
     * Returns the access token.
     *
     * @return the {@link Jwt} access token
     */
    public Jwt accessToken() {
        return this.jwtPair.accessToken();
    }

    /**
     * Returns the refresh token.
     *
     * @return the {@link Jwt} refresh token
     */
    public Jwt refreshToken() {
        return this.jwtPair.refreshToken();
    }

    /**
     * Refreshes the JWT pair with a new pair.
     *
     * @param jwtPair the new JWT pair containing both access and refresh tokens
     * @throws IllegalArgumentException if jwtPair is {@code null} or contains invalid tokens
     */
    public void refreshTokens(JwtPair jwtPair) {
        validateJwtPairIsNotNull(jwtPair);
        this.jwtPair = jwtPair;
    }

    /**
     * Determines equality based on JWT authentication ID.
     * <p>
     * Two {@code JwtAuthentication} instances are equal only if both have non-null IDs that match.
     * <p>
     * Unauthenticated instances are never equal to any other instance.
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
        JwtAuthentication other = (JwtAuthentication) object;
        if (this.id == null || other.id == null) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    /**
     * Generates hash code based on JWT authentication ID.
     * <p>
     * Uses ID for authenticated instances and identity hash code for unauthenticated instances.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return this.id != null ? Objects.hashCode(this.id) : System.identityHashCode(this);
    }

    /**
     * Returns the JWT authentication's unique identifier.
     *
     * @return the {@link JwtAuthenticationId}, or {@code null} for unauthenticated instances
     */
    public JwtAuthenticationId getId() {
        return this.id;
    }

    /**
     * Returns the JWT authentication's user unique identifier.
     *
     * @return the {@link UserId}
     */
    public UserId getUserId() {
        return this.userId;
    }

    /**
     * Returns the JWT pair.
     *
     * @return the {@link JwtPair} containing access and refresh tokens
     */
    public JwtPair getJwtPair() {
        return this.jwtPair;
    }

    /**
     * Validates that the JWT authentication ID is not {@code null}.
     *
     * @param id the ID to validate
     * @throws IllegalArgumentException if the ID is {@code null}
     */
    private static void validateIdIsNotNull(JwtAuthenticationId id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
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
     * Validates that the JWT pair is not {@code null}.
     *
     * @param jwtPair the JWT pair to validate
     * @throws IllegalArgumentException if the JWT pair is {@code null}
     */
    private static void validateJwtPairIsNotNull(JwtPair jwtPair) {
        if (jwtPair == null) {
            throw new IllegalArgumentException("JWT pair must not be null");
        }
    }

}
