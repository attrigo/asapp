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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import com.bcn.asapp.users.infrastructure.security.AuthenticationNotFoundException;
import com.bcn.asapp.users.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.users.infrastructure.security.UnexpectedJwtTypeException;

/**
 * Tests {@link GlobalExceptionHandler} exception-to-ProblemDetail translation and HTTP status mapping.
 * <p>
 * Coverage:
 * <li>Translates request validation failures to 400 Bad Request with sorted field errors</li>
 * <li>Translates authentication failures to 401 Unauthorized with generic messages (security best practice)</li>
 * <li>Translates database failures to 500 Internal Server Error with generic messages</li>
 * <li>Translates cache connection failures to 503 Service Unavailable</li>
 * <li>All responses follow RFC 7807 Problem Details structure with error codes</li>
 */
class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Nested
    class HandleConstraintViolationException {

        @Test
        void Returns400AndProblemDetail_InvalidRequestParameter() {
            // Given
            var violations = Set.of(violation("searchById.term", "must not be blank"), violation("searchById.id", "must be a valid UUID"));
            var exception = new ConstraintViolationException(violations);
            var sortedErrors = List.of(RequestValidationError.of("id", "must be a valid UUID"), RequestValidationError.of("term", "must not be blank"));

            // When
            var actual = globalExceptionHandler.handleConstraintViolationException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            var problemDetail = actual.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Bad Request");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(400);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("Request validation failed");
                softly.assertThat(problemDetail.getProperties()).as("error").containsEntry("error", "invalid_request");
                softly.assertThat(problemDetail.getProperties()).as("field errors").containsEntry("field_errors", sortedErrors);
                // @formatter:on
            });
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

    @Nested
    class HandleMethodArgumentNotValid {

        @Test
        void Returns400WithGenericMessage_BodyValidationFails() {
            // Given
            var bindingResult = mock(BindingResult.class);
            given(bindingResult.getFieldErrors()).willReturn(List.of());
            var ex = new MethodArgumentNotValidException((MethodParameter) null, bindingResult);

            // When
            ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST,
                    mock(WebRequest.class));

            // Then
            var problemDetail = (ProblemDetail) response.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Bad Request");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(400);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("Request validation failed");
                softly.assertThat(problemDetail.getProperties()).as("error code").containsEntry("error", "invalid_request");
                // @formatter:on
            });
        }

        @Test
        void ReturnsSortedErrors_SameFieldMultipleViolations() {
            // Given
            FieldError empty = new FieldError("createUserRequest", "username", "must not be empty");
            FieldError tooShort = new FieldError("createUserRequest", "username", "size must be between 3 and 50");
            var bindingResult = mock(BindingResult.class);
            given(bindingResult.getFieldErrors()).willReturn(List.of(empty, tooShort));
            var ex = new MethodArgumentNotValidException((MethodParameter) null, bindingResult);
            var sortedErrors = List.of(RequestValidationError.of("username", "must not be empty"),
                    RequestValidationError.of("username", "size must be between 3 and 50"));

            // When
            var response = globalExceptionHandler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

            // Then
            var problemDetail = (ProblemDetail) response.getBody();
            @SuppressWarnings("unchecked")
            var errors = (List<RequestValidationError>) problemDetail.getProperties()
                                                                     .get("field_errors");
            assertThat(errors).containsExactlyElementsOf(sortedErrors);
        }

        @Test
        void ReturnsSortedErrors_MixedFieldsOutOfOrder() {
            // Given
            FieldError phoneNumberPattern = new FieldError("createUserRequest", "phoneNumber", "The phone number must be a valid phone number");
            FieldError emailEmpty = new FieldError("createUserRequest", "email", "The email must not be empty");
            FieldError firstNameEmpty = new FieldError("createUserRequest", "firstName", "The first name must not be empty");
            var bindingResult = mock(BindingResult.class);
            given(bindingResult.getFieldErrors()).willReturn(List.of(phoneNumberPattern, emailEmpty, firstNameEmpty));
            var ex = new MethodArgumentNotValidException((MethodParameter) null, bindingResult);
            var sortedErrors = List.of(RequestValidationError.of("email", "The email must not be empty"),
                    RequestValidationError.of("firstName", "The first name must not be empty"),
                    RequestValidationError.of("phoneNumber", "The phone number must be a valid phone number"));

            // When
            ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST,
                    mock(WebRequest.class));

            // Then
            var problemDetail = (ProblemDetail) response.getBody();
            @SuppressWarnings("unchecked")
            var errors = (List<RequestValidationError>) problemDetail.getProperties()
                                                                     .get("field_errors");
            assertThat(errors).containsExactlyElementsOf(sortedErrors);
        }

    }

    @Nested
    class HandleIllegalArgumentException {

        @Test
        void Returns400WithGenericMessage_InvalidArgument() {
            // Given
            var exception = new IllegalArgumentException("Email must be a valid email address");

            // When
            var actual = globalExceptionHandler.handleIllegalArgumentException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(actual.getBody()).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getBody().getTitle()).as("title").isEqualTo("Invalid Argument");
                softly.assertThat(actual.getBody().getStatus()).as("status").isEqualTo(400);
                softly.assertThat(actual.getBody().getDetail()).as("detail").isEqualTo("Invalid argument provided");
                softly.assertThat(actual.getBody().getProperties()).as("error code").containsEntry("error", "invalid_request");
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleAuthenticationNotFoundException {

        @Test
        void Returns401WithGenericMessage_AuthenticationNotFound() {
            // Given
            var exception = new AuthenticationNotFoundException("Access token not found in active sessions");

            // When
            var actual = globalExceptionHandler.handleAuthenticationNotFoundException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(actual.getBody()).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getBody().getTitle()).as("title").isEqualTo("Authentication Failed");
                softly.assertThat(actual.getBody().getStatus()).as("status").isEqualTo(401);
                softly.assertThat(actual.getBody().getDetail()).as("detail").isEqualTo("Invalid credentials");
                softly.assertThat(actual.getBody().getProperties()).as("error code").containsEntry("error", "invalid_grant");
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleUnexpectedJwtTypeException {

        @Test
        void Returns401WithGenericMessage_UnexpectedJwtType() {
            // Given
            var exception = new UnexpectedJwtTypeException("Token is not an access token");

            // When
            var actual = globalExceptionHandler.handleUnexpectedJwtTypeException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(actual.getBody()).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getBody().getTitle()).as("title").isEqualTo("Authentication Failed");
                softly.assertThat(actual.getBody().getStatus()).as("status").isEqualTo(401);
                softly.assertThat(actual.getBody().getDetail()).as("detail").isEqualTo("Invalid token");
                softly.assertThat(actual.getBody().getProperties()).as("error code").containsEntry("error", "invalid_grant");
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleInvalidJwtException {

        @Test
        void Returns401WithGenericMessage_InvalidJwt() {
            // Given
            var exception = new InvalidJwtException("JWT signature validation failed", new RuntimeException("Signature error"));

            // When
            var actual = globalExceptionHandler.handleInvalidJwtException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(actual.getBody()).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getBody().getTitle()).as("title").isEqualTo("Authentication Failed");
                softly.assertThat(actual.getBody().getStatus()).as("status").isEqualTo(401);
                softly.assertThat(actual.getBody().getDetail()).as("detail").isEqualTo("Invalid credentials");
                softly.assertThat(actual.getBody().getProperties()).as("error code").containsEntry("error", "invalid_grant");
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleDataAccessException {

        @Test
        void Returns500WithGenericMessage_DatabaseOperationFails() {
            // Given
            var exception = new DataAccessException("Database connection failed") {};

            // When
            var actual = globalExceptionHandler.handleDataAccessException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(actual.getBody()).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getBody().getTitle()).as("title").isEqualTo("Internal Server Error");
                softly.assertThat(actual.getBody().getStatus()).as("status").isEqualTo(500);
                softly.assertThat(actual.getBody().getDetail()).as("detail").isEqualTo("An internal error occurred");
                softly.assertThat(actual.getBody().getProperties()).as("error code").containsEntry("error", "server_error");
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleRedisException {

        @Test
        void Returns503WithGenericMessage_CacheConnectionFails() {
            // Given
            var exception = new RedisConnectionFailureException("Cannot connect to Redis server", new RuntimeException("Connection refused"));

            // When
            var actual = globalExceptionHandler.handleRedisException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(actual.getBody()).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getBody().getTitle()).as("title").isEqualTo("Service Unavailable");
                softly.assertThat(actual.getBody().getStatus()).as("status").isEqualTo(503);
                softly.assertThat(actual.getBody().getDetail()).as("detail").isEqualTo("Service temporarily unavailable");
                softly.assertThat(actual.getBody().getProperties()).as("error code").containsEntry("error", "temporarily_unavailable");
                // @formatter:on
            });
        }

    }

}
