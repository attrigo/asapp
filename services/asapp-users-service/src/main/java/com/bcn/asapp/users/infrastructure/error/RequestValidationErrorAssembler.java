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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;

/**
 * Assembles sorted {@link RequestValidationError} lists from Spring and Jakarta validation failures.
 * <p>
 * Maps body field errors to {@link ParameterLocation#BODY} and resolves the request location of method-parameter constraint violations (path, query, header) by
 * inspecting the controller parameter annotations. Results are ordered by location, then field, then message.
 *
 * @since 0.4.0
 * @author attrigo
 */
final class RequestValidationErrorAssembler {

    private static final Comparator<RequestValidationError> SORT_ORDER = Comparator.comparing(RequestValidationError::location)
                                                                                   .thenComparing(RequestValidationError::field)
                                                                                   .thenComparing(RequestValidationError::message);

    private RequestValidationErrorAssembler() {}

    /**
     * Builds a sorted list of invalid parameters from body field errors.
     *
     * @param fieldErrors the list of {@link FieldError} from body validation
     * @return a sorted {@link List} of {@link RequestValidationError}
     */
    static List<RequestValidationError> fromFieldErrors(List<FieldError> fieldErrors) {
        Function<FieldError, RequestValidationError> fieldErrorMapper = fieldError -> new RequestValidationError(ParameterLocation.BODY, fieldError.getField(),
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
                             return new RequestValidationError(resolveLocation(v), field, v.getMessage());
                         })
                         .sorted(SORT_ORDER)
                         .toList();
    }

    private static ParameterLocation resolveLocation(ConstraintViolation<?> violation) {
        Class<?> rootClass = violation.getRootBeanClass();
        Iterator<Path.Node> nodes = violation.getPropertyPath()
                                             .iterator();

        if (!nodes.hasNext()) {
            return ParameterLocation.QUERY;
        }
        String methodName = nodes.next()
                                 .getName();

        if (!nodes.hasNext()) {
            return ParameterLocation.QUERY;
        }
        Path.Node paramNode = nodes.next();
        if (paramNode.getKind() != ElementKind.PARAMETER) {
            return ParameterLocation.QUERY;
        }
        Integer paramIndex = paramNode.as(Path.ParameterNode.class)
                                      .getParameterIndex();

        if (paramIndex == null) {
            return ParameterLocation.QUERY;
        }

        for (Method method : rootClass.getMethods()) {

            if (!method.getName()
                       .equals(methodName)
                    || paramIndex >= method.getParameterCount()) {
                continue;
            }

            Parameter parameter = method.getParameters()[paramIndex];
            if (parameter.isAnnotationPresent(PathVariable.class)) {
                return ParameterLocation.PATH;
            }
            if (parameter.isAnnotationPresent(RequestHeader.class)) {
                return ParameterLocation.HEADER;
            }
            if (parameter.isAnnotationPresent(RequestParam.class)) {
                return ParameterLocation.QUERY;
            }
            return ParameterLocation.QUERY;
        }

        return ParameterLocation.QUERY;
    }

}
