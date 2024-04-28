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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void beforeEach() {
        taskRepository.deleteAll();
    }

    // deleteTaskById
    @Test
    @DisplayName("GIVEN id not exists WHEN delete a task by id THEN does not delete any task And returns zero")
    void IdNotExists_DeleteTaskById_DoesNotDeleteTaskAndReturnsZero() {
        // When
        var idToDelete = UUID.randomUUID();

        var actual = taskRepository.deleteTaskById(idToDelete);

        // Then
        assertEquals(0L, actual);
    }

    @Test
    @DisplayName("GIVEN id exists WHEN delete a task by id THEN deletes the task with the given id And returns the amount of tasks deleted")
    void IdExists_DeleteTaskById_DeletesTaskAndReturnsAmountOfTasksDeleted() {
        // Given
        var fakeTask = new Task(null, "Title Test", "Description Test", LocalDateTime.now());
        var taskToBeDeleted = taskRepository.save(fakeTask);
        Assertions.assertNotNull(taskToBeDeleted);

        // When
        var idToDelete = taskToBeDeleted.id();

        var actual = taskRepository.deleteTaskById(idToDelete);

        // Then
        assertEquals(1L, actual);

        assertFalse(taskRepository.findById(taskToBeDeleted.id())
                                  .isPresent());
    }

}
