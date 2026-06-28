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

package com.attrigo.asapp.tasks.infrastructure.task.in;

import static com.attrigo.asapp.tasks.testutil.fixture.TaskMother.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.attrigo.asapp.tasks.application.task.in.ReadTaskUseCase;
import com.attrigo.asapp.tasks.domain.task.Task;
import com.attrigo.asapp.tasks.infrastructure.task.in.response.GetTasksResponse;
import com.attrigo.asapp.tasks.infrastructure.task.mapper.TaskMapper;

/**
 * Tests {@link TaskRestController} request dispatch between retrieving all tasks and retrieving tasks by identifiers.
 * <p>
 * Coverage:
 * <li>Retrieves all tasks when no identifiers are supplied</li>
 * <li>Retrieves only the requested tasks when identifiers are supplied</li>
 * <li>Skips the unused retrieval path on each branch</li>
 */
@ExtendWith(MockitoExtension.class)
class TaskRestControllerTests {

    @Mock
    private ReadTaskUseCase readTaskUseCase;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskRestController taskRestController;

    @Nested
    class GetTasks {

        @Test
        void ReturnsAllTasks_NoIds() {
            // Given
            var task = aTask();
            var response = buildGetTasksResponse(task);

            given(readTaskUseCase.getAllTasks()).willReturn(List.of(task));
            given(taskMapper.toGetTasksResponse(task)).willReturn(response);

            // When
            var actual = taskRestController.getTasks(null);

            // Then
            assertThat(actual).containsExactly(response);

            then(readTaskUseCase).should()
                                 .getAllTasks();
            then(readTaskUseCase).should(never())
                                 .getTasksByIds(any());
        }

        @Test
        void ReturnsTasksByIds_WithIds() {
            // Given
            var task = aTask();
            var taskIds = List.of(task.getId()
                                      .value());
            var response = buildGetTasksResponse(task);

            given(readTaskUseCase.getTasksByIds(taskIds)).willReturn(List.of(task));
            given(taskMapper.toGetTasksResponse(task)).willReturn(response);

            // When
            var actual = taskRestController.getTasks(taskIds);

            // Then
            assertThat(actual).containsExactly(response);

            then(readTaskUseCase).should()
                                 .getTasksByIds(taskIds);
            then(readTaskUseCase).should(never())
                                 .getAllTasks();
        }

    }

    private static GetTasksResponse buildGetTasksResponse(Task task) {
        var id = task.getId();
        var userId = task.getUserId();
        var title = task.getTitle();
        var description = task.getDescription();
        var startDate = task.getStartDate();
        var endDate = task.getEndDate();
        return new GetTasksResponse(id.value(), userId.value(), title.value(), description.value(), startDate.value(), endDate.value());
    }

}
