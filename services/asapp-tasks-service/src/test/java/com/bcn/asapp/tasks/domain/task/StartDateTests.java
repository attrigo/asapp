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

class StartDateTests {

    private final Instant startDateValue = Instant.parse("2025-01-01T10:00:00Z");

    @Nested
    class CreateStartDateWithConstructor {

        @Test
        void ThrowsIllegalArgumentException_NullStartDate() {
            // When
            var thrown = catchThrowable(() -> new StartDate(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Start date must not be null");
        }

        @Test
        void ReturnsStartDate_ValidStartDate() {
            // When
            var actual = new StartDate(startDateValue);

            // Then
            assertThat(actual.startDate()).isEqualTo(startDateValue);
        }

    }

    @Nested
    class CreateStartDateWithFactoryMethod {

        @Test
        void ThrowsIllegalArgumentException_NullStartDate() {
            // When
            var thrown = catchThrowable(() -> StartDate.of(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Start date must not be null");
        }

        @Test
        void ReturnsStartDate_ValidStartDate() {
            // When
            var actual = StartDate.of(startDateValue);

            // Then
            assertThat(actual.startDate()).isEqualTo(startDateValue);
        }

    }

    @Nested
    class CreateStartDateWithNullableFactoryMethod {

        @Test
        void ReturnsNull_NullStartDate() {
            // When
            var actual = StartDate.ofNullable(null);

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ReturnsStartDate_ValidStartDate() {
            // When
            var actual = StartDate.ofNullable(startDateValue);

            // Then
            assertThat(actual.startDate()).isEqualTo(startDateValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsStartDateValue_ValidStartDate() {
            // Given
            var startDate = StartDate.of(startDateValue);

            // When
            var actual = startDate.value();

            // Then
            assertThat(actual).isEqualTo(startDateValue);
        }

    }

}
