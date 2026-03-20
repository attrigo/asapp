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

package com.bcn.asapp.tasks.infrastructure.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;

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

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Nested
    class HandleIllegalArgumentException {

        @Test
        void Returns400WithExceptionMessage_InvalidArgument() {
            // Given
            var exception = new IllegalArgumentException("Username must be a valid email address");

            // When
            var actual = globalExceptionHandler.handleIllegalArgumentException(exception);

            // Then
            assertThat(actual.getStatusCode()).as("status code")
                                              .isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(actual.getBody()).as("body")
                                        .isNotNull();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getBody().getTitle()).as("title").isEqualTo("Invalid Argument");
                softly.assertThat(actual.getBody().getStatus()).as("status").isEqualTo(400);
                softly.assertThat(actual.getBody().getDetail()).as("detail").isEqualTo("Username must be a valid email address");
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
            assertThat(actual.getStatusCode()).as("status code")
                                              .isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(actual.getBody()).as("body")
                                        .isNotNull();
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
            assertThat(actual.getStatusCode()).as("status code")
                                              .isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(actual.getBody()).as("body")
                                        .isNotNull();
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
            assertThat(actual.getStatusCode()).as("status code")
                                              .isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(actual.getBody()).as("body")
                                        .isNotNull();
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
            assertThat(actual.getStatusCode()).as("status code")
                                              .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(actual.getBody()).as("body")
                                        .isNotNull();
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
            assertThat(actual.getStatusCode()).as("status code")
                                              .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(actual.getBody()).as("body")
                                        .isNotNull();
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
