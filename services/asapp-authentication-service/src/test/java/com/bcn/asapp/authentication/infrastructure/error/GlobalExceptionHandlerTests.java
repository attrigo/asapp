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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;

import io.jsonwebtoken.JwtException;

import com.bcn.asapp.authentication.application.CompensatingTransactionException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtException;

class GlobalExceptionHandlerTests {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
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
            var exception = new InvalidJwtException("JWT signature validation failed");

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
    class HandleCompensatingTransactionException {

        @Test
        void Returns500WithCriticalFlag_CompensatingTransactionFails() {
            // Given
            var exception = new CompensatingTransactionException("Failed to compensate token rotation after token activation failure",
                    new RuntimeException("Could not restore old tokens"));

            // When
            var response = globalExceptionHandler.handleCompensatingTransactionException(exception);

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
            assertThat(response.getBody()
                               .getProperties()).containsEntry("critical", true);
        }

    }

    @Nested
    class HandleJwtException {

        @Test
        void Returns500WithGenericMessage_JwtOperationFails() {
            // Given
            var exception = new JwtException("JWT signing failed");

            // When
            var response = globalExceptionHandler.handleJwtException(exception);

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
    class HandleTokenStoreException {

        @Test
        void Returns503WithGenericMessage_TokenStoreFails() {
            // Given
            var exception = new TokenStoreException("Could not rotate tokens in fast-access store", new RuntimeException("Redis connection failed"));

            // When
            var response = globalExceptionHandler.handleTokenStoreException(exception);

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
