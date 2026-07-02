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

package com.attrigo.asapp.authentication.infrastructure.error;

import static com.attrigo.asapp.authentication.infrastructure.error.ErrorMessages.*;

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

import com.attrigo.asapp.authentication.application.CompensatingTransactionException;
import com.attrigo.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.attrigo.asapp.authentication.application.authentication.InvalidCredentialsException;
import com.attrigo.asapp.authentication.application.authentication.InvalidJwtException;
import com.attrigo.asapp.authentication.application.authentication.TokenStoreException;
import com.attrigo.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.attrigo.asapp.authentication.domain.authentication.InvalidEncodedTokenException;
import com.attrigo.asapp.authentication.domain.user.InvalidPasswordException;
import com.attrigo.asapp.authentication.domain.user.InvalidUsernameException;
import com.attrigo.asapp.authentication.infrastructure.security.JwtIssuanceException;

/**
 * Handles REST API exceptions and maps them to RFC 7807 {@link ProblemDetail} responses.
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
 * @author attrigo
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ============================================================================
    // 400 BAD REQUEST - Validation Errors
    // ============================================================================

    /**
     * Handles method argument validation failures.
     * <p>
     * Thrown by Spring when request body fields fail Jakarta Bean Validation constraints.
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
     * Thrown by domain value objects when constructor or factory method arguments violate format constraints.
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
    // 401 UNAUTHORIZED - Authentication Failures
    // ============================================================================

    /**
     * Handles invalid-credential exceptions (bad format or credential mismatch).
     * <p>
     * Captured for:
     * <ul>
     * <li>{@link InvalidCredentialsException}: credentials rejected during authentication</li>
     * <li>{@link InvalidUsernameException}: username is not a valid email format</li>
     * <li>{@link InvalidPasswordException}: password length is outside the allowed range</li>
     * <li>{@link InvalidEncodedTokenException}: token is not a valid JWT format</li>
     * </ul>
     * All are treated as an authentication failure rather than a validation error to prevent user enumeration attacks.
     * <p>
     * Returns HTTP 401 Unauthorized with a generic error message.
     *
     * @param ex the captured credential exception
     * @return a {@link ResponseEntity} with status 401 and generic error message
     */
    @ExceptionHandler({ InvalidCredentialsException.class, InvalidUsernameException.class, InvalidPasswordException.class, InvalidEncodedTokenException.class })
    protected ResponseEntity<ProblemDetail> handleInvalidCredentials(RuntimeException ex) {
        log.warn("Authentication failed due to invalid credentials: {}", ex.getMessage());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_DETAIL);
        problemDetail.setTitle(AUTHENTICATION_FAILED_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, INVALID_GRANT_ERROR);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(problemDetail);
    }

    /**
     * Handles authentication not found exceptions.
     * <p>
     * Thrown when authentication sessions are not found (token revoked, session expired).
     * <p>
     * Returns HTTP 401 Unauthorized with a generic message to prevent user enumeration attacks.
     *
     * @param ex the {@link AuthenticationNotFoundException}
     * @return a {@link ResponseEntity} with status 401 and generic error message
     */
    @ExceptionHandler(AuthenticationNotFoundException.class)
    protected ResponseEntity<ProblemDetail> handleAuthenticationNotFoundException(AuthenticationNotFoundException ex) {
        log.warn("Authentication not found: {}", ex.getMessage());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_DETAIL);
        problemDetail.setTitle(AUTHENTICATION_FAILED_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, INVALID_GRANT_ERROR);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(problemDetail);
    }

    /**
     * Handles unexpected JWT type exceptions.
     * <p>
     * Thrown when token type doesn't match expected type (e.g., access token provided when refresh token expected).
     * <p>
     * Returns HTTP 401 Unauthorized with a generic message to avoid revealing token validation logic.
     *
     * @param ex the {@link UnexpectedJwtTypeException}
     * @return a {@link ResponseEntity} with status 401 and generic error message
     */
    @ExceptionHandler(UnexpectedJwtTypeException.class)
    protected ResponseEntity<ProblemDetail> handleUnexpectedJwtTypeException(UnexpectedJwtTypeException ex) {
        log.warn("Invalid token type: {}", ex.getMessage());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_DETAIL);
        problemDetail.setTitle(AUTHENTICATION_FAILED_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, INVALID_GRANT_ERROR);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(problemDetail);
    }

    /**
     * Handles invalid JWT exceptions.
     * <p>
     * Thrown when tokens are invalid, malformed, expired, or fail cryptographic verification.
     * <p>
     * Returns HTTP 401 Unauthorized with a generic message to prevent revealing token validation details.
     *
     * @param ex the {@link InvalidJwtException}
     * @return a {@link ResponseEntity} with status 401 and generic error message
     */
    @ExceptionHandler(InvalidJwtException.class)
    protected ResponseEntity<ProblemDetail> handleInvalidJwtException(InvalidJwtException ex) {
        log.warn("Invalid JWT: {}", ex.getMessage());

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_DETAIL);
        problemDetail.setTitle(AUTHENTICATION_FAILED_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, INVALID_GRANT_ERROR);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(problemDetail);
    }

    // ============================================================================
    // 500 INTERNAL SERVER ERROR - Server/Infrastructure Failures
    // ============================================================================

    /**
     * Handles compensating transaction failures across the application.
     * <p>
     * This is a critical error indicating the system could not automatically recover from a failure, potentially leaving data in an inconsistent state.
     * <p>
     * Returns HTTP 500 Internal Server Error with a generic message and critical flag for monitoring alerts.
     *
     * @param ex the {@link CompensatingTransactionException}
     * @return a {@link ResponseEntity} with status 500 and generic error message
     */
    @ExceptionHandler(CompensatingTransactionException.class)
    protected ResponseEntity<ProblemDetail> handleCompensatingTransactionException(CompensatingTransactionException ex) {
        log.error("CRITICAL: Compensating transaction failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_DETAIL);
        problemDetail.setTitle(INTERNAL_SERVER_ERROR_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, SERVER_ERROR);
        problemDetail.setProperty(CRITICAL_PROPERTY, true);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(problemDetail);
    }

    /**
     * Handles JWT signing and issuance failures.
     * <p>
     * Thrown when JWT generation or cryptographic operations fail.
     * <p>
     * Returns HTTP 500 Internal Server Error with a generic message to avoid exposing cryptographic implementation details.
     *
     * @param ex the {@link JwtIssuanceException}
     * @return a {@link ResponseEntity} with status 500 and generic error message
     */
    @ExceptionHandler(JwtIssuanceException.class)
    protected ResponseEntity<ProblemDetail> handleJwtIssuanceException(JwtIssuanceException ex) {
        log.error("JWT operation failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_DETAIL);
        problemDetail.setTitle(INTERNAL_SERVER_ERROR_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, SERVER_ERROR);
        problemDetail.setProperty(CRITICAL_PROPERTY, true);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(problemDetail);
    }

    /**
     * Handles Spring Data JDBC failures during database operations.
     * <p>
     * Thrown when database operations fail (connection issues, constraint violations, etc.).
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
     * Handles JWT store operation failures in authentication operations.
     * <p>
     * Thrown when Redis token store operations fail (save, delete, check existence).
     * <p>
     * Returns HTTP 503 Service Unavailable with a generic message to avoid exposing Redis infrastructure details.
     *
     * @param ex the {@link TokenStoreException}
     * @return a {@link ResponseEntity} with status 503 and generic error message
     */
    @ExceptionHandler(TokenStoreException.class)
    protected ResponseEntity<ProblemDetail> handleTokenStoreException(TokenStoreException ex) {
        log.error("Token store operation failed: {}", ex.getMessage(), ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE_DETAIL);
        problemDetail.setTitle(SERVICE_UNAVAILABLE_TITLE);
        problemDetail.setProperty(ERROR_PROPERTY, TEMPORARILY_UNAVAILABLE_ERROR);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                             .body(problemDetail);
    }

    /**
     * Handles Redis connection and operation failures.
     * <p>
     * Thrown when Redis connection or low-level operations fail.
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
     * Ensures an unexpected failure returns an RFC 7807 response instead of escaping as a raw Spring error. Being the broadest match, it fires only when no
     * more specific handler applies.
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
