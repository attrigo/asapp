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

package com.bcn.asapp.tasks.infrastructure.task.out;

import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.defaultFakeTask;
import static com.bcn.asapp.tasks.testutil.TestDataFaker.TaskDataFaker.fakeTaskBuilder;
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
class TaskJdbcRepositoryIT {

    @Autowired
    private TaskJdbcRepository taskRepository;

    @BeforeEach
    void beforeEach() {
        taskRepository.deleteAll();
    }

    @Nested
    class FindByUserId {

        @Test
        void DoesNotFindTasksAndReturnsEmptyList_TasksNotExistsByUserId() {
            // When
            var userId = UUID.randomUUID();

            var actual = taskRepository.findByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void FindsTasksAndReturnsTasksFound_TasksExistsByUserId() {
            // Given
            var userId = UUID.randomUUID();

            var firstTask = fakeTaskBuilder().withUserId(userId)
                                             .build();
            var secondTask = fakeTaskBuilder().withUserId(userId)
                                              .build();
            var thirdTask = fakeTaskBuilder().withUserId(userId)
                                             .build();
            var firstTaskCreated = taskRepository.save(firstTask);
            var secondTaskCreated = taskRepository.save(secondTask);
            var thirdTaskCreated = taskRepository.save(thirdTask);
            assertThat(firstTaskCreated).isNotNull();
            assertThat(secondTaskCreated).isNotNull();
            assertThat(thirdTaskCreated).isNotNull();

            // When
            var actual = taskRepository.findByUserId(userId);

            // Then
            assertThat(actual).hasSize(3)
                              .containsOnly(firstTaskCreated, secondTaskCreated, thirdTaskCreated);
        }

    }

    @Nested
    class DeleteTaskById {

        @Test
        void DoesNotDeleteTaskAndReturnsZero_TaskNotExists() {
            // When
            var taskId = UUID.randomUUID();

            var actual = taskRepository.deleteTaskById(taskId);

            // Then
            assertThat(actual).isZero();
        }

        @Test
        void DeletesTaskAndReturnsAmountOfTasksDeleted_TaskExists() {
            // Given
            var task = defaultFakeTask();
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
