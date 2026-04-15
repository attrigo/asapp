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

package com.bcn.asapp.authentication.domain.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link Subject} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null or blank subject values</li>
 * <li>Validates subject must be a valid email address format</li>
 * <li>Accepts valid inputs through constructor and factory method</li>
 * <li>Provides access to wrapped subject value</li>
 */
class SubjectTests {

    @Nested
    class CreateSubjectWithConstructor {

        @Test
        void ReturnsSubject_ValidSubject() {
            // Given
            var subject = "user@asapp.com";

            // When
            var actual = new Subject(subject);

            // Then
            assertThat(actual.subject()).isEqualTo(subject);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankSubject(String subject) {
            // When
            var actual = catchThrowable(() -> new Subject(subject));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must not be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = { "notanemail", "@nodomain.com", "user@", "user@@domain.com" })
        void ThrowsIllegalArgumentException_SubjectNotEmail(String subject) {
            // When
            var actual = catchThrowable(() -> new Subject(subject));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must be a valid email address");
        }

    }

    @Nested
    class CreateSubjectWithFactoryMethod {

        @Test
        void ReturnsSubject_ValidSubject() {
            // Given
            var subject = "user@asapp.com";

            // When
            var actual = Subject.of(subject);

            // Then
            assertThat(actual.subject()).isEqualTo(subject);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "\t" })
        void ThrowsIllegalArgumentException_NullOrBlankSubject(String subject) {
            // When
            var actual = catchThrowable(() -> Subject.of(subject));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must not be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = { "notanemail", "@nodomain.com", "user@", "user@@domain.com" })
        void ThrowsIllegalArgumentException_SubjectNotEmail(String subject) {
            // When
            var actual = catchThrowable(() -> Subject.of(subject));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must be a valid email address");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsSubjectValue_ValidSubject() {
            // Given
            var subjectValue = "user@asapp.com";
            var subject = Subject.of(subjectValue);

            // When
            var actual = subject.value();

            // Then
            assertThat(actual).isEqualTo(subjectValue);
        }

    }

}
