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

package com.bcn.asapp.authentication.application.authentication.out;

import java.util.Optional;

import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.user.UserId;

/**
 * Repository port for JWT authentication persistence operations.
 * <p>
 * Defines the contract for storing and retrieving {@link JwtAuthentication} entities.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface JwtAuthenticationRepository {

    /**
     * Finds a JWT authentication by its access token.
     *
     * @param accessToken the encoded access token
     * @return an {@link Optional} containing the {@link JwtAuthentication} if found, {@link Optional#empty} otherwise
     */
    Optional<JwtAuthentication> findByAccessToken(EncodedToken accessToken);

    /**
     * Finds a JWT authentication by its refresh token.
     *
     * @param refreshToken the encoded refresh token
     * @return an {@link Optional} containing the {@link JwtAuthentication} if found, {@link Optional#empty} otherwise
     */
    Optional<JwtAuthentication> findByRefreshToken(EncodedToken refreshToken);

    /**
     * Saves a JWT authentication to the repository.
     * <p>
     * If the authentication is unauthenticated (without ID), it will be persisted and returned with a generated ID.
     * <p>
     * If the authentication is authenticated (with ID), it will be updated.
     *
     * @param jwtAuthentication the {@link JwtAuthentication} to save
     * @return the saved {@link JwtAuthentication} with a persistent ID
     */
    JwtAuthentication save(JwtAuthentication jwtAuthentication);

    /**
     * Deletes a JWT authentication by its unique identifier.
     *
     * @param jwtAuthenticationId the JWT authentication's unique identifier
     */
    void deleteById(JwtAuthenticationId jwtAuthenticationId);

    /**
     * Deletes all JWT authentications associated with a user.
     *
     * @param userId the user's unique identifier
     */
    void deleteAllByUserId(UserId userId);

}
