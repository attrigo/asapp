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

package com.bcn.asapp.authentication.infrastructure.error;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.bcn.asapp.authentication.application.CompensatingTransactionException;
import com.bcn.asapp.authentication.application.PersistenceException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.TokenGenerationException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;

/**
 * Global exception handler for REST API endpoints.
 * <p>
 * Extends {@link ResponseEntityExceptionHandler} to provide centralized exception handling and consistent error responses across the application.
 * <p>
 * Error response strategy follows RFC 6749 (OAuth2 Token Endpoint) and Spring Security best practices:
 * <ul>
 * <li><strong>Validation errors (400):</strong> Include detailed field errors (safe, aids client developers)</li>
 * <li><strong>Authentication errors (401):</strong> Generic messages only (prevent user enumeration attacks)</li>
 * <li><strong>Server errors (5xx):</strong> Generic messages only (avoid internal implementation disclosure)</li>
 * </ul>
 *
 * @since 0.2.0
 * @see ResponseEntityExceptionHandler
 * @see MethodArgumentNotValidException
 * @author attrigo
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles method argument validation failures.
     * <p>
     * Converts validation errors into a structured {@link ProblemDetail} response containing details about each invalid parameter.
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

        logger.debug("Validation failed: {} field errors", ex.getBindingResult()
                                                             .getFieldErrorCount());

        var invalidParameters = buildInvalidParameters(ex.getBindingResult()
                                                         .getFieldErrors());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("errors", invalidParameters);

        return handleExceptionInternal(ex, problemDetail, headers, status, request);
    }

    /**
     * Handles authentication not found exceptions.
     * <p>
     * Returns HTTP 401 Unauthorized with a generic message to prevent user enumeration attacks.
     * <p>
     * Actual error details are logged server-side for debugging.
     *
     * @param ex the {@link AuthenticationNotFoundException}
     * @return a {@link ResponseEntity} with status 401 and generic error message
     */
    @ExceptionHandler(AuthenticationNotFoundException.class)
    protected ResponseEntity<ProblemDetail> handleAuthenticationNotFoundException(AuthenticationNotFoundException ex) {
        logger.warn("Authentication not found: {}", ex.getMessage());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setProperty("error", "invalid_grant");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(problemDetail);
    }

    /**
     * Handles unexpected JWT type exceptions.
     * <p>
     * Returns HTTP 401 Unauthorized with a generic message to avoid revealing token validation logic.
     * <p>
     * Actual error details are logged server-side for debugging.
     *
     * @param ex the {@link UnexpectedJwtTypeException}
     * @return a {@link ResponseEntity} with status 401 and generic error message
     */
    @ExceptionHandler(UnexpectedJwtTypeException.class)
    protected ResponseEntity<ProblemDetail> handleUnexpectedJwtTypeException(UnexpectedJwtTypeException ex) {
        logger.warn("Invalid token type: {}", ex.getMessage());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid token");
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setProperty("error", "invalid_grant");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(problemDetail);
    }

    /**
     * Handles JWT generation failures in authentication operations.
     * <p>
     * Returns HTTP 500 Internal Server Error with a generic message to avoid exposing cryptographic implementation details.
     * <p>
     * Actual error details are logged server-side for debugging.
     *
     * @param ex the {@link TokenGenerationException}
     * @return a {@link ResponseEntity} with status 500 and generic error message
     */
    @ExceptionHandler(TokenGenerationException.class)
    protected ResponseEntity<ProblemDetail> handleTokenGenerationException(TokenGenerationException ex) {
        logger.error("Token generation failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("error", "server_error");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(problemDetail);
    }

    /**
     * Handles JWT store operation failures in authentication operations.
     * <p>
     * Returns HTTP 503 Service Unavailable with a generic message to avoid exposing Redis infrastructure details.
     * <p>
     * Actual error details are logged server-side for debugging.
     *
     * @param ex the {@link TokenStoreException}
     * @return a {@link ResponseEntity} with status 503 and generic error message
     */
    @ExceptionHandler(TokenStoreException.class)
    protected ResponseEntity<ProblemDetail> handleTokenStoreException(TokenStoreException ex) {
        logger.error("Token store operation failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setProperty("error", "temporarily_unavailable");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                             .body(problemDetail);
    }

    /**
     * Handles repository persistence failures across the application.
     * <p>
     * Returns HTTP 500 Internal Server Error with a generic message to avoid exposing database implementation details.
     * <p>
     * Actual error details are logged server-side for debugging.
     *
     * @param ex the {@link PersistenceException} (or any subclass)
     * @return a {@link ResponseEntity} with status 500 and generic error message
     */
    @ExceptionHandler(PersistenceException.class)
    protected ResponseEntity<ProblemDetail> handlePersistenceException(PersistenceException ex) {
        logger.error("Persistence operation failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("error", "server_error");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(problemDetail);
    }

    /**
     * Handles compensating transaction failures across the application.
     * <p>
     * Returns HTTP 500 Internal Server Error with a generic message. This is a critical error that requires manual intervention as it indicates the system
     * could not automatically recover from a failure, potentially leaving data in an inconsistent state. <p< Actual error details are logged server-side for
     * debugging.
     *
     * @param ex the {@link CompensatingTransactionException}
     * @return a {@link ResponseEntity} with status 500 and generic error message
     */
    @ExceptionHandler(CompensatingTransactionException.class)
    protected ResponseEntity<ProblemDetail> handleCompensatingTransactionException(CompensatingTransactionException ex) {
        logger.error("CRITICAL: Compensating transaction failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("error", "server_error");
        problemDetail.setProperty("critical", true);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(problemDetail);
    }

    /**
     * Builds a list of invalid parameter details from field errors.
     *
     * @param fieldErrors the list of {@link FieldError} from validation
     * @return a {@link List} of {@link InvalidRequestParameter} containing error details
     */
    private List<InvalidRequestParameter> buildInvalidParameters(List<FieldError> fieldErrors) {
        Function<FieldError, InvalidRequestParameter> fieldErrorMapper = fieldError -> new InvalidRequestParameter(fieldError.getObjectName(),
                fieldError.getField(), fieldError.getDefaultMessage());

        return fieldErrors.stream()
                          .map(fieldErrorMapper)
                          .toList();
    }

}
