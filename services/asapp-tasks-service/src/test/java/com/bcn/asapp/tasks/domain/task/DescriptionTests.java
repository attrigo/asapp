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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class DescriptionTests {

    private final String descriptionValue = "Description";

    @Nested
    class CreateDescriptionWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenDescriptionIsNullOrEmpty(String description) {
            // When
            var thrown = catchThrowable(() -> new Description(description));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Description must not be null or empty");
        }

        @Test
        void ThenReturnsDescription_GivenDescriptionIsValid() {
            // When
            var actual = new Description(descriptionValue);

            // Then
            assertThat(actual.description()).isEqualTo(descriptionValue);
        }

    }

    @Nested
    class CreateDescriptionWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenDescriptionIsNullOrEmpty(String description) {
            // When
            var thrown = catchThrowable(() -> Description.of(description));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Description must not be null or empty");
        }

        @Test
        void ThenReturnsDescription_GivenDescriptionIsValid() {
            // When
            var actual = Description.of(descriptionValue);

            // Then
            assertThat(actual.description()).isEqualTo(descriptionValue);
        }

    }

    @Nested
    class CreateDescriptionWithNullableFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenReturnsNull_GivenDescriptionIsNullOrEmpty(String description) {
            // When
            var actual = Description.ofNullable(description);

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ThenReturnsNull_GivenDescriptionIsBlank() {
            // When
            var actual = Description.ofNullable("   ");

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ThenReturnsDescription_GivenDescriptionIsValid() {
            // When
            var actual = Description.ofNullable(descriptionValue);

            // Then
            assertThat(actual.description()).isEqualTo(descriptionValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsDescriptionValue_GivenDescriptionIsValid() {
            // Given
            var description = Description.of(descriptionValue);

            // When
            var actual = description.value();

            // Then
            assertThat(actual).isEqualTo(descriptionValue);
        }

    }

}
