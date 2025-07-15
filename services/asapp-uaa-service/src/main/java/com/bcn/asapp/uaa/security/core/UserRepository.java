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
 * Repository interface that provides CRUD operations for {@link User} entities.
 * <p>
 * Extends {@link ListCrudRepository} to inherit basic data access methods.
 *
 * @since 0.2.0
 * @author ttrigo
 */
@Repository
public interface UserRepository extends ListCrudRepository<User, UUID> {

    /**
     * Finds a user by their username.
     *
     * @param username the username of the user, must not be {@literal null}
     * @return {@link Optional} containing the {@link User} found, or {@link Optional#empty} if no user exists with the given username
     */
    Optional<User> findByUsername(String username);

    @Modifying
    @Query("DELETE FROM user u WHERE u.user_id = :id")
    Long deleteUserById(UUID id);

}
