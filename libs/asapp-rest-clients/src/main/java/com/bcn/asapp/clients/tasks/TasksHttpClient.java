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

package com.bcn.asapp.clients.tasks;

import static com.bcn.asapp.url.tasks.TaskRestAPIURL.TASKS_GET_BY_USER_ID_FULL_PATH;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import com.bcn.asapp.clients.tasks.response.TasksByUserIdResponse;

/**
 * Declares the HTTP contract for retrieving task data from the Tasks Service.
 * <p>
 * Defines the HTTP contract for retrieving task data from the Tasks Service as a Spring {@link HttpExchange} interface. A client proxy is created and
 * configured by the consuming service, which owns base-url resolution, authentication propagation, and load balancing.
 *
 * @since 0.4.0
 * @see HttpExchange
 * @author attrigo
 */
@HttpExchange
public interface TasksHttpClient {

    /**
     * Retrieves all tasks associated with a specific user.
     *
     * @param id the unique identifier of the user whose tasks should be retrieved
     * @return a {@link List} of task responses belonging to the user; an empty list if the user has no tasks
     */
    @GetExchange(TASKS_GET_BY_USER_ID_FULL_PATH)
    List<TasksByUserIdResponse> getTasksByUserId(@PathVariable UUID id);

}
