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

import static com.attrigo.asapp.url.authentication.AuthenticationApiUrl.AUTH_REFRESH_TOKEN_FULL_PATH;
import static com.attrigo.asapp.url.authentication.AuthenticationApiUrl.AUTH_TOKEN_FULL_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.attrigo.asapp.authentication.application.CompensatingTransactionException;
import com.attrigo.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.attrigo.asapp.authentication.application.authentication.InvalidCredentialsException;
import com.attrigo.asapp.authentication.application.authentication.InvalidJwtException;
import com.attrigo.asapp.authentication.application.authentication.TokenStoreException;
import com.attrigo.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.attrigo.asapp.authentication.infrastructure.security.JwtIssuanceException;
import com.attrigo.asapp.authentication.testutil.WebMvcTestContext;
import com.attrigo.asapp.authentication.testutil.fixture.EncodedTokenMother;

/**
 * Tests {@link GlobalExceptionHandler} routing of exceptions escaping a use case through the MVC dispatch pipeline.
 * <p>
 * Setup:
 * <li>Loads the web layer with a mock MVC environment and mocked service collaborators</li>
 * <p>
 * Coverage:
 * <li>Routes invalid-argument failures escaping a use case to a 400 Problem Detail</li>
 * <li>Routes authentication failures escaping a use case to a 401 Problem Detail</li>
 * <li>Routes token-type and JWT validation failures escaping a use case to a 401 Problem Detail</li>
 * <li>Routes compensating-transaction, JWT signing and database failures escaping a use case to a 500 Problem Detail flagged critical</li>
 * <li>Routes token-store and cache connection failures escaping a use case to a 503 Problem Detail</li>
 * <li>Routes any otherwise-unhandled exception escaping a use case to a 500 Problem Detail instead of a raw Spring error</li>
 * <p>
 * Request-body validation (MethodArgumentNotValid) failures (400) surface before any use case runs, so they are covered by the controller integration tests
 * rather than here.
 */
class GlobalExceptionHandlerIT extends WebMvcTestContext {

    @Nested
    class HandleIllegalArgumentException {

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidArgument() {
            // Given
            var requestBody = """
                    {
                    "username": "user@asapp.com",
                    "password": "TEST@09_password?!"
                    }
                    """;
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            given(authenticateUseCase.authenticate(any())).willThrow(new IllegalArgumentException("Username must be a valid email address"));

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.BAD_REQUEST)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("error", "invalid_request")
                                                                     .containsEntry("detail", "Invalid argument provided"));
        }

    }

    @Nested
    class HandleInvalidCredentials {

        @Test
        void ReturnsStatusUnauthorizedAndBodyWithProblemDetail_InvalidCredentials() {
            // Given
            var requestBody = """
                    {
                    "username": "user@asapp.com",
                    "password": "TEST@09_password?!"
                    }
                    """;
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            given(authenticateUseCase.authenticate(any())).willThrow(
                    new InvalidCredentialsException("Invalid credentials", new RuntimeException("bad credentials")));

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.UNAUTHORIZED)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("error", "invalid_grant")
                                                                     .containsEntry("detail", "Invalid credentials"));
        }

    }

    @Nested
    class HandleAuthenticationNotFoundException {

        @Test
        void ReturnsStatusUnauthorizedAndBodyWithProblemDetail_AuthenticationNotFound() {
            // Given
            var requestBody = """
                    {
                    "refreshToken": "%s"
                    }
                    """.formatted(EncodedTokenMother.encodedRefreshToken());
            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(requestBody);

            given(refreshAuthenticationUseCase.refreshAuthentication(any())).willThrow(
                    new AuthenticationNotFoundException("Access token not found in active sessions"));

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.UNAUTHORIZED)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("error", "invalid_grant")
                                                                     .containsEntry("detail", "Invalid credentials"));
        }

    }

    @Nested
    class HandleUnexpectedJwtTypeException {

        @Test
        void ReturnsStatusUnauthorizedAndBodyWithProblemDetail_UnexpectedJwtType() {
            // Given
            var requestBody = """
                    {
                    "refreshToken": "%s"
                    }
                    """.formatted(EncodedTokenMother.encodedRefreshToken());
            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(requestBody);

            given(refreshAuthenticationUseCase.refreshAuthentication(any())).willThrow(new UnexpectedJwtTypeException("Token is not an access token"));

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.UNAUTHORIZED)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("error", "invalid_grant")
                                                                     .containsEntry("detail", "Invalid token"));
        }

    }

    @Nested
    class HandleInvalidJwtException {

        @Test
        void ReturnsStatusUnauthorizedAndBodyWithProblemDetail_InvalidJwt() {
            // Given
            var requestBody = """
                    {
                    "refreshToken": "%s"
                    }
                    """.formatted(EncodedTokenMother.encodedRefreshToken());
            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(requestBody);

            given(refreshAuthenticationUseCase.refreshAuthentication(any())).willThrow(
                    new InvalidJwtException("JWT signature validation failed", new RuntimeException("Signature error")));

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.UNAUTHORIZED)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("error", "invalid_grant")
                                                                     .containsEntry("detail", "Invalid credentials"));
        }

    }

    @Nested
    class HandleCompensatingTransactionException {

        @Test
        void ReturnsStatusInternalServerErrorAndBodyWithProblemDetail_CompensatingTransactionFails() {
            // Given
            var requestBody = """
                    {
                    "refreshToken": "%s"
                    }
                    """.formatted(EncodedTokenMother.encodedRefreshToken());
            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(requestBody);

            given(refreshAuthenticationUseCase.refreshAuthentication(any())).willThrow(new CompensatingTransactionException(
                    "Failed to compensate token rotation after token activation failure", new RuntimeException("Could not restore old tokens")));

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("error", "server_error")
                                                                     .containsEntry("critical", true));
        }

    }

    @Nested
    class HandleJwtIssuanceException {

        @Test
        void ReturnsStatusInternalServerErrorAndBodyWithProblemDetail_JwtIssuanceFails() {
            // Given
            var requestBody = """
                    {
                    "username": "user@asapp.com",
                    "password": "TEST@09_password?!"
                    }
                    """;
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            given(authenticateUseCase.authenticate(any())).willThrow(new JwtIssuanceException("JWT signing failed", new RuntimeException("Signing error")));

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("error", "server_error")
                                                                     .containsEntry("critical", true));
        }

    }

    @Nested
    class HandleDataAccessException {

        @Test
        void ReturnsStatusInternalServerErrorAndBodyWithProblemDetail_DatabaseOperationFails() {
            // Given
            var requestBody = """
                    {
                    "username": "user@asapp.com",
                    "password": "TEST@09_password?!"
                    }
                    """;
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            given(authenticateUseCase.authenticate(any())).willThrow(new DataAccessException("Database connection failed") {});

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("error", "server_error")
                                                                     .containsEntry("critical", true));
        }

    }

    @Nested
    class HandleTokenStoreException {

        @Test
        void ReturnsStatusServiceUnavailableAndBodyWithProblemDetail_TokenStoreFails() {
            // Given
            var requestBody = """
                    {
                    "username": "user@asapp.com",
                    "password": "TEST@09_password?!"
                    }
                    """;
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            given(authenticateUseCase.authenticate(any())).willThrow(
                    new TokenStoreException("Could not rotate tokens in fast-access store", new RuntimeException("Redis connection failed")));

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("error", "temporarily_unavailable")
                                                                     .containsEntry("detail", "Service temporarily unavailable"));
        }

    }

    @Nested
    class HandleRedisException {

        @Test
        void ReturnsStatusServiceUnavailableAndBodyWithProblemDetail_CacheConnectionFails() {
            // Given
            var requestBody = """
                    {
                    "username": "user@asapp.com",
                    "password": "TEST@09_password?!"
                    }
                    """;
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            given(authenticateUseCase.authenticate(any())).willThrow(
                    new RedisConnectionFailureException("Cannot connect to Redis server", new RuntimeException("Connection refused")));

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("error", "temporarily_unavailable")
                                                                     .containsEntry("detail", "Service temporarily unavailable"));
        }

    }

    @Nested
    class HandleUnexpectedException {

        @Test
        void ReturnsStatusInternalServerErrorAndBodyWithProblemDetail_UnexpectedError() {
            // Given
            var requestBody = """
                    {
                    "username": "user@asapp.com",
                    "password": "TEST@09_password?!"
                    }
                    """;
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            given(authenticateUseCase.authenticate(any())).willThrow(new RuntimeException("Simulated unexpected failure"));

            // When
            var actual = mockMvcTester.perform(requestBuilder);

            // Then
            assertThat(actual).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                              .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                              .bodyJson()
                              .convertTo(String.class)
                              .satisfies(json -> assertThatJson(json).isObject()
                                                                     .containsEntry("detail", "An internal error occurred")
                                                                     .containsEntry("critical", true));
        }

    }

}
