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

package com.attrigo.asapp.tasks.application.task.in.service;

import static com.attrigo.asapp.tasks.testutil.fixture.TaskMother.aTask;
import static com.attrigo.asapp.tasks.testutil.fixture.TaskMother.aTaskBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.attrigo.asapp.tasks.application.task.out.TaskRepository;
import com.attrigo.asapp.tasks.domain.task.TaskId;
import com.attrigo.asapp.tasks.domain.task.UserId;

/**
 * Tests {@link ReadTaskService} single, collection, and ownership-based retrieval.
 * <p>
 * Coverage:
 * <li>Retrieval failures propagate for all query strategies (by ID, by user, all tasks)</li>
 * <li>Returns empty result when no tasks match query criteria</li>
 * <li>Returns single task when queried by unique identifier</li>
 * <li>Returns task collection when queried by a list of identifiers</li>
 * <li>Deduplicates duplicate identifiers before querying</li>
 * <li>Throws when the input list contains a null identifier</li>
 * <li>Returns task collection when queried by user ownership</li>
 * <li>Returns all tasks regardless of ownership</li>
 */
@ExtendWith(MockitoExtension.class)
class ReadTaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ReadTaskService readTaskService;

    @Nested
    class GetTaskById {

        @Test
        void ReturnsTask_TaskExists() {
            // Given
            var task = aTask();
            var taskId = task.getId();

            given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

            // When
            var actual = readTaskService.getTaskById(taskId.value());

            // Then
            assertThat(actual).isPresent();
            assertThat(actual.get()).isEqualTo(task);

            then(taskRepository).should(times(1))
                                .findById(taskId);
        }

        @Test
        void ReturnsEmptyOptional_TaskNotExists() {
            // Given
            var taskIdValue = UUID.fromString("c3d4e5f6-a7b8-4901-c2d3-e4f5a6b7c8d9");
            var taskId = TaskId.of(taskIdValue);

            given(taskRepository.findById(taskId)).willReturn(Optional.empty());

            // When
            var actual = readTaskService.getTaskById(taskIdValue);

            // Then
            assertThat(actual).isEmpty();

            then(taskRepository).should(times(1))
                                .findById(taskId);
        }

        @Test
        void ThrowsException_TaskRetrievalFails() {
            // Given
            var taskIdValue = UUID.fromString("c3d4e5f6-a7b8-4901-c2d3-e4f5a6b7c8d9");
            var taskId = TaskId.of(taskIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .findById(taskId);

            // When
            var actual = catchThrowable(() -> readTaskService.getTaskById(taskIdValue));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(taskRepository).should(times(1))
                                .findById(taskId);
        }

    }

    @Nested
    class GetTasksByIds {

        @Test
        void ReturnsTasks_AllTasksExist() {
            // Given
            var task1 = aTask();
            var task2 = aTaskBuilder().withTaskId(UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04"))
                                      .withTitle("Task 2")
                                      .withDescription("Description 2")
                                      .build();
            var taskId1 = task1.getId();
            var taskId2 = task2.getId();
            var tasks = List.of(task1, task2);
            var taskIds = Set.of(taskId1, taskId2);
            var taskIdValues = List.of(taskId1.value(), taskId2.value());

            given(taskRepository.findByIds(taskIds)).willReturn(tasks);

            // When
            var actual = readTaskService.getTasksByIds(taskIdValues);

            // Then
            assertThat(actual).containsExactlyInAnyOrder(task1, task2);

            then(taskRepository).should(times(1))
                                .findByIds(taskIds);
        }

        @Test
        void ReturnsFoundTasks_SomeTasksExist() {
            // Given
            var task1 = aTask();
            var task2 = aTaskBuilder().withTaskId(UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04"))
                                      .withTitle("Task 2")
                                      .withDescription("Description 2")
                                      .build();
            var taskId1 = task1.getId();
            var taskId2 = task2.getId();
            var taskIds = Set.of(taskId1, taskId2);
            var taskIdValues = List.of(taskId1.value(), taskId2.value());

            given(taskRepository.findByIds(taskIds)).willReturn(List.of(task1));

            // When
            var actual = readTaskService.getTasksByIds(taskIdValues);

            // Then
            assertThat(actual).containsExactlyInAnyOrder(task1);

            then(taskRepository).should(times(1))
                                .findByIds(taskIds);
        }

        @Test
        void ReturnsEmptyList_TasksNotExist() {
            // Given
            var task1 = aTask();
            var task2 = aTaskBuilder().withTaskId(UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04"))
                                      .withTitle("Task 2")
                                      .withDescription("Description 2")
                                      .build();
            var taskId1 = task1.getId();
            var taskId2 = task2.getId();
            var taskIds = Set.of(taskId1, taskId2);
            var taskIdValues = List.of(taskId1.value(), taskId2.value());

            given(taskRepository.findByIds(taskIds)).willReturn(List.of());

            // When
            var actual = readTaskService.getTasksByIds(taskIdValues);

            // Then
            assertThat(actual).isEmpty();

            then(taskRepository).should(times(1))
                                .findByIds(taskIds);
        }

        @Test
        void ReturnsTasksOnce_DuplicateIds() {
            // Given
            var task1 = aTask();
            var task2 = aTaskBuilder().withTaskId(UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04"))
                                      .withTitle("Task 2")
                                      .withDescription("Description 2")
                                      .build();
            var taskId1 = task1.getId();
            var taskId2 = task2.getId();
            var tasks = List.of(task1, task2);
            var taskIds = Set.of(taskId1, taskId2);
            var taskIdValues = List.of(taskId1.value(), taskId1.value(), taskId2.value());

            given(taskRepository.findByIds(taskIds)).willReturn(tasks);

            // When
            var actual = readTaskService.getTasksByIds(taskIdValues);

            // Then
            assertThat(actual).containsExactlyInAnyOrder(task1, task2);

            then(taskRepository).should(times(1))
                                .findByIds(taskIds);
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // Given
            var taskIdValue = UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04");
            var taskIds = new ArrayList<UUID>();
            taskIds.add(taskIdValue);
            taskIds.add(null);

            // When
            var actual = catchThrowable(() -> readTaskService.getTasksByIds(taskIds));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Task ID must not be null");
        }

        @Test
        void ThrowsException_TasksRetrievalFails() {
            // Given
            var taskIdValue = UUID.fromString("6626568e-b677-43bb-99ad-923ca9b24b04");
            var taskId = TaskId.of(taskIdValue);
            var taskIds = Set.of(taskId);
            var taskIdValues = List.of(taskIdValue);

            given(taskRepository.findByIds(taskIds)).willThrow(RuntimeException.class);

            // When
            var actual = catchThrowable(() -> readTaskService.getTasksByIds(taskIdValues));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class);

            then(taskRepository).should(times(1))
                                .findByIds(taskIds);
        }

    }

    @Nested
    class GetTasksByUserId {

        @Test
        void ReturnsTasks_TasksExistForUserId() {
            // Given
            var task1 = aTask();
            var task2 = aTaskBuilder().withTaskId(UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef0123456789"))
                                      .withTitle("Task 2")
                                      .withDescription("Description 2")
                                      .build();
            var userId = task1.getUserId();
            var tasks = List.of(task1, task2);

            given(taskRepository.findByUserId(userId)).willReturn(tasks);

            // When
            var actual = readTaskService.getTasksByUserId(userId.value());

            // Then
            assertThat(actual).containsExactlyInAnyOrder(task1, task2);

            then(taskRepository).should(times(1))
                                .findByUserId(userId);
        }

        @Test
        void ReturnsEmptyList_TasksNotExistForUserId() {
            // Given
            var userIdValue = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var userId = UserId.of(userIdValue);

            given(taskRepository.findByUserId(userId)).willReturn(List.of());

            // When
            var actual = readTaskService.getTasksByUserId(userIdValue);

            // Then
            assertThat(actual).isEmpty();

            then(taskRepository).should(times(1))
                                .findByUserId(userId);
        }

        @Test
        void ThrowsException_TasksRetrievalFails() {
            // Given
            var userIdValue = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var userId = UserId.of(userIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .findByUserId(userId);

            // When
            var actual = catchThrowable(() -> readTaskService.getTasksByUserId(userIdValue));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(taskRepository).should(times(1))
                                .findByUserId(userId);
        }

    }

    @Nested
    class GetAllTasks {

        @Test
        void ReturnsTasks_TasksExist() {
            // Given
            var task1 = aTask();
            var task2 = aTaskBuilder().withTaskId(UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef0123456789"))
                                      .withUserId(UUID.fromString("b2c3d4e5-f6a7-4890-bcde-f01234567890"))
                                      .withTitle("Task 2")
                                      .withDescription("Description 2")
                                      .build();
            var tasks = List.of(task1, task2);

            given(taskRepository.findAll()).willReturn(tasks);

            // When
            var actual = readTaskService.getAllTasks();

            // Then
            assertThat(actual).containsExactlyInAnyOrder(task1, task2);

            then(taskRepository).should(times(1))
                                .findAll();
        }

        @Test
        void ReturnsEmptyList_TasksNotExist() {
            // Given
            given(taskRepository.findAll()).willReturn(List.of());

            // When
            var actual = readTaskService.getAllTasks();

            // Then
            assertThat(actual).isEmpty();

            then(taskRepository).should(times(1))
                                .findAll();
        }

        @Test
        void ThrowsException_TasksRetrievalFails() {
            // Given
            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .findAll();

            // When
            var actual = catchThrowable(() -> readTaskService.getAllTasks());

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(taskRepository).should(times(1))
                                .findAll();
        }

    }

}
