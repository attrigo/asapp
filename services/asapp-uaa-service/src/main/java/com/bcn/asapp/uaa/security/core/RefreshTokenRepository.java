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
package com.bcn.asapp.uaa.security.core;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface that provides CRUD operations for {@link RefreshToken} entities.
 * <p>
 * Extends {@link ListCrudRepository} to inherit basic data access methods.
 *
 * @since 0.2.0
 * @author ttrigo
 */
@Repository
public interface RefreshTokenRepository extends ListCrudRepository<RefreshToken, UUID> {

    /**
     * Finds a refresh token by the associated user ID.
     *
     * @param userId the user ID to search for, must not be {@literal null}
     * @return an {@link Optional} containing the {@link RefreshToken} if found, or {@link Optional#empty} if not found
     */
    Optional<RefreshToken> findByUserId(UUID userId);

    /**
     * Determines whether a refresh token exists for the specified user ID and JWT.
     *
     * @param userId the user ID to search for, must not be {@literal null}
     * @param jwt    the JWT string associated with the refresh token, must not be {@literal null}
     * @return {@code true} if a matching refresh token exists, {@code false} otherwise
     */
    Boolean existsByUserIdAndJwt(UUID userId, String jwt);

}
