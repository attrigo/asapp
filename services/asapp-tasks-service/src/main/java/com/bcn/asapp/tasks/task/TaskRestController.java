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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bcn.asapp.dto.task.TaskDTO;

/**
 * Default implementation of {@link TaskRestAPI}.
 *
 * @author ttrigo
 * @since 0.1.0
 */
@RestController
public class TaskRestController implements TaskRestAPI {

    private static final Logger log = LoggerFactory.getLogger(TaskRestController.class);

    private final TaskService taskService;

    /**
     * Default constructor.
     *
     * @param taskService the service that brings task's business operations, must not be {@literal null}.
     */
    public TaskRestController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public ResponseEntity<TaskDTO> getTaskById(UUID id) {
        log.info("getTaskById");
        return taskService.findById(id)
                          .map(ResponseEntity::ok)
                          .orElseGet(() -> ResponseEntity.notFound()
                                                         .build());
    }

    @Override
    public List<TaskDTO> getAllTasks() {
        log.info("getAllTasks");
        return taskService.findAll();
    }

    @Override
    public List<TaskDTO> getTasksByProjectId(UUID projectId) {
        log.info("getTasksByProjectId");
        return taskService.findByProjectId(projectId);
    }

    @Override
    public TaskDTO createTask(TaskDTO task) {
        log.info("createTask");
        return taskService.create(task);
    }

    @Override
    public ResponseEntity<TaskDTO> updateTaskById(UUID id, TaskDTO newTaskData) {
        log.info("updateTaskById");
        return taskService.updateById(id, newTaskData)
                          .map(ResponseEntity::ok)
                          .orElseGet(() -> ResponseEntity.notFound()
                                                         .build());
    }

    @Override
    public ResponseEntity<Void> deleteTaskById(UUID id) {
        log.info("deleteTaskById");
        boolean taskHasBeenDeleted = taskService.deleteById(id);
        return ResponseEntity.status(Boolean.TRUE.equals(taskHasBeenDeleted) ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND)
                             .build();
    }

}
