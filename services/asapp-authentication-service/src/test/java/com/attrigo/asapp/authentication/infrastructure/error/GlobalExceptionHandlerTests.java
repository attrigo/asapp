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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
 * Tests {@link GlobalExceptionHandler} exception-to-ProblemDetail translation and HTTP status mapping.
 * <p>
 * Coverage:
 * <li>Translates request validation failures to 400 Bad Request with sorted field errors</li>
 * <li>Translates invalid arguments to 400 Bad Request with a generic detail</li>
 * <li>Translates authentication failures to 401 Unauthorized with generic messages (security best practice)</li>
 * <li>Translates compensating-transaction failures to 500 Internal Server Error flagged critical</li>
 * <li>Translates JWT signing failures to 500 Internal Server Error with generic messages</li>
 * <li>Translates database failures to 500 Internal Server Error with generic messages</li>
 * <li>Translates token-store and cache connection failures to 503 Service Unavailable</li>
 * <li>All responses follow RFC 7807 Problem Details structure with error codes</li>
 */
class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Nested
    class HandleMethodArgumentNotValid {

        @Test
        void ReturnsBadRequestAndProblemDetail_InvalidRequestBody() throws NoSuchMethodException {
            // Given
            var usernameEmptyFieldError = new FieldError("authenticateRequest", "username", "must not be blank");
            var passwordEmptyFieldError = new FieldError("authenticateRequest", "password", "must not be blank");
            var fieldErrors = List.of(usernameEmptyFieldError, passwordEmptyFieldError);
            var methodParameter = new MethodParameter(getClass().getDeclaredMethod("authenticate", Object.class), 0);
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
        void authenticate(Object body) {}

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
    class HandleInvalidCredentials {

        @ParameterizedTest
        @MethodSource("invalidCredentialExceptions")
        void ReturnsUnauthorizedAndProblemDetail_InvalidCredentials(RuntimeException exception) {
            // When
            var actual = globalExceptionHandler.handleInvalidCredentials(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            var problemDetail = actual.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Authentication Failed");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(401);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("Invalid credentials");
                softly.assertThat(problemDetail.getProperties()).as("error code").containsEntry("error", "invalid_grant");
                // @formatter:on
            });
        }

        private static Stream<RuntimeException> invalidCredentialExceptions() {
            return Stream.of(new InvalidCredentialsException("Invalid credentials", new RuntimeException("bad credentials")),
                    new InvalidUsernameException("Username must be a valid email address"),
                    new InvalidPasswordException("Raw password must be between 8 and 64 characters"),
                    new InvalidEncodedTokenException("Encoded token must be a valid JWT format"));
        }

    }

    @Nested
    class HandleAuthenticationNotFoundException {

        @Test
        void ReturnsUnauthorizedAndProblemDetail_AuthenticationNotFound() {
            // Given
            var exception = new AuthenticationNotFoundException("Access token not found in active sessions");

            // When
            var actual = globalExceptionHandler.handleAuthenticationNotFoundException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            var problemDetail = actual.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Authentication Failed");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(401);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("Invalid credentials");
                softly.assertThat(problemDetail.getProperties()).as("error code").containsEntry("error", "invalid_grant");
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleUnexpectedJwtTypeException {

        @Test
        void ReturnsUnauthorizedAndProblemDetail_UnexpectedJwtType() {
            // Given
            var exception = new UnexpectedJwtTypeException("Token is not an access token");

            // When
            var actual = globalExceptionHandler.handleUnexpectedJwtTypeException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            var problemDetail = actual.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Authentication Failed");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(401);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("Invalid token");
                softly.assertThat(problemDetail.getProperties()).as("error code").containsEntry("error", "invalid_grant");
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleInvalidJwtException {

        @Test
        void ReturnsUnauthorizedAndProblemDetail_InvalidJwt() {
            // Given
            var exception = new InvalidJwtException("JWT signature validation failed", new RuntimeException("Signature error"));

            // When
            var actual = globalExceptionHandler.handleInvalidJwtException(exception);

            // Then
            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            var problemDetail = actual.getBody();
            assertThat(problemDetail).isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(problemDetail.getTitle()).as("title").isEqualTo("Authentication Failed");
                softly.assertThat(problemDetail.getStatus()).as("status").isEqualTo(401);
                softly.assertThat(problemDetail.getDetail()).as("detail").isEqualTo("Invalid credentials");
                softly.assertThat(problemDetail.getProperties()).as("error code").containsEntry("error", "invalid_grant");
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleCompensatingTransactionException {

        @Test
        void ReturnsInternalServerErrorAndProblemDetail_CompensatingTransactionFails() {
            // Given
            var exception = new CompensatingTransactionException("Failed to compensate token rotation after token activation failure",
                    new RuntimeException("Could not restore old tokens"));

            // When
            var actual = globalExceptionHandler.handleCompensatingTransactionException(exception);

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
    class HandleJwtIssuanceException {

        @Test
        void ReturnsInternalServerErrorAndProblemDetail_JwtIssuanceFails() {
            // Given
            var exception = new JwtIssuanceException("JWT signing failed", new RuntimeException("Signing error"));

            // When
            var actual = globalExceptionHandler.handleJwtIssuanceException(exception);

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
                // @formatter:on
            });
        }

    }

    @Nested
    class HandleTokenStoreException {

        @Test
        void ReturnsServiceUnavailableAndProblemDetail_TokenStoreFails() {
            // Given
            var exception = new TokenStoreException("Could not rotate tokens in fast-access store", new RuntimeException("Redis connection failed"));

            // When
            var actual = globalExceptionHandler.handleTokenStoreException(exception);

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

}
