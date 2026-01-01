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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
import com.bcn.asapp.tasks.domain.task.Description;
import com.bcn.asapp.tasks.domain.task.EndDate;
import com.bcn.asapp.tasks.domain.task.StartDate;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskId;
import com.bcn.asapp.tasks.domain.task.Title;
import com.bcn.asapp.tasks.domain.task.UserId;

@ExtendWith(MockitoExtension.class)
class UpdateTaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private UpdateTaskService updateTaskService;

    // Specific test data
    private final UUID taskIdValue = UUID.randomUUID();

    private final UUID userIdValue = UUID.randomUUID();

    private final String titleValue = "Updated Title";

    private final String descriptionValue = "Updated Description";

    private final Instant startDateValue = Instant.now();

    private final Instant endDateValue = Instant.now()
                                                .plusSeconds(3600);

    @Nested
    class UpdateTaskById {

        @Test
        void ThrowsRuntimeException_FetchTaskFails() {
            // Given
            var taskId = TaskId.of(taskIdValue);
            var command = new UpdateTaskCommand(taskIdValue, userIdValue, titleValue, descriptionValue, startDateValue, endDateValue);

            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .findById(taskId);

            // When
            var thrown = catchThrowable(() -> updateTaskService.updateTaskById(command));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Database connection failed");

            then(taskRepository).should(times(1))
                                .findById(taskId);
            then(taskRepository).should(never())
                                .save(any(Task.class));
        }

        @Test
        void ThrowsRuntimeException_SaveTaskFails() {
            // Given
            var taskId = TaskId.of(taskIdValue);
            var userId = UserId.of(userIdValue);
            var existingTask = Task.reconstitute(taskId, userId, Title.of("Old Title"), Description.ofNullable("Old Description"),
                    StartDate.ofNullable(startDateValue), EndDate.ofNullable(endDateValue));
            var command = new UpdateTaskCommand(taskIdValue, userIdValue, titleValue, descriptionValue, startDateValue, endDateValue);

            given(taskRepository.findById(taskId)).willReturn(Optional.of(existingTask));
            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .save(existingTask);

            // When
            var thrown = catchThrowable(() -> updateTaskService.updateTaskById(command));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Database connection failed");

            then(taskRepository).should(times(1))
                                .findById(taskId);
            then(taskRepository).should(times(1))
                                .save(existingTask);
        }

        @Test
        void ReturnsEmpty_TaskNotExists() {
            // Given
            var taskId = TaskId.of(taskIdValue);

            given(taskRepository.findById(taskId)).willReturn(Optional.empty());

            var command = new UpdateTaskCommand(taskIdValue, userIdValue, titleValue, descriptionValue, startDateValue, endDateValue);

            // When
            var result = updateTaskService.updateTaskById(command);

            // Then
            then(taskRepository).should(times(1))
                                .findById(taskId);
            then(taskRepository).should(never())
                                .save(any(Task.class));

            assertThat(result).isEmpty();
        }

        @Test
        void ReturnsUpdatedTask_TaskExists() {
            // Given
            var taskId = TaskId.of(taskIdValue);
            var userId = UserId.of(userIdValue);
            var title = Title.of(titleValue);
            var description = Description.ofNullable(descriptionValue);
            var startDate = StartDate.ofNullable(startDateValue);
            var endDate = EndDate.ofNullable(endDateValue);

            var existingTask = Task.reconstitute(taskId, userId, Title.of("Old Title"), Description.ofNullable("Old Description"), startDate, endDate);

            var updatedTask = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            given(taskRepository.findById(taskId)).willReturn(Optional.of(existingTask));
            given(taskRepository.save(existingTask)).willReturn(updatedTask);

            var command = new UpdateTaskCommand(taskIdValue, userIdValue, titleValue, descriptionValue, startDateValue, endDateValue);

            // When
            var result = updateTaskService.updateTaskById(command);

            // Then
            then(taskRepository).should(times(1))
                                .findById(taskId);
            then(taskRepository).should(times(1))
                                .save(existingTask);

            assertThat(result).isPresent();
            assertThat(result.get()
                             .getTitle()).isEqualTo(title);
            assertThat(result.get()
                             .getDescription()).isEqualTo(description);
        }

    }

}
