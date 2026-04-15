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

package com.bcn.asapp.tasks.domain.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link TaskId} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null identifier via constructor and factory method</li>
 * <li>Accepts valid UUIDs through constructor and factory method</li>
 * <li>Provides access to wrapped identifier value</li>
 */
class TaskIdTests {

    @Nested
    class CreateTaskIdWithConstructor {

        @Test
        void ReturnsTaskId_ValidId() {
            // Given
            var taskId = UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d");

            // When
            var actual = new TaskId(taskId);

            // Then
            assertThat(actual.id()).isEqualTo(taskId);
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // When
            var actual = catchThrowable(() -> new TaskId(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Task ID must not be null");
        }

    }

    @Nested
    class CreateTaskIdWithFactoryMethod {

        @Test
        void ReturnsTaskId_ValidId() {
            // Given
            var taskId = UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d");

            // When
            var actual = TaskId.of(taskId);

            // Then
            assertThat(actual.id()).isEqualTo(taskId);
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // When
            var actual = catchThrowable(() -> TaskId.of(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Task ID must not be null");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsTaskIdValue_ValidTaskId() {
            // Given
            var taskIdValue = UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d");
            var taskId = TaskId.of(taskIdValue);

            // When
            var actual = taskId.value();

            // Then
            assertThat(actual).isEqualTo(taskIdValue);
        }

    }

}
