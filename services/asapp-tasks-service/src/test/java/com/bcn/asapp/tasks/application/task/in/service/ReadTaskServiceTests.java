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

package com.bcn.asapp.tasks.application.task.in.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.Description;
import com.bcn.asapp.tasks.domain.task.EndDate;
import com.bcn.asapp.tasks.domain.task.StartDate;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskId;
import com.bcn.asapp.tasks.domain.task.Title;
import com.bcn.asapp.tasks.domain.task.UserId;

@ExtendWith(MockitoExtension.class)
class ReadTaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ReadTaskService readTaskService;

    // Specific test data
    private final UUID taskIdValue = UUID.randomUUID();

    private final UUID userIdValue = UUID.randomUUID();

    private final String titleValue = "Task Title";

    private final String descriptionValue = "Task Description";

    private final Instant startDateValue = Instant.now();

    private final Instant endDateValue = Instant.now()
                                                .plusSeconds(3600);

    @Nested
    class GetTaskById {

        @Test
        void ThrowsRuntimeException_FetchTaskFails() {
            // Given
            var taskId = TaskId.of(taskIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .findById(taskId);

            // When
            var thrown = catchThrowable(() -> readTaskService.getTaskById(taskIdValue));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Database connection failed");

            then(taskRepository).should(times(1))
                                .findById(taskId);
        }

        @Test
        void ReturnsEmptyOptional_TaskNotExists() {
            // Given
            var taskId = TaskId.of(taskIdValue);

            given(taskRepository.findById(taskId)).willReturn(Optional.empty());

            // When
            var result = readTaskService.getTaskById(taskIdValue);

            // Then
            then(taskRepository).should(times(1))
                                .findById(taskId);
            assertThat(result).isEmpty();
        }

        @Test
        void ReturnsTask_TaskExists() {
            // Given
            var taskId = TaskId.of(taskIdValue);
            var userId = UserId.of(userIdValue);
            var title = Title.of(titleValue);
            var description = Description.ofNullable(descriptionValue);
            var startDate = StartDate.ofNullable(startDateValue);
            var endDate = EndDate.ofNullable(endDateValue);

            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

            // When
            var result = readTaskService.getTaskById(taskIdValue);

            // Then
            then(taskRepository).should(times(1))
                                .findById(taskId);
            assertThat(result).isPresent();
            assertThat(result.get()
                             .getId()).isEqualTo(taskId);
            assertThat(result.get()
                             .getTitle()).isEqualTo(title);
        }

    }

    @Nested
    class GetTasksByUserId {

        @Test
        void ThrowsRuntimeException_FetchTasksFails() {
            // Given
            var userId = UserId.of(userIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .findByUserId(userId);

            // When
            var thrown = catchThrowable(() -> readTaskService.getTasksByUserId(userIdValue));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Database connection failed");

            then(taskRepository).should(times(1))
                                .findByUserId(userId);
        }

        @Test
        void ReturnsEmptyList_TasksNotExist() {
            // Given
            var userId = UserId.of(userIdValue);

            given(taskRepository.findByUserId(userId)).willReturn(List.of());

            // When
            var result = readTaskService.getTasksByUserId(userIdValue);

            // Then
            then(taskRepository).should(times(1))
                                .findByUserId(userId);
            assertThat(result).isEmpty();
        }

        @Test
        void ReturnsTasks_TasksExist() {
            // Given
            var userId = UserId.of(userIdValue);
            var taskId1 = TaskId.of(UUID.randomUUID());
            var taskId2 = TaskId.of(UUID.randomUUID());

            var task1 = Task.reconstitute(taskId1, userId, Title.of("Task 1"), Description.ofNullable("Description 1"), StartDate.ofNullable(startDateValue),
                    EndDate.ofNullable(endDateValue));

            var task2 = Task.reconstitute(taskId2, userId, Title.of("Task 2"), Description.ofNullable("Description 2"), StartDate.ofNullable(startDateValue),
                    EndDate.ofNullable(endDateValue));

            Collection<Task> tasks = List.of(task1, task2);

            given(taskRepository.findByUserId(userId)).willReturn(tasks);

            // When
            var result = readTaskService.getTasksByUserId(userIdValue);

            // Then
            then(taskRepository).should(times(1))
                                .findByUserId(userId);
            assertThat(result).hasSize(2);
            assertThat(result).contains(task1, task2);
        }

    }

    @Nested
    class GetAllTasks {

        @Test
        void ThrowsRuntimeException_FetchTasksFails() {
            // Given
            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .findAll();

            // When
            var thrown = catchThrowable(() -> readTaskService.getAllTasks());

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Database connection failed");

            then(taskRepository).should(times(1))
                                .findAll();
        }

        @Test
        void ReturnsEmptyList_TasksNotExist() {
            // Given
            given(taskRepository.findAll()).willReturn(List.of());

            // When
            var result = readTaskService.getAllTasks();

            // Then
            then(taskRepository).should(times(1))
                                .findAll();
            assertThat(result).isEmpty();
        }

        @Test
        void ReturnsTasks_TasksExist() {
            // Given
            var userId1 = UserId.of(UUID.randomUUID());
            var userId2 = UserId.of(UUID.randomUUID());
            var taskId1 = TaskId.of(UUID.randomUUID());
            var taskId2 = TaskId.of(UUID.randomUUID());

            var task1 = Task.reconstitute(taskId1, userId1, Title.of("Task 1"), Description.ofNullable("Description 1"), StartDate.ofNullable(startDateValue),
                    EndDate.ofNullable(endDateValue));

            var task2 = Task.reconstitute(taskId2, userId2, Title.of("Task 2"), Description.ofNullable("Description 2"), StartDate.ofNullable(startDateValue),
                    EndDate.ofNullable(endDateValue));

            Collection<Task> tasks = List.of(task1, task2);

            given(taskRepository.findAll()).willReturn(tasks);

            // When
            var result = readTaskService.getAllTasks();

            // Then
            then(taskRepository).should(times(1))
                                .findAll();
            assertThat(result).hasSize(2);
            assertThat(result).contains(task1, task2);
        }

    }

}
