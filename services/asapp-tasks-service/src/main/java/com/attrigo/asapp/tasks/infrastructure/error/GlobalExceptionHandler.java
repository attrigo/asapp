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

import static com.attrigo.asapp.tasks.infrastructure.error.ErrorMessages.*;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;

/**
 * Handles REST API exceptions and maps them to RFC 7807 {@link ProblemDetail} responses.
 * <p>
 * Error response strategy follows Spring Security best practices:
 * <ul>
 * <li><strong>Validation errors (400):</strong> Include detailed field errors (safe, aids client developers)</li>
 * <li><strong>Server errors (5xx):</strong> Generic messages only (avoid internal implementation disclosure)</li>
 * </ul>
 *
 * @since 0.2.0
 * @see ResponseEntityExceptionHandler
 * @author attrigo
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ============================================================================
    // 400 BAD REQUEST - Validation Errors
    // ============================================================================

    /**
     * Handles invalid request parameters.
     * <p>
     * Catches {@link ConstraintViolationException} when a query, path, or header parameter fails a validation rule.
     * <p>
     * Returns HTTP 400 Bad Request with a sorted list of field-level validation errors.
     *
     * @param ex the {@link ConstraintViolationException}
     * @return a {@link ResponseEntity} containing the error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex) {
        var invalidParameters = RequestValidationErrorAssembler.fromConstraintViolations(ex.getConstraintViolations());

        log.warn("Constraint violation: {}", ex.getMessage());
        log.atTrace()
           .log(() -> "Invalid parameters: " + invalidParameters);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_FAILED_DETAIL);
        problemDetail.setTitle(BAD_REQUEST_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, INVALID_REQUEST_ERROR);
        problemDetail.setProperty(FIELD_ERRORS_PROPERTY, invalidParameters);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(problemDetail);
    }

    /**
     * Handles method argument validation failures.
     * <p>
     * Catches {@link MethodArgumentNotValidException} when the request body contains one or more invalid fields.
     * <p>
     * Returns HTTP 400 Bad Request with a sorted list of field-level validation errors.
     *
     * @param ex      the {@link MethodArgumentNotValidException}
     * @param headers the HTTP headers
     * @param status  the HTTP status
     * @param request the web request
     * @return a {@link ResponseEntity} wrapping the {@link ProblemDetail} with validation errors
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {

        var invalidArguments = RequestValidationErrorAssembler.fromFieldErrors(ex.getBindingResult()
                                                                                 .getFieldErrors());

        log.warn("Argument not valid: {}", ex.getMessage());
        log.atTrace()
           .log(() -> "Invalid arguments: " + invalidArguments);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_FAILED_DETAIL);
        problemDetail.setTitle(BAD_REQUEST_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, INVALID_REQUEST_ERROR);
        problemDetail.setProperty(FIELD_ERRORS_PROPERTY, invalidArguments);

        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    /**
     * Handles illegal argument exceptions.
     * <p>
     * Catches {@link IllegalArgumentException} when a submitted value has an invalid format.
     * <p>
     * Returns HTTP 400 Bad Request with a fixed error message (never the raw exception message).
     *
     * @param ex the {@link IllegalArgumentException}
     * @return a {@link ResponseEntity} containing the error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_ARGUMENT_DETAIL);
        problemDetail.setTitle(INVALID_ARGUMENT_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, INVALID_REQUEST_ERROR);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(problemDetail);
    }

    // ============================================================================
    // 500 INTERNAL SERVER ERROR - Server/Infrastructure Failures
    // ============================================================================

    /**
     * Handles database access failures.
     * <p>
     * Catches {@link DataAccessException} when the database is unreachable or rejects an operation.
     * <p>
     * Returns HTTP 500 Internal Server Error with a generic message to avoid exposing database implementation details.
     *
     * @param ex the {@link DataAccessException}
     * @return a {@link ResponseEntity} with status 500 and generic error message
     */
    @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<ProblemDetail> handleDataAccessException(DataAccessException ex) {
        log.error("Database operation failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_DETAIL);
        problemDetail.setTitle(INTERNAL_SERVER_ERROR_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, SERVER_ERROR);
        problemDetail.setProperty(CRITICAL_PROPERTY, true);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(problemDetail);
    }

    // ============================================================================
    // 503 SERVICE UNAVAILABLE - External Service Failures
    // ============================================================================

    /**
     * Handles cache store failures.
     * <p>
     * Catches {@link RedisConnectionFailureException} when the cache store cannot be reached.
     * <p>
     * Returns HTTP 503 Service Unavailable with a generic message to avoid exposing Redis infrastructure details.
     *
     * @param ex the {@link RedisConnectionFailureException}
     * @return a {@link ResponseEntity} with status 503 and generic error message
     */
    @ExceptionHandler(RedisConnectionFailureException.class)
    protected ResponseEntity<ProblemDetail> handleRedisException(RedisConnectionFailureException ex) {
        log.error("Redis operation failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE_DETAIL);
        problemDetail.setTitle(SERVICE_UNAVAILABLE_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, TEMPORARILY_UNAVAILABLE_ERROR);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                             .body(problemDetail);
    }

    // ============================================================================
    // FALLBACK - Any Otherwise-Unhandled Exception (500 Internal Server Error)
    // ============================================================================

    /**
     * Handles any otherwise-unhandled exception as a last resort.
     * <p>
     * Catches {@link Exception} when no more specific handler applies, so an unexpected failure returns an RFC 7807 response instead of escaping as a raw
     * error.
     * <p>
     * Returns HTTP 500 Internal Server Error with a generic message and a critical flag for monitoring alerts.
     *
     * @param ex the unhandled {@link Exception}
     * @return a {@link ResponseEntity} with status 500 and generic error message
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ProblemDetail> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_DETAIL);
        problemDetail.setTitle(INTERNAL_SERVER_ERROR_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, SERVER_ERROR);
        problemDetail.setProperty(CRITICAL_PROPERTY, true);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(problemDetail);
    }

}
