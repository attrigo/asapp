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

package com.bcn.asapp.authentication.infrastructure.authentication.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.AuthenticationPersistenceException;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.infrastructure.authentication.mapper.JwtAuthenticationMapper;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationRepository;

/**
 * Adapter implementation of {@link JwtAuthenticationRepository} for JDBC persistence.
 * <p>
 * Bridges the application layer with the infrastructure layer by translating domain operations to JDBC repository calls and mapping between domain entities and
 * database entities.
 * <p>
 * This adapter performs type translation by converting domain aggregates ({@link JwtAuthentication}) to database entities ({@link JdbcJwtAuthenticationEntity})
 * for persistence operations, and vice versa for retrieval operations, using {@link JwtAuthenticationMapper} to maintain separation between domain and
 * infrastructure concerns.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class JwtAuthenticationRepositoryAdapter implements JwtAuthenticationRepository {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationRepositoryAdapter.class);

    private final JdbcJwtAuthenticationRepository jwtAuthenticationRepository;

    private final JwtAuthenticationMapper jwtAuthenticationMapper;

    /**
     * Constructs a new {@code JwtAuthenticationRepositoryAdapter} with required dependencies.
     *
     * @param jwtAuthenticationRepository the Spring Data JDBC repository
     * @param jwtAuthenticationMapper     the mapper for converting between domain and database entities
     */
    public JwtAuthenticationRepositoryAdapter(JdbcJwtAuthenticationRepository jwtAuthenticationRepository, JwtAuthenticationMapper jwtAuthenticationMapper) {
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
        logger.trace("[JWT_AUTH_REPOSITORY] Finding authentication by access token");
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
        logger.trace("[JWT_AUTH_REPOSITORY] Finding authentication by refresh token");
        return jwtAuthenticationRepository.findByRefreshTokenToken(refreshToken.value())
                                          .map(jwtAuthenticationMapper::toJwtAuthentication);
    }

    /**
     * Finds all JWT authentications associated with a user.
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of {@link JwtAuthentication} for the user, empty list if none found
     */
    @Override
    public List<JwtAuthentication> findAllByUserId(UserId userId) {
        logger.trace("[JWT_AUTH_REPOSITORY] Finding all authentications by user ID");
        return jwtAuthenticationRepository.findAllByUserId(userId.value())
                                          .stream()
                                          .map(jwtAuthenticationMapper::toJwtAuthentication)
                                          .toList();
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
        logger.trace("[JWT_AUTH_REPOSITORY] Saving authentication");
        var jwtAuthenticationToSave = jwtAuthenticationMapper.toJdbcJwtAuthenticationEntity(jwtAuthentication);

        var jwtAuthenticationSaved = jwtAuthenticationRepository.save(jwtAuthenticationToSave);

        return jwtAuthenticationMapper.toJwtAuthentication(jwtAuthenticationSaved);
    }

    /**
     * Deletes a JWT authentication by its unique identifier.
     *
     * @param jwtAuthenticationId the JWT authentication's unique identifier
     * @throws AuthenticationPersistenceException if the database operation fails
     */
    @Override
    public void deleteById(JwtAuthenticationId jwtAuthenticationId) {
        logger.trace("[JWT_AUTH_REPOSITORY] Deleting authentication by ID");
        try {
            jwtAuthenticationRepository.deleteById(jwtAuthenticationId.value());
        } catch (DataAccessException e) {
            throw new AuthenticationPersistenceException("Could not delete authentication from repository", e);
        }
    }

    /**
     * Deletes all JWT authentications associated with a user.
     *
     * @param userId the user's unique identifier
     */
    @Override
    public void deleteAllByUserId(UserId userId) {
        logger.trace("[JWT_AUTH_REPOSITORY] Deleting all authentications by user ID");
        jwtAuthenticationRepository.deleteAllJwtAuthenticationByUserId(userId.value());
    }

    /**
     * Deletes all JWT authentications with refresh tokens expired before the given instant.
     *
     * @param expiredBefore the instant before which refresh tokens are considered expired
     * @return the number of deleted authentications
     */
    @Override
    public Integer deleteAllByRefreshTokenExpiredBefore(Instant expiredBefore) {
        logger.trace("[JWT_AUTH_REPOSITORY] Deleting all authentications with expired refresh tokens");
        return jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);
    }

}
