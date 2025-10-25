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

package com.bcn.asapp.tasks.infrastructure.task.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bcn.asapp.tasks.application.task.in.command.CreateTaskCommand;
import com.bcn.asapp.tasks.application.task.in.command.UpdateTaskCommand;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.infrastructure.task.in.request.CreateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.request.UpdateTaskRequest;
import com.bcn.asapp.tasks.infrastructure.task.in.response.CreateTaskResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetAllTasksResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTaskByIdResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.GetTasksByUserIdResponse;
import com.bcn.asapp.tasks.infrastructure.task.in.response.UpdateTaskResponse;
import com.bcn.asapp.tasks.infrastructure.task.out.entity.TaskEntity;

/**
 * MapStruct mapper for mapping between task-related objects.
 * <p>
 * Handles mappings between REST requests, commands, domain entities, database entities, and responses.
 * <p>
 * Uses custom object factories and component mappers for complex value object transformations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring", uses = { TaskObjectFactory.class, TaskIdMapper.class, UserIdMapper.class, TitleMapper.class, DescriptionMapper.class,
        StartDateMapper.class, EndDateMapper.class })
public interface TaskMapper {

    /**
     * Maps a {@link CreateTaskRequest} to a {@link CreateTaskCommand}.
     *
     * @param request the {@link CreateTaskRequest}
     * @return the {@link CreateTaskCommand}
     */
    CreateTaskCommand toCreateTaskCommand(CreateTaskRequest request);

    /**
     * Maps a {@link UpdateTaskRequest} and the task ID to a {@link UpdateTaskCommand}.
     *
     * @param taskId  the task's unique identifier
     * @param request the {@link UpdateTaskRequest}
     * @return the {@link UpdateTaskCommand}
     */
    UpdateTaskCommand toUpdateTaskCommand(UUID taskId, UpdateTaskRequest request);

    /**
     * Maps a domain {@link Task} to a database {@link TaskEntity} entity.
     *
     * @param task the {@link Task} domain entity
     * @return the {@link TaskEntity} database entity
     */
    TaskEntity toTaskEntity(Task task);

    /**
     * Maps a database {@link TaskEntity} entity to a domain {@link Task}.
     * <p>
     * Uses {@link TaskObjectFactory} to construct the domain entity with proper validation.
     *
     * @param taskEntity the {@link TaskEntity} database entity
     * @return the {@link Task} domain entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    Task toTask(TaskEntity taskEntity);

    /**
     * Maps a domain {@link Task} to a {@link GetTaskByIdResponse}.
     *
     * @param task the {@link Task} domain entity
     * @return the {@link GetTaskByIdResponse}
     */
    @Mapping(target = "taskId", source = "id")
    GetTaskByIdResponse toGetTaskByIdResponse(Task task);

    /**
     * Maps a domain {@link Task} to a {@link GetTasksByUserIdResponse}.
     *
     * @param task the {@link Task} domain entity
     * @return the {@link GetTasksByUserIdResponse}
     */
    @Mapping(target = "taskId", source = "id")
    GetTasksByUserIdResponse toGetTasksByUserIdResponse(Task task);

    /**
     * Maps a domain {@link Task} to a {@link GetAllTasksResponse}.
     *
     * @param task the {@link Task} domain entity
     * @return the {@link GetAllTasksResponse}
     */
    @Mapping(target = "taskId", source = "id")
    GetAllTasksResponse toGetAllTasksResponse(Task task);

    /**
     * Maps a domain {@link Task} to a {@link CreateTaskResponse}.
     *
     * @param task the {@link Task} domain entity
     * @return the {@link CreateTaskResponse}
     */
    @Mapping(target = "taskId", source = "id")
    CreateTaskResponse toCreateTaskResponse(Task task);

    /**
     * Maps a domain {@link Task} to a {@link UpdateTaskResponse}.
     *
     * @param task the {@link Task} domain entity
     * @return the {@link UpdateTaskResponse}
     */
    @Mapping(target = "taskId", source = "id")
    UpdateTaskResponse toUpdateTaskResponse(Task task);

}
