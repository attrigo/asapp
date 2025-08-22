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
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import com.bcn.asapp.uaa.infrastructure.authentication.out.entity.JwtAuthenticationEntity;

/**
 * Spring Data JDBC repository for JWT authentication persistence operations.
 * <p>
 * Provides database access methods for {@link JwtAuthenticationEntity} using Spring Data JDBC.
 * <p>
 * Extends {@link ListCrudRepository} to inherit standard CRUD operations.
 *
 * @since 0.2.0
 * @see ListCrudRepository
 * @author attrigo
 */
@Repository
public interface JwtAuthenticationJdbcRepository extends ListCrudRepository<JwtAuthenticationEntity, UUID> {

    /**
     * Finds a JWT authentication by its access token.
     *
     * @param accessToken the access token string
     * @return an {@link Optional} containing the {@link JwtAuthenticationEntity} if found, empty otherwise
     */
    Optional<JwtAuthenticationEntity> findByAccessTokenToken(String accessToken);

    /**
     * Finds a JWT authentication by its refresh token.
     *
     * @param refreshToken the refresh token string
     * @return an {@link Optional} containing the {@link JwtAuthenticationEntity} if found, empty otherwise
     */
    Optional<JwtAuthenticationEntity> findByRefreshTokenToken(String refreshToken);

    /**
     * Deletes all JWT authentications associated with a user.
     *
     * @param userId the user's unique identifier
     */
    @Modifying
    @Query("DELETE FROM jwt_authentications a WHERE a.user_id = :userId")
    void deleteAllJwtAuthenticationByUserId(UUID userId);

}
