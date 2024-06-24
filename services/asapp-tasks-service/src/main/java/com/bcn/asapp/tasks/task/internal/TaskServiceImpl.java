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
package com.bcn.asapp.tasks.task.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bcn.asapp.dto.task.TaskDTO;
import com.bcn.asapp.tasks.task.Task;
import com.bcn.asapp.tasks.task.TaskMapper;
import com.bcn.asapp.tasks.task.TaskRepository;
import com.bcn.asapp.tasks.task.TaskService;

/**
 * Default implementation of {@link TaskService}.
 *
 * @author ttrigo
 * @since 0.1.0
 */

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    private final TaskMapper taskMapper;

    /**
     * Default constructor.
     *
     * @param taskMapper     the mapper to map between {@link TaskDTO} and {@link Task} and, must not be {@literal null}.
     * @param taskRepository the repository to access task's data, must not be {@literal null}.
     */
    public TaskServiceImpl(TaskMapper taskMapper, TaskRepository taskRepository) {
        this.taskMapper = taskMapper;
        this.taskRepository = taskRepository;
    }

    @Override
    public Optional<TaskDTO> findById(UUID id) {
        return this.taskRepository.findById(id)
                                  .map(taskMapper::toTaskDTO);
    }

    @Override
    public List<TaskDTO> findAll() {
        return taskRepository.findAll()
                             .stream()
                             .map(taskMapper::toTaskDTO)
                             .toList();
    }

    @Override
    public List<TaskDTO> findByProjectId(UUID projectId) {
        return taskRepository.findByProjectId(projectId)
                             .stream()
                             .map(taskMapper::toTaskDTO)
                             .toList();
    }

    @Override
    public TaskDTO create(TaskDTO task) {
        var taskWithoutId = taskMapper.toTaskIgnoreId(task);
        var taskCreated = taskRepository.save(taskWithoutId);

        return taskMapper.toTaskDTO(taskCreated);
    }

    @Override
    public Optional<TaskDTO> updateById(UUID id, TaskDTO newTaskData) {
        var taskExists = taskRepository.existsById(id);

        if (!taskExists) {
            return Optional.empty();
        }

        var task = taskMapper.toTask(newTaskData, id);
        var taskUpdated = taskRepository.save(task);

        var taskDTOUpdated = taskMapper.toTaskDTO(taskUpdated);
        return Optional.of(taskDTOUpdated);
    }

    @Override
    public Boolean deleteById(UUID id) {
        return taskRepository.deleteTaskById(id) > 0;
    }

}
