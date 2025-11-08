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

package com.bcn.asapp.tasks.domain.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TaskIdTests {

    private final UUID idValue = UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d");;

    @Nested
    class CreateTaskIdWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> new TaskId(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Task ID must not be null");
        }

        @Test
        void ThenReturnsTaskId_GivenIdIsValid() {
            // When
            var actual = new TaskId(idValue);

            // Then
            assertThat(actual.id()).isEqualTo(idValue);
        }

    }

    @Nested
    class CreateTaskIdWithFactoryMethod {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> TaskId.of(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Task ID must not be null");
        }

        @Test
        void ThenReturnsTaskId_GivenIdIsValid() {
            // When
            var actual = TaskId.of(idValue);

            // Then
            assertThat(actual.id()).isEqualTo(idValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsTaskIdValue_GivenTaskIdIsValid() {
            // Given
            var taskId = TaskId.of(idValue);

            // When
            var actual = taskId.value();

            // Then
            assertThat(actual).isEqualTo(idValue);
        }

    }

}
