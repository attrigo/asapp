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

package com.bcn.asapp.uaa.infrastructure.authentication.out;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.uaa.domain.authentication.EncodedToken;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.uaa.domain.user.UserId;
import com.bcn.asapp.uaa.infrastructure.authentication.mapper.JwtAuthenticationMapper;

/**
 * Adapter implementation of {@link JwtAuthenticationRepository} for JDBC persistence.
 * <p>
 * Bridges the application layer with the infrastructure layer by translating domain operations to JDBC repository calls and mapping between domain entities and
 * database entities.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class JwtAuthenticationRepositoryAdapter implements JwtAuthenticationRepository {

    private final JwtAuthenticationJdbcRepository jwtAuthenticationRepository;

    private final JwtAuthenticationMapper jwtAuthenticationMapper;

    /**
     * Constructs a new {@code JwtAuthenticationRepositoryAdapter} with required dependencies.
     *
     * @param jwtAuthenticationRepository the Spring Data JDBC repository
     * @param jwtAuthenticationMapper     the mapper for converting between domain and database entities
     */
    public JwtAuthenticationRepositoryAdapter(JwtAuthenticationJdbcRepository jwtAuthenticationRepository, JwtAuthenticationMapper jwtAuthenticationMapper) {
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
        this.jwtAuthenticationMapper = jwtAuthenticationMapper;
    }

    /**
     * Finds a JWT authentication by its access token.
     *
     * @param accessToken the encoded access token
     * @return an {@link Optional} containing the {@link JwtAuthentication} if found, empty otherwise
     */
    @Override
    public Optional<JwtAuthentication> findByAccessToken(EncodedToken accessToken) {
        return jwtAuthenticationRepository.findByAccessTokenToken(accessToken.value())
                                          .map(jwtAuthenticationMapper::toJwtAuthentication);
    }

    /**
     * Finds a JWT authentication by its refresh token.
     *
     * @param refreshToken the encoded refresh token
     * @return an {@link Optional} containing the {@link JwtAuthentication} if found, empty otherwise
     */
    @Override
    public Optional<JwtAuthentication> findByRefreshToken(EncodedToken refreshToken) {
        return jwtAuthenticationRepository.findByRefreshTokenToken(refreshToken.value())
                                          .map(jwtAuthenticationMapper::toJwtAuthentication);
    }

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
    @Override
    public JwtAuthentication save(JwtAuthentication jwtAuthentication) {
        var jwtAuthenticationToSave = jwtAuthenticationMapper.toJwtAuthenticationEntity(jwtAuthentication);

        var jwtAuthenticationSaved = jwtAuthenticationRepository.save(jwtAuthenticationToSave);

        return jwtAuthenticationMapper.toJwtAuthentication(jwtAuthenticationSaved);
    }

    /**
     * Deletes a JWT authentication by its unique identifier.
     *
     * @param jwtAuthenticationId the JWT authentication's unique identifier
     */
    @Override
    public void deleteById(JwtAuthenticationId jwtAuthenticationId) {
        jwtAuthenticationRepository.deleteById(jwtAuthenticationId.value());
    }

    /**
     * Deletes all JWT authentications associated with a user.
     *
     * @param userId the user's unique identifier
     */
    @Override
    public void deleteAllByUserId(UserId userId) {
        jwtAuthenticationRepository.deleteAllJwtAuthenticationByUserId(userId.value());
    }

}
