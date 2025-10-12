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

package com.bcn.asapp.authentication.domain.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class SubjectTests {

    private final String subjectValue = "subject";

    @Nested
    class CreateSubjectWithConstructor {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenSubjectIsNullOrEmpty(String subject) {
            // When
            var thrown = catchThrowable(() -> new Subject(subject));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must not be null or empty");
        }

        @Test
        void ThenReturnsSubject_GivenSubjectIsValid() {
            // When
            var actual = new Subject(subjectValue);

            // Then
            assertThat(actual.subject()).isEqualTo(subjectValue);
        }

    }

    @Nested
    class CreateSubjectWithFactoryMethod {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenSubjectIsNullOrEmpty(String subject) {
            // When
            var thrown = catchThrowable(() -> Subject.of(subject));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must not be null or empty");
        }

        @Test
        void ThenReturnsSubject_GivenSubjectIsValid() {
            // When
            var actual = Subject.of(subjectValue);

            // Then
            assertThat(actual.subject()).isEqualTo(subjectValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsSubjectValue_GivenSubjectIsValid() {
            // Given
            var subject = Subject.of(subjectValue);

            // When
            var actual = subject.value();

            // Then
            assertThat(actual).isEqualTo(subjectValue);
        }

    }

}
