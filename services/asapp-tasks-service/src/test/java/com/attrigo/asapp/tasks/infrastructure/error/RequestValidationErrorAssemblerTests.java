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

package com.attrigo.asapp.tasks.infrastructure.error;

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
 * <li>Preserves the full field path, stripping only the method-name prefix from the property path</li>
 * <li>Keeps duplicate leaf field names distinct via their full paths</li>
 * <li>Returns an empty list when there are no failures</li>
 */
class RequestValidationErrorAssemblerTests {

    @Nested
    class FromFieldErrors {

        @Test
        void ReturnsSortedValidationErrors_MultipleFieldErrors() {
            // Given
            var idFieldError = new FieldError("parameter", "id", "must be a valid UUID");
            var termFieldError = new FieldError("parameter", "term", "must not be blank");
            var nameFieldError = new FieldError("parameter", "name", "must not be empty");
            var fieldErrors = List.of(idFieldError, termFieldError, nameFieldError);
            var sortedErrors = List.of(RequestValidationError.of("id", "must be a valid UUID"), RequestValidationError.of("name", "must not be empty"),
                    RequestValidationError.of("term", "must not be blank"));

            // When
            var actual = RequestValidationErrorAssembler.fromFieldErrors(fieldErrors);

            // Then
            assertThat(actual).containsExactlyElementsOf(sortedErrors);
        }

        @Test
        void ReturnsSortedValidationErrors_SameFieldDifferentMessages() {
            // Given
            var nameSizeFieldError = new FieldError("parameter", "name", "size must be between 3 and 30");
            var nameBlankFieldError = new FieldError("parameter", "name", "must not be blank");
            var fieldErrors = List.of(nameSizeFieldError, nameBlankFieldError);
            var sortedErrors = List.of(RequestValidationError.of("name", "must not be blank"),
                    RequestValidationError.of("name", "size must be between 3 and 30"));

            // When
            var actual = RequestValidationErrorAssembler.fromFieldErrors(fieldErrors);

            // Then
            assertThat(actual).containsExactlyElementsOf(sortedErrors);
        }

        @Test
        void ReturnsSortedValidationErrors_NestedFieldErrors() {
            // Given
            var idFieldError = new FieldError("parameter", "data.id", "must be a valid UUID");
            var termFieldError = new FieldError("parameter", "data.term", "must not be blank");
            var nameFieldError = new FieldError("parameter", "data.nested.name", "must not be empty");
            var emailFieldError = new FieldError("parameter", "data.nested.email", "must be a valid email");
            var fieldErrors = List.of(idFieldError, termFieldError, nameFieldError, emailFieldError);
            var sortedErrors = List.of(RequestValidationError.of("data.id", "must be a valid UUID"),
                    RequestValidationError.of("data.nested.email", "must be a valid email"), RequestValidationError.of("data.nested.name", "must not be empty"),
                    RequestValidationError.of("data.term", "must not be blank"));

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
            var violations = Set.of(violation("search.term", "must not be blank"), violation("search.name", "must not be empty"));
            var sortedErrors = List.of(RequestValidationError.of("name", "must not be empty"), RequestValidationError.of("term", "must not be blank"));

            // When
            var actual = RequestValidationErrorAssembler.fromConstraintViolations(violations);

            // Then
            assertThat(actual).containsExactlyElementsOf(sortedErrors);
        }

        @Test
        void ReturnsSortedValidationErrors_NestedPropertyPaths() {
            // Given
            var violations = Set.of(violation("search.filter.term", "must not be blank"), violation("search.filter.name", "must not be empty"));
            var sortedErrors = List.of(RequestValidationError.of("filter.name", "must not be empty"),
                    RequestValidationError.of("filter.term", "must not be blank"));

            // When
            var actual = RequestValidationErrorAssembler.fromConstraintViolations(violations);

            // Then
            assertThat(actual).containsExactlyElementsOf(sortedErrors);
        }

        @Test
        void ReturnsDistinctValidationErrors_DuplicateLeafFieldNames() {
            // Given
            var violations = Set.of(violation("search.term", "must not be blank"), violation("search.filter.term", "must not be blank"));
            var sortedErrors = List.of(RequestValidationError.of("filter.term", "must not be blank"), RequestValidationError.of("term", "must not be blank"));

            // When
            var actual = RequestValidationErrorAssembler.fromConstraintViolations(violations);

            // Then
            assertThat(actual).containsExactlyElementsOf(sortedErrors);
        }

        @Test
        void ReturnsSortedValidationErrors_PathsWithoutDot() {
            // Given
            var violations = Set.of(violation("term", "must not be blank"), violation("name", "must not be empty"));
            var sortedErrors = List.of(RequestValidationError.of("name", "must not be empty"), RequestValidationError.of("term", "must not be blank"));

            // When
            var actual = RequestValidationErrorAssembler.fromConstraintViolations(violations);

            // Then
            assertThat(actual).containsExactlyElementsOf(sortedErrors);
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
