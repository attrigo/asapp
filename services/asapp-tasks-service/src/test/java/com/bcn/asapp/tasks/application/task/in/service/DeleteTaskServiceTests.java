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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.TaskId;

/**
 * Tests {@link DeleteTaskService} task deletion and failure propagation.
 * <p>
 * Coverage:
 * <li>Deletion failures propagate without completing workflow</li>
 * <li>Returns false when task does not exist</li>
 * <li>Returns true when task successfully deleted</li>
 */
@ExtendWith(MockitoExtension.class)
class DeleteTaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DeleteTaskService deleteTaskService;

    @Nested
    class DeleteTaskById {

        @Test
        void ReturnsTrue_TaskExists() {
            // Given
            var taskIdValue = UUID.fromString("c3d4e5f6-a7b8-4901-c2d3-e4f5a6b7c8d9");
            var taskId = TaskId.of(taskIdValue);

            given(taskRepository.deleteById(taskId)).willReturn(true);

            // When
            var actual = deleteTaskService.deleteTaskById(taskIdValue);

            // Then
            assertThat(actual).isTrue();

            then(taskRepository).should(times(1))
                                .deleteById(taskId);
        }

        @Test
        void ReturnsFalse_TaskNotExists() {
            // Given
            var taskIdValue = UUID.fromString("c3d4e5f6-a7b8-4901-c2d3-e4f5a6b7c8d9");
            var taskId = TaskId.of(taskIdValue);

            given(taskRepository.deleteById(taskId)).willReturn(false);

            // When
            var actual = deleteTaskService.deleteTaskById(taskIdValue);

            // Then
            assertThat(actual).isFalse();

            then(taskRepository).should(times(1))
                                .deleteById(taskId);
        }

        @Test
        void ThrowsRuntimeException_TaskDeletionFails() {
            // Given
            var taskIdValue = UUID.fromString("c3d4e5f6-a7b8-4901-c2d3-e4f5a6b7c8d9");
            var taskId = TaskId.of(taskIdValue);

            willThrow(new RuntimeException("Database connection failed")).given(taskRepository)
                                                                         .deleteById(taskId);

            // When
            var actual = catchThrowable(() -> deleteTaskService.deleteTaskById(taskIdValue));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Database connection failed");

            then(taskRepository).should(times(1))
                                .deleteById(taskId);
        }

    }

}
