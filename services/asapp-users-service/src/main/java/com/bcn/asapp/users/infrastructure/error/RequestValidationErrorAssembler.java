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

package com.bcn.asapp.users.infrastructure.error;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.validation.FieldError;

import jakarta.validation.ConstraintViolation;

/**
 * Assembles sorted {@link RequestValidationError} lists from Spring and Jakarta validation failures.
 * <p>
 * Maps body field errors and method-parameter constraint violations to {@link RequestValidationError}. Results are ordered by field, then message.
 *
 * @since 0.4.0
 * @author attrigo
 */
final class RequestValidationErrorAssembler {

    private static final Comparator<RequestValidationError> SORT_ORDER = Comparator.comparing(RequestValidationError::field)
                                                                                   .thenComparing(RequestValidationError::message);

    private RequestValidationErrorAssembler() {}

    /**
     * Builds a sorted list of invalid parameters from body field errors.
     *
     * @param fieldErrors the list of {@link FieldError} from body validation
     * @return a sorted {@link List} of {@link RequestValidationError}
     */
    static List<RequestValidationError> fromFieldErrors(List<FieldError> fieldErrors) {
        Function<FieldError, RequestValidationError> fieldErrorMapper = fieldError -> new RequestValidationError(fieldError.getField(),
                fieldError.getDefaultMessage());

        return fieldErrors.stream()
                          .map(fieldErrorMapper)
                          .sorted(SORT_ORDER)
                          .toList();
    }

    /**
     * Builds a sorted list of invalid parameters from method-parameter constraint violations.
     *
     * @param violations the set of {@link ConstraintViolation} from bean validation
     * @return a sorted {@link List} of {@link RequestValidationError}
     */
    static List<RequestValidationError> fromConstraintViolations(Set<ConstraintViolation<?>> violations) {
        return violations.stream()
                         .map(v -> {
                             var path = v.getPropertyPath()
                                         .toString();
                             var field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                             return new RequestValidationError(field, v.getMessage());
                         })
                         .sorted(SORT_ORDER)
                         .toList();
    }

}
