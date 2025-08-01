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

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface that provides CRUD operations for {@link AccessToken} entities.
 * <p>
 * This interface extends {@link ListCrudRepository} to inherit basic data access methods for handling {@link AccessToken} entities, such as saving, deleting,
 * and finding tokens.
 *
 * @since 0.2.0
 * @author ttrigo
 */
@Repository
public interface AccessTokenRepository extends ListCrudRepository<AccessToken, UUID> {

    /**
     * Finds an access token by the associated user ID.
     *
     * @param userId the user ID to search for, must not be {@literal null}
     * @return an {@link Optional} containing the {@link AccessToken} if found, or {@link Optional#empty} if not found
     * @throws IllegalArgumentException if {@code userId} is {@literal null}
     */
    Optional<AccessToken> findByUserId(UUID userId);

    /**
     * Determines whether an access token exists for the specified user ID and JWT.
     *
     * @param userId the user ID to search for, must not be {@literal null}
     * @param jwt    the JWT string associated with the access token, must not be {@literal null}
     * @return {@code true} if a matching access token exists, {@code false} otherwise
     * @throws IllegalArgumentException if either {@code userId} or {@code jwt} is {@literal null}
     */
    Boolean existsByUserIdAndJwt(UUID userId, String jwt);

    /**
     * Deletes all access tokens associated with a specific user ID.
     *
     * @param userId the ID of the user whose access tokens should be deleted, must not be {@literal null}
     * @throws IllegalArgumentException if {@code userId} is {@literal null}
     */
    @Modifying
    @Query("DELETE FROM Access_Token a WHERE a.user_id = :userId")
    void deleteByUserId(UUID userId);

}
