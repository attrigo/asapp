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

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import com.bcn.asapp.tasks.application.task.out.TaskRepository;
import com.bcn.asapp.tasks.domain.task.TaskId;

@ExtendWith(MockitoExtension.class)
class DeleteTaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DeleteTaskService deleteTaskService;

    private final UUID taskIdValue = UUID.randomUUID();

    @Nested
    class DeleteTaskById {

        @Test
        void ThrowsDataAccessException_DeleteTaskFails() {
            // Given
            var taskId = TaskId.of(taskIdValue);

            willThrow(new DataAccessException("Database connection failed") {}).given(taskRepository)
                                                                               .deleteById(taskId);

            // When
            var thrown = catchThrowable(() -> deleteTaskService.deleteTaskById(taskIdValue));

            // Then
            assertThat(thrown).isInstanceOf(DataAccessException.class)
                              .hasMessageContaining("Database connection failed");

            then(taskRepository).should(times(1))
                                .deleteById(taskId);
        }

        @Test
        void DoesNotDeleteTaskReturnsFalse_TaskNotExists() {
            // Given
            var taskId = TaskId.of(taskIdValue);

            given(taskRepository.deleteById(taskId)).willReturn(false);

            // When
            var result = deleteTaskService.deleteTaskById(taskIdValue);

            // Then
            then(taskRepository).should(times(1))
                                .deleteById(taskId);
            assertThat(result).isFalse();
        }

        @Test
        void DeletesTaskAndReturnsTrue_TaskExists() {
            // Given
            var taskId = TaskId.of(taskIdValue);

            given(taskRepository.deleteById(taskId)).willReturn(true);

            // When
            var result = deleteTaskService.deleteTaskById(taskIdValue);

            // Then
            then(taskRepository).should(times(1))
                                .deleteById(taskId);
            assertThat(result).isTrue();
        }

    }

}
