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

class EndDateTests {

    private final Instant endDateValue = Instant.parse("2025-01-02T10:00:00Z");

    @Nested
    class CreateEndDateWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenEndDateIsNull() {
            // When
            var thrown = catchThrowable(() -> new EndDate(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("End date must not be null");
        }

        @Test
        void ThenReturnsEndDate_GivenEndDateIsValid() {
            // When
            var actual = new EndDate(endDateValue);

            // Then
            assertThat(actual.endDate()).isEqualTo(endDateValue);
        }

    }

    @Nested
    class CreateEndDateWithFactoryMethod {

        @Test
        void ThenThrowsIllegalArgumentException_GivenEndDateIsNull() {
            // When
            var thrown = catchThrowable(() -> EndDate.of(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("End date must not be null");
        }

        @Test
        void ThenReturnsEndDate_GivenEndDateIsValid() {
            // When
            var actual = EndDate.of(endDateValue);

            // Then
            assertThat(actual.endDate()).isEqualTo(endDateValue);
        }

    }

    @Nested
    class CreateEndDateWithNullableFactoryMethod {

        @Test
        void ThenReturnsNull_GivenEndDateIsNull() {
            // When
            var actual = EndDate.ofNullable(null);

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ThenReturnsEndDate_GivenEndDateIsValid() {
            // When
            var actual = EndDate.ofNullable(endDateValue);

            // Then
            assertThat(actual.endDate()).isEqualTo(endDateValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsEndDateValue_GivenEndDateIsValid() {
            // Given
            var endDate = EndDate.of(endDateValue);

            // When
            var actual = endDate.value();

            // Then
            assertThat(actual).isEqualTo(endDateValue);
        }

    }

}
