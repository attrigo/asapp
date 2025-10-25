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

class TitleTests {

    private final String titleValue = "title";

    @Nested
    class CreateTitleWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenTitleIsNullOrEmpty(String title) {
            // When
            var thrown = catchThrowable(() -> new Title(title));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null or empty");
        }

        @Test
        void ThenReturnsTitle_GivenTitleIsValid() {
            // When
            var actual = new Title(titleValue);

            // Then
            assertThat(actual.title()).isEqualTo(titleValue);
        }

    }

    @Nested
    class CreateTitleWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenTitleIsNullOrEmpty(String title) {
            // When
            var thrown = catchThrowable(() -> Title.of(title));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null or empty");
        }

        @Test
        void ThenReturnsTitle_GivenTitleIsValid() {
            // When
            var actual = Title.of(titleValue);

            // Then
            assertThat(actual.title()).isEqualTo(titleValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsTitleValue_GivenTitleIsValid() {
            // Given
            var title = Title.of(titleValue);

            // When
            var actual = title.value();

            // Then
            assertThat(actual).isEqualTo(titleValue);
        }

    }

}
