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

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.validation.FieldError;

import jakarta.validation.ConstraintViolation;

/**
 * Assembles sorted {@link RequestValidationError} lists from Spring and Jakarta validation failures.
 * <p>
 * Maps body field errors and method-parameter constraint violations to {@link RequestValidationError}, preserving the full field path so nested or duplicate
 * field names stay distinct. Results are ordered by field, then message.
 *
 * @since 0.4.0
 * @author attrigo
 */
final class RequestValidationErrorAssembler {

    private static final Comparator<RequestValidationError> SORT_ORDER = Comparator.comparing(RequestValidationError::field)
                                                                                   .thenComparing(RequestValidationError::message);

    private RequestValidationErrorAssembler() {}

    /**
     * Builds a sorted list of validation errors from body field errors.
     * <p>
     * The full field path reported by {@link FieldError#getField()} is preserved (e.g. {@code data.nested.email}), so nested or duplicate leaf field names do
     * not collide.
     *
     * @param fieldErrors the list of {@link FieldError} from body validation
     * @return a sorted {@link List} of {@link RequestValidationError}
     */
    static List<RequestValidationError> fromFieldErrors(List<FieldError> fieldErrors) {
        Function<FieldError, RequestValidationError> fieldErrorMapper = fieldError -> RequestValidationError.of(fieldError.getField(),
                fieldError.getDefaultMessage());

        return fieldErrors.stream()
                          .map(fieldErrorMapper)
                          .sorted(SORT_ORDER)
                          .toList();
    }

    /**
     * Builds a sorted list of validation errors from method-parameter constraint violations.
     * <p>
     * The constraint-violation property path has the form {@code <method>.<field-path>}. The leading method-name segment is stripped while the rest of the path
     * is preserved (e.g. {@code search.filter.name} maps to {@code filter.name}), so nested or duplicate leaf field names do not collide.
     *
     * @param violations the set of {@link ConstraintViolation} from bean validation
     * @return a sorted {@link List} of {@link RequestValidationError}
     */
    static List<RequestValidationError> fromConstraintViolations(Set<ConstraintViolation<?>> violations) {
        return violations.stream()
                         .map(v -> {
                             var path = v.getPropertyPath()
                                         .toString();
                             var field = path.contains(".") ? path.substring(path.indexOf('.') + 1) : path;
                             return RequestValidationError.of(field, v.getMessage());
                         })
                         .sorted(SORT_ORDER)
                         .toList();
    }

}
