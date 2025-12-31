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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;

import com.bcn.asapp.tasks.infrastructure.security.AuthenticationNotFoundException;
import com.bcn.asapp.tasks.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.tasks.infrastructure.security.UnexpectedJwtTypeException;

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
            var response = globalExceptionHandler.handleIllegalArgumentException(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()
                               .getTitle()).isEqualTo("Invalid Argument");
            assertThat(response.getBody()
                               .getStatus()).isEqualTo(400);
            assertThat(response.getBody()
                               .getDetail()).isEqualTo("Username must be a valid email address");
        }

    }

    @Nested
    class HandleAuthenticationNotFoundException {

        @Test
        void Returns401WithGenericMessage_AuthenticationNotFound() {
            // Given
            var exception = new AuthenticationNotFoundException("Refresh token not found in active sessions");

            // When
            var response = globalExceptionHandler.handleAuthenticationNotFoundException(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()
                               .getTitle()).isEqualTo("Authentication Failed");
            assertThat(response.getBody()
                               .getStatus()).isEqualTo(401);
            assertThat(response.getBody()
                               .getDetail()).isEqualTo("Invalid credentials");
            assertThat(response.getBody()
                               .getProperties()).containsEntry("error", "invalid_grant");
        }

    }

    @Nested
    class HandleUnexpectedJwtTypeException {

        @Test
        void Returns401WithGenericMessage_UnexpectedJwtType() {
            // Given
            var exception = new UnexpectedJwtTypeException("Token is not a refresh token");

            // When
            var response = globalExceptionHandler.handleUnexpectedJwtTypeException(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()
                               .getTitle()).isEqualTo("Authentication Failed");
            assertThat(response.getBody()
                               .getStatus()).isEqualTo(401);
            assertThat(response.getBody()
                               .getDetail()).isEqualTo("Invalid token");
            assertThat(response.getBody()
                               .getProperties()).containsEntry("error", "invalid_grant");
        }

    }

    @Nested
    class HandleInvalidJwtException {

        @Test
        void Returns401WithGenericMessage_InvalidJwt() {
            // Given
            var exception = new InvalidJwtException("JWT signature validation failed", new RuntimeException("Signature error"));

            // When
            var response = globalExceptionHandler.handleInvalidJwtException(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()
                               .getTitle()).isEqualTo("Authentication Failed");
            assertThat(response.getBody()
                               .getStatus()).isEqualTo(401);
            assertThat(response.getBody()
                               .getDetail()).isEqualTo("Invalid credentials");
            assertThat(response.getBody()
                               .getProperties()).containsEntry("error", "invalid_grant");
        }

    }

    @Nested
    class HandleDataAccessException {

        @Test
        void Returns500WithGenericMessage_DatabaseOperationFails() {
            // Given
            var exception = new DataAccessException("Database connection failed") {};

            // When
            var response = globalExceptionHandler.handleDataAccessException(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()
                               .getTitle()).isEqualTo("Internal Server Error");
            assertThat(response.getBody()
                               .getStatus()).isEqualTo(500);
            assertThat(response.getBody()
                               .getDetail()).isEqualTo("An internal error occurred");
            assertThat(response.getBody()
                               .getProperties()).containsEntry("error", "server_error");
        }

    }

    @Nested
    class HandleRedisException {

        @Test
        void Returns503WithGenericMessage_RedisConnectionFails() {
            // Given
            var exception = new RedisConnectionFailureException("Cannot connect to Redis server", new RuntimeException("Connection refused"));

            // When
            var response = globalExceptionHandler.handleRedisException(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()
                               .getTitle()).isEqualTo("Service Unavailable");
            assertThat(response.getBody()
                               .getStatus()).isEqualTo(503);
            assertThat(response.getBody()
                               .getDetail()).isEqualTo("Service temporarily unavailable");
            assertThat(response.getBody()
                               .getProperties()).containsEntry("error", "temporarily_unavailable");
        }

    }

}
