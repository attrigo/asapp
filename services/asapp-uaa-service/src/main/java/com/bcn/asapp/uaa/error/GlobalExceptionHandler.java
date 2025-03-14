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
package com.bcn.asapp.uaa.error;

import java.util.List;
import java.util.function.Function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.bcn.asapp.dto.error.InvalidRequestParameterDTO;

/**
 * Provides a centralized way to handle the exceptions.
 * <p>
 * It is responsible for handling and customizing the response to various exceptions. It extends {@link ResponseEntityExceptionHandler} to handle common Spring
 * exceptions, such as validation errors, and return a consistent response format.
 *
 * @author ttrigo
 * @see MethodArgumentNotValidException
 * @since 0.2.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {

        var invalidParameters = buildInvalidParametersDTO(ex.getBindingResult()
                                                            .getFieldErrors());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("errors", invalidParameters);

        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    /**
     * Maps the invalid request fields ({@link FieldError}) to a more specific DTO {@link InvalidRequestParameterDTO}.
     *
     * @param fieldErrors the list of invalid fields to map.
     * @return a list of {@link InvalidRequestParameterDTO} that contains the invalid fields.
     */
    private List<InvalidRequestParameterDTO> buildInvalidParametersDTO(List<FieldError> fieldErrors) {
        Function<FieldError, InvalidRequestParameterDTO> fieldErrorMapper = fieldError -> new InvalidRequestParameterDTO(fieldError.getObjectName(),
                fieldError.getField(), fieldError.getDefaultMessage());

        return fieldErrors.stream()
                          .map(fieldErrorMapper)
                          .toList();
    }

}
