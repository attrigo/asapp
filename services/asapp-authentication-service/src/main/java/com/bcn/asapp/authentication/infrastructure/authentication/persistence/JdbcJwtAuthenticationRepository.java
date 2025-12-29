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

package com.bcn.asapp.authentication.infrastructure.authentication.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for JWT authentication persistence operations.
 * <p>
 * Provides database access methods for {@link JdbcJwtAuthenticationEntity} using Spring Data JDBC.
 * <p>
 * Extends {@link ListCrudRepository} to inherit standard CRUD operations.
 *
 * @since 0.2.0
 * @see ListCrudRepository
 * @author attrigo
 */
@Repository
public interface JdbcJwtAuthenticationRepository extends ListCrudRepository<JdbcJwtAuthenticationEntity, UUID> {

    /**
     * Finds a JWT authentication by its access token.
     *
     * @param accessToken the access token string
     * @return an {@link Optional} containing the {@link JdbcJwtAuthenticationEntity} if found, empty otherwise
     */
    Optional<JdbcJwtAuthenticationEntity> findByAccessTokenToken(String accessToken);

    /**
     * Finds a JWT authentication by its refresh token.
     *
     * @param refreshToken the refresh token string
     * @return an {@link Optional} containing the {@link JdbcJwtAuthenticationEntity} if found, empty otherwise
     */
    Optional<JdbcJwtAuthenticationEntity> findByRefreshTokenToken(String refreshToken);

    /**
     * Finds all JWT authentications associated with a user.
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of {@link JdbcJwtAuthenticationEntity} for the user, empty list if none found
     */
    List<JdbcJwtAuthenticationEntity> findAllByUserId(UUID userId);

    /**
     * Deletes all JWT authentications associated with a user.
     *
     * @param userId the user's unique identifier
     */
    @Modifying
    @Query("DELETE FROM jwt_authentications a WHERE a.user_id = :userId")
    void deleteAllByUserId(UUID userId);

    /**
     * Deletes all JWT authentications with refresh tokens expired before the given instant.
     *
     * @param expiredBefore the instant before which refresh tokens are considered expired
     * @return the number of deleted rows
     */
    @Modifying
    @Query("DELETE FROM jwt_authentications WHERE refresh_token_expiration < :expiredBefore")
    Integer deleteAllByRefreshTokenExpiredBefore(Instant expiredBefore);

}
