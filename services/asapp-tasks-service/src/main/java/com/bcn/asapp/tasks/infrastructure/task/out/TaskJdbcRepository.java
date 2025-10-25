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

package com.bcn.asapp.tasks.infrastructure.task.out;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import com.bcn.asapp.tasks.infrastructure.task.out.entity.TaskEntity;

/**
 * Spring Data JDBC repository for task persistence operations.
 * <p>
 * Provides database access methods for {@link TaskEntity} using Spring Data JDBC.
 * <p>
 * Extends {@link ListCrudRepository} to inherit standard CRUD operations.
 *
 * @since 0.2.0
 * @see ListCrudRepository
 * @author attrigo
 */
@Repository
public interface TaskJdbcRepository extends ListCrudRepository<TaskEntity, UUID> {

    /**
     * Finds all task entities by their user's unique identifier.
     *
     * @param userId the user's unique identifier
     * @return a {@link Collection} of {@link TaskEntity} entities belonging to the user
     */
    Collection<TaskEntity> findByUserId(UUID userId);

    /**
     * Deletes a task by their unique identifier.
     *
     * @param id the task's unique identifier
     * @return the number of rows affected (0 if not found, 1 if deleted)
     */
    @Modifying
    @Query("DELETE FROM tasks u WHERE u.id = :id")
    Long deleteTaskById(UUID id);

}
