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

package com.attrigo.asapp.authentication.infrastructure.authentication.out;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.attrigo.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.attrigo.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.attrigo.asapp.authentication.domain.authentication.EncodedToken;
import com.attrigo.asapp.authentication.domain.authentication.JwtAuthentication;
import com.attrigo.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.attrigo.asapp.authentication.domain.user.UserId;
import com.attrigo.asapp.authentication.infrastructure.authentication.mapper.JwtAuthenticationMapper;
import com.attrigo.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationEntity;
import com.attrigo.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationRepository;

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

    @Override
    public JwtAuthentication findByAccessToken(EncodedToken accessToken) {
        logger.trace("[JWT_AUTH_REPOSITORY] Finding authentication by access token");
        return jwtAuthenticationRepository.findByAccessTokenToken(accessToken.value())
                                          .map(jwtAuthenticationMapper::toJwtAuthentication)
                                          .orElseThrow(
                                                  () -> new AuthenticationNotFoundException("Authentication session not found in repository for access token"));
    }

    @Override
    public JwtAuthentication findByRefreshToken(EncodedToken refreshToken) {
        logger.trace("[JWT_AUTH_REPOSITORY] Finding authentication by refresh token");
        return jwtAuthenticationRepository.findByRefreshTokenToken(refreshToken.value())
                                          .map(jwtAuthenticationMapper::toJwtAuthentication)
                                          .orElseThrow(() -> new AuthenticationNotFoundException(
                                                  "Authentication session not found in repository for refresh token"));
    }

    @Override
    public List<JwtAuthentication> findAllByUserId(UserId userId) {
        logger.trace("[JWT_AUTH_REPOSITORY] Finding all authentications by userId={}", userId.value());
        return jwtAuthenticationRepository.findAllByUserId(userId.value())
                                          .stream()
                                          .map(jwtAuthenticationMapper::toJwtAuthentication)
                                          .toList();
    }

    @Override
    public JwtAuthentication save(JwtAuthentication jwtAuthentication) {
        logger.trace("[JWT_AUTH_REPOSITORY] Saving authentication");
        var jwtAuthenticationToSave = jwtAuthenticationMapper.toJdbcJwtAuthenticationEntity(jwtAuthentication);

        var jwtAuthenticationSaved = jwtAuthenticationRepository.save(jwtAuthenticationToSave);

        return jwtAuthenticationMapper.toJwtAuthentication(jwtAuthenticationSaved);
    }

    @Override
    public void deleteById(JwtAuthenticationId jwtAuthenticationId) {
        logger.trace("[JWT_AUTH_REPOSITORY] Deleting authentication by authenticationId={}", jwtAuthenticationId.value());
        jwtAuthenticationRepository.deleteById(jwtAuthenticationId.value());
    }

    @Override
    public void deleteAllByUserId(UserId userId) {
        logger.trace("[JWT_AUTH_REPOSITORY] Deleting all authentications by userId={}", userId.value());
        jwtAuthenticationRepository.deleteAllByUserId(userId.value());
    }

    @Override
    public Integer deleteAllByRefreshTokenExpiredBefore(Instant expiredBefore) {
        logger.trace("[JWT_AUTH_REPOSITORY] Deleting all authentications with expired refresh tokens");
        return jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);
    }

}
