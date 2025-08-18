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

package com.bcn.asapp.tasks.task;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository that provides CRUD operations for {@link Task}.
 *
 * @author ttrigo
 * @since 0.1.0
 * @see ListCrudRepository
 */
@Repository
public interface TaskRepository extends ListCrudRepository<Task, UUID> {

    /**
     * Finds tasks with the given project id.
     *
     * @param projectId the id of the project, must not be {@literal null}.
     * @return a list with the tasks found, or empty list if none found.
     */
    List<Task> findByProjectId(UUID projectId);

    /**
     * Deletes the task with the given id.
     *
     * @param id the id of the task to be deleted, must not be {@literal null}.
     * @return the amount of deleted tasks.
     */
    @Modifying
    @Query("delete from tasks t where t.id = :id")
    Long deleteTaskById(UUID id);

}
