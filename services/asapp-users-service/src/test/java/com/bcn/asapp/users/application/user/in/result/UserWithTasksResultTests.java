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

package com.bcn.asapp.users.application.user.in.result;

import static com.bcn.asapp.users.testutil.fixture.UserMother.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link UserWithTasksResult} construction and null validation.
 * <p>
 * Coverage:
 * <li>Creates result with user and task identifiers</li>
 * <li>Creates result with user and empty task list</li>
 * <li>Rejects null user</li>
 * <li>Rejects null task identifier list</li>
 */
class UserWithTasksResultTests {

    @Nested
    class CreateUserWithTasksResult {

        @Test
        void ReturnsUserWithTasksResult_ValidUserAndTaskIds() {
            // Given
            var user = aUser();
            var taskId1 = UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef0123456789");
            var taskId2 = UUID.fromString("b2c3d4e5-f6a7-4890-bcde-f01234567890");
            var taskIds = List.of(taskId1, taskId2);

            // When
            var actual = new UserWithTasksResult(user, taskIds);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.user()).as("user").isEqualTo(user);
                softly.assertThat(actual.taskIds()).as("task IDs").containsExactly(taskId1, taskId2);
                // @formatter:on
            });
        }

        @Test
        void ReturnsUserWithTasksResult_ValidUserAndEmptyTaskIds() {
            // Given
            var user = aUser();
            var taskIds = List.<UUID>of();

            // When
            var actual = new UserWithTasksResult(user, taskIds);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.user()).as("user").isEqualTo(user);
                softly.assertThat(actual.taskIds()).as("task IDs").isEmpty();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullUser() {
            // Given
            var taskIds = List.<UUID>of();

            // When
            var actual = catchThrowable(() -> new UserWithTasksResult(null, taskIds));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTaskIds() {
            // Given
            var user = aUser();

            // When
            var actual = catchThrowable(() -> new UserWithTasksResult(user, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Task IDs list must not be null");
        }

    }

}
