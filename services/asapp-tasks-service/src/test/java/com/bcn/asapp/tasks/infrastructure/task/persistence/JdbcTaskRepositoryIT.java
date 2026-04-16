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

package com.bcn.asapp.tasks.infrastructure.task.persistence;

import static com.bcn.asapp.tasks.testutil.fixture.TaskMother.aJdbcTask;
import static com.bcn.asapp.tasks.testutil.fixture.TaskMother.aTaskBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.bcn.asapp.tasks.testutil.TestContainerConfiguration;

/**
 * Tests {@link JdbcTaskRepository} query and delete operations against PostgreSQL.
 * <p>
 * Coverage:
 * <li>Queries tasks by user ownership returning empty when none found</li>
 * <li>Queries tasks by user ownership returning all matching tasks</li>
 * <li>Deletes task by identifier returning zero when not found</li>
 * <li>Deletes task by identifier returning count when successfully deleted</li>
 * <li>Tests actual database operations with TestContainers PostgreSQL</li>
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainerConfiguration.class)
class JdbcTaskRepositoryIT {

    @Autowired
    private JdbcTaskRepository taskRepository;

    @BeforeEach
    void beforeEach() {
        taskRepository.deleteAll();
    }

    @Nested
    class FindByUserId {

        @Test
        void ReturnsFoundTasks_TasksExistForUserId() {
            // Given
            var userId = UUID.fromString("c8e5a2f9-4d7b-46af-9d8e-6b3f1c9a5e2d");

            var task1 = aTaskBuilder().withUserId(userId)
                                      .buildJdbc();
            var task2 = aTaskBuilder().withUserId(userId)
                                      .buildJdbc();
            var task3 = aTaskBuilder().withUserId(userId)
                                      .buildJdbc();
            var createdTask1 = createTask(task1);
            var createdTask2 = createTask(task2);
            var createdTask3 = createTask(task3);

            // When
            var actual = taskRepository.findByUserId(userId);

            // Then
            assertThat(actual).hasSize(3)
                              .containsExactlyInAnyOrder(createdTask1, createdTask2, createdTask3);
        }

        @Test
        void ReturnsEmptyList_TasksNotExistForUserId() {
            // Given
            var userId = UUID.fromString("c8e5a2f9-4d7b-46af-9d8e-6b3f1c9a5e2d");

            // When
            var actual = taskRepository.findByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

    }

    @Nested
    class DeleteTaskById {

        @Test
        void ReturnsDeletionCount_TaskExists() {
            // Given
            var createdTask = createTask();
            var taskId = createdTask.id();

            // When
            var actual = taskRepository.deleteTaskById(taskId);

            // Then
            assertThat(actual).isGreaterThan(0);
        }

        @Test
        void ReturnsZero_TaskNotExists() {
            // Given
            var taskId = UUID.fromString("e2a7c9f4-6b3d-48ab-9f1a-8d5b3e7c2a9f");

            // When
            var actual = taskRepository.deleteTaskById(taskId);

            // Then
            assertThat(actual).isZero();
        }

    }

    // Test Data Creation Helpers

    private JdbcTaskEntity createTask() {
        var task = aJdbcTask();
        return createTask(task);
    }

    private JdbcTaskEntity createTask(JdbcTaskEntity task) {
        var createdTask = taskRepository.save(task);
        assertThat(createdTask).isNotNull();
        return createdTask;
    }

}
