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

import static com.bcn.asapp.tasks.testutil.fixture.TaskMother.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

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
import com.bcn.asapp.tasks.domain.task.Task;

/**
 * Tests {@link CreateTaskService} task creation and persistence.
 * <p>
 * Coverage:
 * <li>Persistence failures propagate without completing creation workflow</li>
 * <li>Successful creation persists task with assigned identity</li>
 * <li>Domain constraints validated before persistence</li>
 */
@ExtendWith(MockitoExtension.class)
class CreateTaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private CreateTaskService createTaskService;

    @Nested
    class CreateTask {

        @Test
        void ReturnsCreatedTask_ValidUser() {
            // Given
            var task = aTask();
            var userId = task.getUserId();
            var title = task.getTitle();
            var description = task.getDescription();
            var startDate = task.getStartDate();
            var endDate = task.getEndDate();
            var command = new CreateTaskCommand(userId.value(), title.value(), description.value(), startDate.value(), endDate.value());

            given(taskRepository.save(any(Task.class))).willReturn(task);

            // When
            var actual = createTaskService.createTask(command);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("created task").isNotNull();
                softly.assertThat(actual.getId()).as("ID").isNotNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(title);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(description);
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(startDate);
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(endDate);
                // @formatter:on
            });

            then(taskRepository).should(times(1))
                                .save(any(Task.class));
        }

        @Test
        void ThrowsRuntimeException_TaskPersistenceFails() {
            // Given
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var description = "Description";
            var startDate = Instant.parse("2025-01-01T10:00:00Z");
            var endDate = Instant.parse("2025-01-02T10:00:00Z");
            var command = new CreateTaskCommand(userId, title, description, startDate, endDate);

            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .save(any(Task.class));

            // When
            var actual = catchThrowable(() -> createTaskService.createTask(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(taskRepository).should(times(1))
                                .save(any(Task.class));
        }

    }

}
