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

import java.time.Instant;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link EndDate} validation, nullable factory, and value access.
 * <p>
 * Coverage:
 * <li>Rejects null end date values</li>
 * <li>Accepts valid inputs through constructor and factory method</li>
 * <li>Returns null from nullable factory for null input</li>
 * <li>Provides access to wrapped Instant value</li>
 */
class EndDateTests {

    @Nested
    class CreateEndDateWithConstructor {

        @Test
        void ReturnsEndDate_ValidEndDate() {
            // Given
            var endDate = Instant.parse("2025-01-02T10:00:00Z");

            // When
            var actual = new EndDate(endDate);

            // Then
            assertThat(actual.endDate()).isEqualTo(endDate);
        }

        @Test
        void ThrowsIllegalArgumentException_NullEndDate() {
            // When
            var actual = catchThrowable(() -> new EndDate(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("End date must not be null");
        }

    }

    @Nested
    class CreateEndDateWithFactoryMethod {

        @Test
        void ReturnsEndDate_ValidEndDate() {
            // Given
            var endDate = Instant.parse("2025-01-02T10:00:00Z");

            // When
            var actual = EndDate.of(endDate);

            // Then
            assertThat(actual.endDate()).isEqualTo(endDate);
        }

        @Test
        void ThrowsIllegalArgumentException_NullEndDate() {
            // When
            var actual = catchThrowable(() -> EndDate.of(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("End date must not be null");
        }

    }

    @Nested
    class CreateEndDateWithNullableFactoryMethod {

        @Test
        void ReturnsEndDate_ValidEndDate() {
            // Given
            var endDate = Instant.parse("2025-01-02T10:00:00Z");

            // When
            var actual = EndDate.ofNullable(endDate);

            // Then
            assertThat(actual.endDate()).isEqualTo(endDate);
        }

        @Test
        void ReturnsNull_NullEndDate() {
            // When
            var actual = EndDate.ofNullable(null);

            // Then
            assertThat(actual).isNull();
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsEndDateValue_ValidEndDate() {
            // Given
            var endDateValue = Instant.parse("2025-01-02T10:00:00Z");
            var endDate = EndDate.of(endDateValue);

            // When
            var actual = endDate.value();

            // Then
            assertThat(actual).isEqualTo(endDateValue);
        }

    }

}
