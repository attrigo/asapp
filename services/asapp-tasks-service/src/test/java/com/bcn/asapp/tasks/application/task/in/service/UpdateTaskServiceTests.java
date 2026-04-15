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

package com.bcn.asapp.tasks.application.task.in.service;

import static com.bcn.asapp.tasks.testutil.fixture.TaskFactory.aTask;
import static com.bcn.asapp.tasks.testutil.fixture.TaskFactory.aTaskBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.tasks.application.task.in.command.UpdateTaskCommand;
import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskId;

/**
 * Tests {@link UpdateTaskService} two-phase fetch-then-persist with field updates.
 * <p>
 * Coverage:
 * <li>Fetch failures prevent update workflow execution</li>
 * <li>Persistence failures after successful fetch propagate</li>
 * <li>Returns empty when task does not exist (no-op update)</li>
 * <li>Successful update completes fetch, mutation, and persistence phases</li>
 */
@ExtendWith(MockitoExtension.class)
class UpdateTaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private UpdateTaskService updateTaskService;

    @Nested
    class UpdateTaskById {

        @Test
        void ReturnsUpdatedTask_TaskExists() {
            // Given
            var existingTask = aTask();
            var existingTaskId = existingTask.getId();
            var existingUserId = existingTask.getUserId();
            var newTask = aTaskBuilder().withTitle("New Title")
                                        .withDescription("New Description")
                                        .withStartDate(Instant.parse("2025-03-03T13:00:00Z"))
                                        .withEndDate(Instant.parse("2025-04-04T14:00:00Z"))
                                        .build();
            var newTitle = newTask.getTitle();
            var newDescription = newTask.getDescription();
            var newStartDate = newTask.getStartDate();
            var newEndDate = newTask.getEndDate();
            var command = new UpdateTaskCommand(existingTaskId.value(), existingUserId.value(), newTitle.value(), newDescription.value(), newStartDate.value(),
                    newEndDate.value());

            given(taskRepository.findById(existingTaskId)).willReturn(Optional.of(existingTask));
            given(taskRepository.save(existingTask)).willReturn(existingTask); // Returns same reference so assertions verify the in-place domain mutation

            // When
            var actual = updateTaskService.updateTaskById(command);

            // Then
            assertThat(actual).as("updated task")
                              .isPresent();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.get().getId()).as("ID").isEqualTo(existingTaskId);
                softly.assertThat(actual.get().getUserId()).as("user ID").isEqualTo(existingUserId);
                softly.assertThat(actual.get().getTitle()).as("title").isEqualTo(newTitle);
                softly.assertThat(actual.get().getDescription()).as("description").isEqualTo(newDescription);
                softly.assertThat(actual.get().getStartDate()).as("start date").isEqualTo(newStartDate);
                softly.assertThat(actual.get().getEndDate()).as("end date").isEqualTo(newEndDate);
                // @formatter:on
            });

            then(taskRepository).should(times(1))
                                .findById(existingTaskId);
            then(taskRepository).should(times(1))
                                .save(existingTask);
        }

        @Test
        void ReturnsEmpty_TaskNotExists() {
            // Given
            var taskIdValue = UUID.fromString("c3d4e5f6-a7b8-4901-c2d3-e4f5a6b7c8d9");
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var description = "Description";
            var startDate = Instant.parse("2025-01-01T10:00:00Z");
            var endDate = Instant.parse("2025-01-02T10:00:00Z");
            var command = new UpdateTaskCommand(taskIdValue, userId, title, description, startDate, endDate);
            var taskId = TaskId.of(taskIdValue);

            given(taskRepository.findById(taskId)).willReturn(Optional.empty());

            // When
            var actual = updateTaskService.updateTaskById(command);

            // Then
            assertThat(actual).isEmpty();

            then(taskRepository).should(times(1))
                                .findById(taskId);
            then(taskRepository).should(never())
                                .save(any(Task.class));
        }

        @Test
        void ThrowsRuntimeException_TaskRetrievalFails() {
            // Given
            var taskIdValue = UUID.fromString("c3d4e5f6-a7b8-4901-c2d3-e4f5a6b7c8d9");
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var description = "Description";
            var startDate = Instant.parse("2025-01-01T10:00:00Z");
            var endDate = Instant.parse("2025-01-02T10:00:00Z");
            var command = new UpdateTaskCommand(taskIdValue, userId, title, description, startDate, endDate);
            var taskId = TaskId.of(taskIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .findById(taskId);

            // When
            var actual = catchThrowable(() -> updateTaskService.updateTaskById(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(taskRepository).should(times(1))
                                .findById(taskId);
            then(taskRepository).should(never())
                                .save(any(Task.class));
        }

        @Test
        void ThrowsRuntimeException_TaskPersistenceFails() {
            // Given
            var existingTask = aTask();
            var existingTaskId = existingTask.getId();
            var existingUserId = existingTask.getUserId();
            var newTitle = "New Title";
            var newDescription = "New Description";
            var newStartDate = Instant.parse("2025-03-03T13:00:00Z");
            var newEndDate = Instant.parse("2025-04-04T14:00:00Z");
            var command = new UpdateTaskCommand(existingTaskId.value(), existingUserId.value(), newTitle, newDescription, newStartDate, newEndDate);

            given(taskRepository.findById(existingTaskId)).willReturn(Optional.of(existingTask));
            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .save(existingTask);

            // When
            var actual = catchThrowable(() -> updateTaskService.updateTaskById(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(taskRepository).should(times(1))
                                .findById(existingTaskId);
            then(taskRepository).should(times(1))
                                .save(existingTask);
        }

    }

}
