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
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

/**
 * Tests {@link GlobalExceptionHandler} exception-to-ProblemDetail translation and HTTP status mapping.
 * <p>
 * Coverage:
 * <li>Translates request validation failures to 400 Bad Request with sorted field errors</li>
 * <li>Translates invalid arguments to 400 Bad Request with a generic detail</li>
 * <li>Translates database failures to 500 Internal Server Error with generic messages</li>
 * <li>Translates unexpected exceptions to 500 Internal Server Error flagged critical</li>
 * <li>Translates cache connection failures to 503 Service Unavailable</li>
 * <li>All responses follow RFC 7807 Problem Details structure with error codes</li>
 */
class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Nested
    class HandleConstraintViolationException {

        @Test
        void ReturnsBadRequestAndProblemDetail_InvalidRequestParameter() {
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
                softly.assertThat(problemDetail.getProperties()).as("field errors").containsEntry("fieldErrors", sortedErrors);
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
        void ReturnsBadRequestAndProblemDetail_InvalidRequestBody() throws NoSuchMethodException {
            // Given
            var usernameEmptyFieldError = new FieldError("createTaskRequest", "username", "must not be blank");
            var passwordEmptyFieldError = new FieldError("createTaskRequest", "password", "must not be blank");
            var fieldErrors = List.of(usernameEmptyFieldError, passwordEmptyFieldError);
            var methodParameter = new MethodParameter(getClass().getDeclaredMethod("createTask", Object.class), 0);
            var bindingResult = mock(BindingResult.class);
            var exception = new MethodArgumentNotValidException(methodParameter, bindingResult);
            var sortedErrors = List.of(RequestValidationError.of("password", "must not be blank"), RequestValidationError.of("username", "must not be blank"));

            given(bindingResult.getFieldErrors()).willReturn(fieldErrors);

            // When
            var actual = globalExceptionHandler.handleMethodArgumentNotValid(exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            var problemDetail = (ProblemDetail) actual.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Bad Request");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(400);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("Request validation failed");
                softly.assertThat(problemDetail.getProperties()).as("error").containsEntry("error", "invalid_request");
                softly.assertThat(problemDetail.getProperties()).as("field errors").containsEntry("fieldErrors", sortedErrors);
                // @formatter:on
            });
        }

        @SuppressWarnings("unused")
        void createTask(Object body) {}

    }

    @Nested
    class HandleIllegalArgumentException {

        @Test
        void ReturnsBadRequestAndProblemDetail_InvalidArgument() {
            // Given
            var exception = new IllegalArgumentException("Username must be a valid email address");

            // When
            var actual = globalExceptionHandler.handleIllegalArgumentException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            var problemDetail = actual.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Invalid Argument");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(400);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("Invalid argument provided");
                softly.assertThat(problemDetail.getProperties()).as("error code").containsEntry("error", "invalid_request");
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleDataAccessException {

        @Test
        void ReturnsInternalServerErrorAndProblemDetail_DatabaseOperationFails() {
            // Given
            var exception = new DataAccessException("Database connection failed") {};

            // When
            var actual = globalExceptionHandler.handleDataAccessException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            var problemDetail = actual.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Internal Server Error");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(500);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("An internal error occurred");
                softly.assertThat(problemDetail.getProperties()).as("error code").containsEntry("error", "server_error");
                softly.assertThat(problemDetail.getProperties()).as("critical flag").containsEntry("critical", true);
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleRedisException {

        @Test
        void ReturnsServiceUnavailableAndProblemDetail_CacheConnectionFails() {
            // Given
            var exception = new RedisConnectionFailureException("Cannot connect to Redis server", new RuntimeException("Connection refused"));

            // When
            var actual = globalExceptionHandler.handleRedisException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            var problemDetail = actual.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Service Unavailable");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(503);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("Service temporarily unavailable");
                softly.assertThat(problemDetail.getProperties()).as("error code").containsEntry("error", "temporarily_unavailable");
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleUnexpectedException {

        @Test
        void ReturnsInternalServerErrorAndProblemDetail_UnexpectedError() {
            // Given
            var exception = new RuntimeException("Simulated unexpected failure");

            // When
            var actual = globalExceptionHandler.handleUnexpectedException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            var problemDetail = actual.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Internal Server Error");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(500);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("An internal error occurred");
                softly.assertThat(problemDetail.getProperties()).as("error code").containsEntry("error", "server_error");
                softly.assertThat(problemDetail.getProperties()).as("critical flag").containsEntry("critical", true);
                // @formatter:on
            });
        }

    }

}
