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
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.tasks.application.task.in.command.CreateTaskCommand;
import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.Description;
import com.bcn.asapp.tasks.domain.task.EndDate;
import com.bcn.asapp.tasks.domain.task.StartDate;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskId;
import com.bcn.asapp.tasks.domain.task.Title;
import com.bcn.asapp.tasks.domain.task.UserId;

@ExtendWith(MockitoExtension.class)
class CreateTaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private CreateTaskService createTaskService;

    // Specific test data
    private final UUID userIdValue = UUID.randomUUID();

    private final String titleValue = "Task Title";

    private final String descriptionValue = "Task Description";

    private final Instant startDateValue = Instant.now();

    private final Instant endDateValue = Instant.now()
                                                .plusSeconds(3600);

    @Nested
    class CreateTask {

        @Test
        void ThrowsRuntimeException_SaveTaskFails() {
            // Given
            var command = new CreateTaskCommand(userIdValue, titleValue, descriptionValue, startDateValue, endDateValue);

            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .save(any(Task.class));

            // When
            var thrown = catchThrowable(() -> createTaskService.createTask(command));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Database connection failed");

            then(taskRepository).should(times(1))
                                .save(any(Task.class));
        }

        @Test
        void ReturnsCreatedTask_CreationSucceeds() {
            // Given
            var userId = UserId.of(userIdValue);
            var title = Title.of(titleValue);
            var description = Description.ofNullable(descriptionValue);
            var startDate = StartDate.ofNullable(startDateValue);
            var endDate = EndDate.ofNullable(endDateValue);
            var savedTask = Task.reconstitute(TaskId.of(UUID.randomUUID()), userId, title, description, startDate, endDate);
            var command = new CreateTaskCommand(userIdValue, titleValue, descriptionValue, startDateValue, endDateValue);

            given(taskRepository.save(any(Task.class))).willReturn(savedTask);

            // When
            var result = createTaskService.createTask(command);

            // Then
            then(taskRepository).should(times(1))
                                .save(any(Task.class));
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo(title);
        }

    }

}
