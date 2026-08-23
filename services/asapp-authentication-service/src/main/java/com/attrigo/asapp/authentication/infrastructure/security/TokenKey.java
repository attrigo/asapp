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

package com.attrigo.asapp.authentication.infrastructure.security;

import org.springframework.util.Assert;

import com.attrigo.asapp.authentication.domain.authentication.EncodedToken;
import com.attrigo.asapp.authentication.domain.authentication.Jwt;
import com.attrigo.asapp.authentication.domain.authentication.JwtType;

/**
 * Represents the key a token is stored under: the encoded token and the type it is namespaced under.
 * <p>
 * Carrying the type with the token makes the order tokens are handed to the store irrelevant, as each one resolves its own key.
 *
 * @param type  the type the token is namespaced under
 * @param token the encoded JWT
 * @since 0.5.0
 * @author attrigo
 */
public record TokenKey(
        JwtType type,
        EncodedToken token
) {

    /**
     * Redis key prefix for access tokens.
     * <p>
     * Used to namespace access tokens in Redis to avoid key collisions and enable efficient key scanning if needed.
     */
    public static final String ACCESS_TOKEN_PREFIX = "jwt:access_token:";

    /**
     * Redis key prefix for refresh tokens.
     * <p>
     * Used to namespace refresh tokens in Redis to avoid key collisions and enable efficient key scanning if needed.
     */
    public static final String REFRESH_TOKEN_PREFIX = "jwt:refresh_token:";

    /**
     * Constructs a new {@code TokenKey} instance and validates its integrity.
     * <p>
     * Validates that all required fields are present.
     *
     * @param type  the type the token is namespaced under
     * @param token the encoded JWT
     * @throws IllegalArgumentException if any validation fails
     */
    public TokenKey {
        Assert.notNull(type, "Token type must not be null");
        Assert.notNull(token, "Token must not be null");
    }

    /**
     * Factory method to create a new {@code TokenKey} instance from a JWT.
     * <p>
     * Reads the type from the JWT itself, so a token cannot be namespaced under the wrong one.
     *
     * @param jwt the JWT to key
     * @return a new {@code TokenKey} instance
     */
    public static TokenKey of(Jwt jwt) {
        Assert.notNull(jwt, "JWT must not be null");
        return new TokenKey(jwt.type(), jwt.encodedToken());
    }

    /**
     * Factory method to create a new {@code TokenKey} instance for an access token.
     *
     * @param accessToken the encoded access token
     * @return a new {@code TokenKey} instance
     * @throws IllegalArgumentException if any validation fails
     */
    public static TokenKey ofAccessToken(EncodedToken accessToken) {
        return new TokenKey(JwtType.ACCESS_TOKEN, accessToken);
    }

    /**
     * Factory method to create a new {@code TokenKey} instance for a refresh token.
     *
     * @param refreshToken the encoded refresh token
     * @return a new {@code TokenKey} instance
     * @throws IllegalArgumentException if any validation fails
     */
    public static TokenKey ofRefreshToken(EncodedToken refreshToken) {
        return new TokenKey(JwtType.REFRESH_TOKEN, refreshToken);
    }

    /**
     * Returns the namespaced key value the token is stored under.
     *
     * @return the prefixed token key value
     */
    public String value() {
        return switch (this.type) {
        case ACCESS_TOKEN -> ACCESS_TOKEN_PREFIX + this.token.value();
        case REFRESH_TOKEN -> REFRESH_TOKEN_PREFIX + this.token.value();
        };
    }

}
