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

package com.bcn.asapp.authentication.infrastructure.user.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for user persistence operations.
 * <p>
 * Provides database access methods for {@link JdbcUserEntity} using Spring Data JDBC.
 * <p>
 * Extends {@link ListCrudRepository} to inherit standard CRUD operations.
 *
 * @since 0.2.0
 * @see ListCrudRepository
 * @author attrigo
 */
@Repository
public interface JdbcUserRepository extends ListCrudRepository<JdbcUserEntity, UUID> {

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the {@link JdbcUserEntity} if found, {@link Optional#empty} otherwise
     */
    Optional<JdbcUserEntity> findByUsername(String username);

    /**
     * Deletes a user by their unique identifier.
     *
     * @param id the user's unique identifier
     * @return the number of rows affected (0 if not found, 1 if deleted)
     */
    @Modifying
    @Query("DELETE FROM users u WHERE u.id = :id")
    Long deleteUserById(UUID id);

}
