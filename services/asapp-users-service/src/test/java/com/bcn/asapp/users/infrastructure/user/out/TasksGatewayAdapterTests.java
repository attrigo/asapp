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

package com.bcn.asapp.users.infrastructure.user.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import com.bcn.asapp.clients.tasks.TasksHttpClient;
import com.bcn.asapp.clients.tasks.response.TasksByUserIdResponse;
import com.bcn.asapp.users.domain.user.UserId;

/**
 * Tests {@link TasksGatewayAdapter} task id mapping and graceful degradation.
 * <p>
 * Coverage:
 * <li>Maps task responses to their task ids</li>
 * <li>Returns an empty list when the user has no tasks</li>
 * <li>Returns an empty list when the client yields a null response body</li>
 * <li>Returns an empty list when the Tasks Service call fails</li>
 */
@ExtendWith(MockitoExtension.class)
class TasksGatewayAdapterTests {

    @Mock
    private TasksHttpClient tasksHttpClient;

    @InjectMocks
    private TasksGatewayAdapter tasksGatewayAdapter;

    @Nested
    class GetTaskIdsByUserId {

        @Test
        void ReturnsTaskIds_UserHasTasks() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            var taskId1 = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
            var taskId2 = UUID.fromString("660e8400-e29b-41d4-a716-446655440002");
            given(tasksHttpClient.getTasksByUserId(userId.value())).willReturn(List.of(new TasksByUserIdResponse(taskId1), new TasksByUserIdResponse(taskId2)));

            // When
            var actual = tasksGatewayAdapter.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).containsExactly(taskId1, taskId2);
        }

        @Test
        void ReturnsEmptyList_UserHasNoTasks() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            given(tasksHttpClient.getTasksByUserId(userId.value())).willReturn(List.of());

            // When
            var actual = tasksGatewayAdapter.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsEmptyList_NullResponse() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            given(tasksHttpClient.getTasksByUserId(userId.value())).willReturn(null);

            // When
            var actual = tasksGatewayAdapter.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsEmptyList_TasksServiceFails() {
            // Given
            var userId = UserId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            given(tasksHttpClient.getTasksByUserId(userId.value())).willThrow(new RestClientException("connection refused"));

            // When
            var actual = tasksGatewayAdapter.getTaskIdsByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

    }

}
