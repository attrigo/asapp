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

package com.bcn.asapp.tasks.infrastructure.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;

/**
 * Tests {@link RequestValidationErrorAssembler} mapping and sorting of body and method-parameter validation failures.
 * <p>
 * Coverage:
 * <li>Maps body field errors to validation errors ordered by field then message</li>
 * <li>Maps method-parameter constraint violations to validation errors ordered by field then message</li>
 * <li>Uses the trailing property-path segment as the field name, or the full path when there is none</li>
 * <li>Returns an empty list when there are no failures</li>
 */
class RequestValidationErrorAssemblerTests {

    @Nested
    class FromFieldErrors {

        @Test
        void ReturnsSortedValidationErrors_MultipleFieldErrors() {
            // Given
            var nameSizeFieldError = new FieldError("request", "name", "size must be between 3 and 30");
            var nameBlankFieldError = new FieldError("request", "name", "must not be blank");
            var emailBlankFieldError = new FieldError("request", "email", "must not be blank");
            var fieldErrors = List.of(nameSizeFieldError, nameBlankFieldError, emailBlankFieldError);
            var sortedErrors = List.of(RequestValidationError.of("email", "must not be blank"), RequestValidationError.of("name", "must not be blank"),
                    RequestValidationError.of("name", "size must be between 3 and 30"));

            // When
            var actual = RequestValidationErrorAssembler.fromFieldErrors(fieldErrors);

            // Then
            assertThat(actual).containsExactlyElementsOf(sortedErrors);
        }

        @Test
        void ReturnsEmptyList_NoFieldErrors() {
            // When
            var actual = RequestValidationErrorAssembler.fromFieldErrors(List.of());

            // Then
            assertThat(actual).isEmpty();
        }

    }

    @Nested
    class FromConstraintViolations {

        @Test
        void ReturnsSortedValidationErrors_MultipleConstraintViolations() {
            // Given
            var violations = Set.of(violation("searchById.term", "must not be blank"), violation("searchById.id", "must be a valid UUID"));
            var sortedErrors = List.of(RequestValidationError.of("id", "must be a valid UUID"), RequestValidationError.of("term", "must not be blank"));

            // When
            var actual = RequestValidationErrorAssembler.fromConstraintViolations(violations);

            // Then
            assertThat(actual).containsExactlyElementsOf(sortedErrors);
        }

        @Test
        void ReturnsValidationError_PathWithoutDot() {
            // Given
            var violations = Set.<ConstraintViolation<?>>of(violation("term", "must not be blank"));

            // When
            var actual = RequestValidationErrorAssembler.fromConstraintViolations(violations);

            // Then
            assertThat(actual).containsExactly(RequestValidationError.of("term", "must not be blank"));
        }

        @Test
        void ReturnsEmptyList_NoConstraintViolations() {
            // When
            var actual = RequestValidationErrorAssembler.fromConstraintViolations(Set.of());

            // Then
            assertThat(actual).isEmpty();
        }

        private static ConstraintViolation<?> violation(String propertyPath, String message) {
            Path path = mock(Path.class);
            given(path.toString()).willReturn(propertyPath);
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            given(violation.getPropertyPath()).willReturn(path);
            given(violation.getMessage()).willReturn(message);
            return violation;
        }

    }

}
