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

import java.time.Instant;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link StartDate} validation, nullable factory, and value access.
 * <p>
 * Coverage:
 * <li>Rejects null start date values</li>
 * <li>Accepts valid inputs through constructor and factory method</li>
 * <li>Returns null from nullable factory for null input</li>
 * <li>Provides access to wrapped Instant value</li>
 */
class StartDateTests {

    @Nested
    class CreateStartDateWithConstructor {

        @Test
        void ReturnsStartDate_ValidStartDate() {
            // Given
            var startDate = Instant.parse("2025-01-01T10:00:00Z");

            // When
            var actual = new StartDate(startDate);

            // Then
            assertThat(actual.startDate()).isEqualTo(startDate);
        }

        @Test
        void ThrowsIllegalArgumentException_NullStartDate() {
            // When
            var actual = catchThrowable(() -> new StartDate(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Start date must not be null");
        }

    }

    @Nested
    class CreateStartDateWithFactoryMethod {

        @Test
        void ReturnsStartDate_ValidStartDate() {
            // Given
            var startDate = Instant.parse("2025-01-01T10:00:00Z");

            // When
            var actual = StartDate.of(startDate);

            // Then
            assertThat(actual.startDate()).isEqualTo(startDate);
        }

        @Test
        void ThrowsIllegalArgumentException_NullStartDate() {
            // When
            var actual = catchThrowable(() -> StartDate.of(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Start date must not be null");
        }

    }

    @Nested
    class CreateStartDateWithNullableFactoryMethod {

        @Test
        void ReturnsStartDate_ValidStartDate() {
            // Given
            var startDate = Instant.parse("2025-01-01T10:00:00Z");

            // When
            var actual = StartDate.ofNullable(startDate);

            // Then
            assertThat(actual.startDate()).isEqualTo(startDate);
        }

        @Test
        void ReturnsNull_NullStartDate() {
            // When
            var actual = StartDate.ofNullable(null);

            // Then
            assertThat(actual).isNull();
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsStartDateValue_ValidStartDate() {
            // Given
            var startDateValue = Instant.parse("2025-01-01T10:00:00Z");
            var startDate = StartDate.of(startDateValue);

            // When
            var actual = startDate.value();

            // Then
            assertThat(actual).isEqualTo(startDateValue);
        }

    }

}
