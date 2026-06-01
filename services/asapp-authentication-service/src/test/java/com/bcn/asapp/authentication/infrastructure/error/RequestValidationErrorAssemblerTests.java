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

package com.bcn.asapp.authentication.infrastructure.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;

/**
 * Tests {@link RequestValidationErrorAssembler} mapping and sorting of body-validation failures.
 * <p>
 * Coverage:
 * <li>Maps body field errors to validation errors ordered by field then message</li>
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

}
