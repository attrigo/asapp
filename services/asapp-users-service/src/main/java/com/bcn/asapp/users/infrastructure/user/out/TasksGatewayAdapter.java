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

package com.bcn.asapp.users.infrastructure.user.out;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.bcn.asapp.clients.tasks.TasksClient;
import com.bcn.asapp.users.application.user.out.TasksGateway;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Adapter implementation of {@link TasksGateway} for external calls to tasks-service.
 * <p>
 * Bridges the application layer with the infrastructure layer by translating domain-level requests into external service calls.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class TasksGatewayAdapter implements TasksGateway {

    private final TasksClient tasksClient;

    /**
     * Constructs a new {@code TasksServiceGateway} with required dependencies.
     *
     * @param tasksClient the client for communicating with the tasks-service
     */
    public TasksGatewayAdapter(TasksClient tasksClient) {
        this.tasksClient = tasksClient;
    }

    /**
     * Retrieves all task identifiers associated with a specific user by delegating to the tasks-service client.
     *
     * @param userId the user's unique identifier
     * @return a {@link List} of task UUIDs associated with the user, otherwise an empty list if the user has no tasks or the retrieval fails
     */
    @Override
    public List<UUID> getTaskIdsByUserId(UserId userId) {
        return tasksClient.getTaskIdsByUserId(userId.value());
    }

}
