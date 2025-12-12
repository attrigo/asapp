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

/**
 * Represents a pair of JWTs (access and refresh).
 * <p>
 * This value object encapsulates both access and refresh tokens together.
 * <p>
 * It enforces structural integrity by ensuring they are both present and valid.
 *
 * @param accessToken  the access token
 * @param refreshToken the refresh token
 * @since 0.2.0
 * @author attrigo
 */
public record JwtPair(
        Jwt accessToken,
        Jwt refreshToken
) {

    /**
     * Constructs a new {@code JwtPair} instance and validates its integrity.
     *
     * @param accessToken  the access token to validate and store
     * @param refreshToken the refresh token to validate and store
     * @throws IllegalArgumentException if either token is {@code null}
     */
    public JwtPair {
        validateAccessToken(accessToken);
        validateRefreshToken(refreshToken);
    }

    /**
     * Factory method to create a new {@code JwtPair} instance.
     *
     * @param accessToken  the access token
     * @param refreshToken the refresh token
     * @return a new {@code JwtPair} instance
     * @throws IllegalArgumentException if either token is {@code null}
     */
    public static JwtPair of(Jwt accessToken, Jwt refreshToken) {
        return new JwtPair(accessToken, refreshToken);
    }

    /**
     * Validates that the access token is not {@code null}.
     *
     * @param accessToken the access token to validate
     * @throws IllegalArgumentException if the access token is {@code null}
     */
    private static void validateAccessToken(Jwt accessToken) {
        if (accessToken == null) {
            throw new IllegalArgumentException("Access token must not be null");
        }
    }

    /**
     * Validates that the refresh token is not {@code null}.
     *
     * @param refreshToken the refresh token to validate
     * @throws IllegalArgumentException if the refresh token is {@code null}
     */
    private static void validateRefreshToken(Jwt refreshToken) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token must not be null");
        }
    }

}
