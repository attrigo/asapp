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
 * Tests {@link Title} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null or blank title values</li>
 * <li>Accepts valid inputs through constructor and factory method</li>
 * <li>Provides access to wrapped title value</li>
 */
class TitleTests {

    @Nested
    class CreateTitleWithConstructor {

        @Test
        void ReturnsTitle_ValidTitle() {
            // Given
            var title = "Title";

            // When
            var actual = new Title(title);

            // Then
            assertThat(actual.title()).isEqualTo(title);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankTitle(String title) {
            // When
            var actual = catchThrowable(() -> new Title(title));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null or empty");
        }

    }

    @Nested
    class CreateTitleWithFactoryMethod {

        @Test
        void ReturnsTitle_ValidTitle() {
            // Given
            var title = "Title";

            // When
            var actual = Title.of(title);

            // Then
            assertThat(actual.title()).isEqualTo(title);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankTitle(String title) {
            // When
            var actual = catchThrowable(() -> Title.of(title));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null or empty");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsTitleValue_ValidTitle() {
            // Given
            var titleValue = "Title";
            var title = Title.of(titleValue);

            // When
            var actual = title.value();

            // Then
            assertThat(actual).isEqualTo(titleValue);
        }

    }

}
