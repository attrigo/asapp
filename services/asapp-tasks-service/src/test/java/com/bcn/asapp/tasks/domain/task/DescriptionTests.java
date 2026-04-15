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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link Description} validation, nullable factory, and value access.
 * <p>
 * Coverage:
 * <li>Rejects null or blank description values</li>
 * <li>Accepts valid inputs through constructor and factory method</li>
 * <li>Returns null from nullable factory for null, empty, or blank input</li>
 * <li>Provides access to wrapped description value</li>
 */
class DescriptionTests {

    @Nested
    class CreateDescriptionWithConstructor {

        @Test
        void ReturnsDescription_ValidDescription() {
            // Given
            var description = "Description";

            // When
            var actual = new Description(description);

            // Then
            assertThat(actual.description()).isEqualTo(description);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankDescription(String description) {
            // When
            var actual = catchThrowable(() -> new Description(description));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Description must not be null or empty");
        }

    }

    @Nested
    class CreateDescriptionWithFactoryMethod {

        @Test
        void ReturnsDescription_ValidDescription() {
            // Given
            var description = "Description";

            // When
            var actual = Description.of(description);

            // Then
            assertThat(actual.description()).isEqualTo(description);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankDescription(String description) {
            // When
            var actual = catchThrowable(() -> Description.of(description));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Description must not be null or empty");
        }

    }

    @Nested
    class CreateDescriptionWithNullableFactoryMethod {

        @Test
        void ReturnsDescription_ValidDescription() {
            // Given
            var description = "Description";

            // When
            var actual = Description.ofNullable(description);

            // Then
            assertThat(actual.description()).isEqualTo(description);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ReturnsNull_NullOrBlankDescription(String description) {
            // When
            var actual = Description.ofNullable(description);

            // Then
            assertThat(actual).isNull();
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsDescriptionValue_ValidDescription() {
            // Given
            var descriptionValue = "Description";
            var description = Description.of(descriptionValue);

            // When
            var actual = description.value();

            // Then
            assertThat(actual).isEqualTo(descriptionValue);
        }

    }

}
