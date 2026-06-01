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

package com.bcn.asapp.tasks.infrastructure.error;

import static com.bcn.asapp.tasks.infrastructure.error.ErrorMessages.INVALID_ARGUMENT_DETAIL;
import static com.bcn.asapp.tasks.infrastructure.error.ErrorMessages.VALIDATION_FAILED_DETAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.executable.ExecutableValidator;

import com.bcn.asapp.tasks.infrastructure.security.AuthenticationNotFoundException;
import com.bcn.asapp.tasks.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.tasks.infrastructure.security.UnexpectedJwtTypeException;

/**
 * Tests {@link GlobalExceptionHandler} exception-to-ProblemDetail translation and HTTP status mapping.
 * <p>
 * Coverage:
 * <li>Translates domain validation failures to 400 Bad Request with specific messages</li>
 * <li>Translates authentication failures to 401 Unauthorized with generic messages (security best practice)</li>
 * <li>Translates database failures to 500 Internal Server Error with generic messages</li>
 * <li>Translates cache connection failures to 503 Service Unavailable</li>
 * <li>All responses follow RFC 7807 Problem Details structure with error codes</li>
 */
class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Nested
    class HandleConstraintViolationException {

        private ExecutableValidator executableValidator;

        private FakeController fakeController;

        @BeforeEach
        void setUp() {
            var validatorFactory = Validation.buildDefaultValidatorFactory();
            executableValidator = validatorFactory.getValidator()
                                                  .forExecutables();
            fakeController = new FakeController();
        }

        @Test
        void handleConstraintViolationException_setsFixedDetail() {
            // Given
            var ex = new ConstraintViolationException("ignored message", Set.of());

            // When
            ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleConstraintViolationException(ex);

            // Then
            assertThat(response.getBody()
                               .getDetail()).isEqualTo(VALIDATION_FAILED_DETAIL);
        }

        @Test
        void handleConstraintViolationException_whenMultipleViolations_sortsByFieldThenMessage() throws Exception {
            // Given
            var method = FakeController.class.getMethod("searchById", String.class, String.class);
            var violations = executableValidator.validateParameters(fakeController, method, new Object[] { null, null });
            var ex = new ConstraintViolationException(violations);

            // When
            ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleConstraintViolationException(ex);

            // Then
            var problemDetail = response.getBody();
            @SuppressWarnings("unchecked")
            var errors = (List<RequestValidationError>) problemDetail.getProperties()
                                                                     .get("field_errors");
            assertThat(errors).hasSize(2)
                              .containsExactly(RequestValidationError.of("id", "must not be null"), RequestValidationError.of("term", "must not be null"));
        }

        static class FakeController {

            public void searchById(@PathVariable @NotNull String id, @RequestParam @NotNull String term) {}

        }

    }

    @Nested
    class HandleMethodArgumentNotValid {

        @Test
        void handleMethodArgumentNotValid_setsFixedDetail() {
            // Given
            var bindingResult = mock(BindingResult.class);
            given(bindingResult.getFieldErrors()).willReturn(List.of());
            var ex = new MethodArgumentNotValidException((MethodParameter) null, bindingResult);

            // When
            ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST,
                    mock(WebRequest.class));

            // Then
            var problemDetail = (ProblemDetail) response.getBody();
            assertThat(problemDetail.getDetail()).isEqualTo(VALIDATION_FAILED_DETAIL);
        }

        @Test
        void handleMethodArgumentNotValid_whenFieldHasMultipleViolations_returnsAllViolations() {
            // Given
            FieldError empty = new FieldError("createTaskRequest", "title", "must not be empty");
            FieldError tooShort = new FieldError("createTaskRequest", "title", "size must be between 3 and 50");
            var bindingResult = mock(BindingResult.class);
            given(bindingResult.getFieldErrors()).willReturn(List.of(empty, tooShort));
            var ex = new MethodArgumentNotValidException((MethodParameter) null, bindingResult);

            // When
            var response = globalExceptionHandler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

            // Then
            var problemDetail = (ProblemDetail) response.getBody();
            @SuppressWarnings("unchecked")
            var errors = (List<RequestValidationError>) problemDetail.getProperties()
                                                                     .get("field_errors");
            assertThat(errors).containsExactly(RequestValidationError.of("title", "must not be empty"),
                    RequestValidationError.of("title", "size must be between 3 and 50"));
        }

        @Test
        void handleMethodArgumentNotValid_whenMixedFieldsOutOfOrder_sortsErrors() {
            // Given
            FieldError usernameSize = new FieldError("createTaskRequest", "username", "size must be between 3 and 30");
            FieldError usernameEmpty = new FieldError("createTaskRequest", "username", "must not be empty");
            FieldError passwordEmpty = new FieldError("createTaskRequest", "password", "must not be empty");
            var bindingResult = mock(BindingResult.class);
            given(bindingResult.getFieldErrors()).willReturn(List.of(usernameSize, usernameEmpty, passwordEmpty));
            var ex = new MethodArgumentNotValidException((MethodParameter) null, bindingResult);

            // When
            ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST,
                    mock(WebRequest.class));

            // Then
            var problemDetail = (ProblemDetail) response.getBody();
            @SuppressWarnings("unchecked")
            var errors = (List<RequestValidationError>) problemDetail.getProperties()
                                                                     .get("field_errors");
            assertThat(errors).containsExactly(RequestValidationError.of("password", "must not be empty"),
                    RequestValidationError.of("username", "must not be empty"), RequestValidationError.of("username", "size must be between 3 and 30"));
        }

    }

    @Nested
    class HandleIllegalArgumentException {

        @Test
        void handleIllegalArgumentException_setsFixedDetail() {
            // Given
            var ex = new IllegalArgumentException("any dynamic message that must NOT appear in response");

            // When
            ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleIllegalArgumentException(ex);

            // Then
            assertThat(response.getBody()
                               .getDetail()).isEqualTo(INVALID_ARGUMENT_DETAIL);
        }

        @Test
        void Returns400WithGenericMessage_InvalidArgument() {
            // Given
            var exception = new IllegalArgumentException("Username must be a valid email address");

            // When
            var actual = globalExceptionHandler.handleIllegalArgumentException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(actual.getBody()).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getBody().getTitle()).as("title").isEqualTo("Invalid Argument");
                softly.assertThat(actual.getBody().getStatus()).as("status").isEqualTo(400);
                softly.assertThat(actual.getBody().getDetail()).as("detail").isEqualTo(INVALID_ARGUMENT_DETAIL);
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
