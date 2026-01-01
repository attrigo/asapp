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

package com.bcn.asapp.tasks.application.task.in;

import com.bcn.asapp.tasks.application.task.in.command.CreateTaskCommand;
import com.bcn.asapp.tasks.domain.task.Task;

/**
 * Use case for creating new tasks in the system.
 * <p>
 * Defines the contract for task creation operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface CreateTaskUseCase {

    /**
     * Creates a new task based on the provided command.
     *
     * @param command the {@link CreateTaskCommand} containing task registration data
     * @return the created {@link Task} with a persistent ID
     * @throws IllegalArgumentException if any data within the command is invalid
     */
    Task createTask(CreateTaskCommand command);

}
