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

package com.bcn.asapp.tasks.infrastructure.task.persistence;

import static com.bcn.asapp.tasks.testutil.TestFactory.TestTaskFactory.defaultTestTask;
import static com.bcn.asapp.tasks.testutil.TestFactory.TestTaskFactory.testTaskBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.bcn.asapp.tasks.testutil.TestContainerConfiguration;

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
        void DoesNotFindTasksAndReturnsEmptyList_TasksNotExistsByUserId() {
            // When
            var userId = UUID.fromString("c8e5a2f9-4d7b-46af-9d8e-6b3f1c9a5e2d");

            var actual = taskRepository.findByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void FindsTasksAndReturnsTasksFound_TasksExistsByUserId() {
            // Given
            var userId = UUID.fromString("c8e5a2f9-4d7b-46af-9d8e-6b3f1c9a5e2d");

            var task1 = testTaskBuilder().withUserId(userId)
                                         .build();
            var task2 = testTaskBuilder().withUserId(userId)
                                         .build();
            var task3 = testTaskBuilder().withUserId(userId)
                                         .build();
            var taskCreated1 = taskRepository.save(task1);
            var taskCreated2 = taskRepository.save(task2);
            var taskCreated3 = taskRepository.save(task3);
            assertThat(taskCreated1).isNotNull();
            assertThat(taskCreated2).isNotNull();
            assertThat(taskCreated3).isNotNull();

            // When
            var actual = taskRepository.findByUserId(userId);

            // Then
            assertThat(actual).hasSize(3)
                              .containsExactlyInAnyOrder(taskCreated1, taskCreated2, taskCreated3);
        }

    }

    @Nested
    class DeleteTaskById {

        @Test
        void DoesNotDeleteTaskAndReturnsZero_TaskNotExists() {
            // When
            var taskId = UUID.fromString("e2a7c9f4-6b3d-48ab-9f1a-8d5b3e7c2a9f");

            var actual = taskRepository.deleteTaskById(taskId);

            // Then
            assertThat(actual).isZero();
        }

        @Test
        void DeletesTaskAndReturnsAmountOfTasksDeleted_TaskExists() {
            // Given
            var task = defaultTestTask();
            var taskCreated = taskRepository.save(task);
            assertThat(taskCreated).isNotNull();

            // When
            var taskId = taskCreated.id();

            var actual = taskRepository.deleteTaskById(taskId);

            // Then
            assertThat(actual).isGreaterThan(0);
        }

    }

}
