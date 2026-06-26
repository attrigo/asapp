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

package com.attrigo.asapp.authentication.infrastructure.authentication.in;

import static com.attrigo.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REFRESH_TOKEN_FULL_PATH;
import static com.attrigo.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REVOKE_FULL_PATH;
import static com.attrigo.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_TOKEN_FULL_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.attrigo.asapp.authentication.testutil.WebMvcTestContext;

/**
 * Tests {@link AuthenticationRestController} request validation and error responses.
 * <p>
 * Setup:
 * <li>Loads the web layer with a mock MVC environment and mocked service collaborators</li>
 * <p>
 * Coverage:
 * <li>Validates request content type (JSON required for POST operations)</li>
 * <li>Validates request body presence and structure</li>
 * <li>Validates mandatory field constraints (username, password, tokens)</li>
 * <li>Returns RFC 7807 Problem Details for all validation failures</li>
 * <li>Tests all HTTP endpoints (authenticate, refresh, revoke)</li>
 */
class AuthenticationRestControllerIT extends WebMvcTestContext {

    @Nested
    class Authenticate {

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_NonJsonRequestBody() {
            // Given
            var requestBody = "";
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                           .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Unsupported Media Type")
                                                                .containsEntry("status", 415)
                                                                .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                                .containsEntry("instance", "/api/auth/token"));

        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_MissingRequestBody() {
            // Given
            var requestBody = "";
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400)
                                                                .containsEntry("detail", "Failed to read request")
                                                                .containsEntry("instance", "/api/auth/token"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // Given
            var requestBody = "{}";
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("detail", "Request validation failed")
                                                 .containsEntry("error", "invalid_request")
                                                 .containsEntry("instance", "/api/auth/token");
                             assertThatJson(json).node("fieldErrors")
                                                 .isArray()
                                                 .hasSize(2);
                             assertThatJson(json).node("fieldErrors[0]")
                                                 .isObject()
                                                 .containsEntry("field", "password")
                                                 .containsEntry("message", "The password must not be empty");
                             assertThatJson(json).node("fieldErrors[1]")
                                                 .isObject()
                                                 .containsEntry("field", "username")
                                                 .containsEntry("message", "The username must not be empty");
                         });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyMandatoryFields() {
            // Given
            var requestBody = """
                    {
                    "username": "",
                    "password": ""
                    }
                    """;
            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("detail", "Request validation failed")
                                                 .containsEntry("error", "invalid_request")
                                                 .containsEntry("instance", "/api/auth/token");
                             assertThatJson(json).node("fieldErrors")
                                                 .isArray()
                                                 .hasSize(2);
                             assertThatJson(json).node("fieldErrors[0]")
                                                 .isObject()
                                                 .containsEntry("field", "password")
                                                 .containsEntry("message", "The password must not be empty");
                             assertThatJson(json).node("fieldErrors[1]")
                                                 .isObject()
                                                 .containsEntry("field", "username")
                                                 .containsEntry("message", "The username must not be empty");
                         });
        }

    }

    @Nested
    class RefreshAuthentication {

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_NonJsonRequestBody() {
            // Given
            var requestBody = "";
            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                                   .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Unsupported Media Type")
                                                                .containsEntry("status", 415)
                                                                .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                                .containsEntry("instance", "/api/auth/refresh"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_MissingRequestBody() {
            // Given
            var requestBody = "";
            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400)
                                                                .containsEntry("detail", "Failed to read request")
                                                                .containsEntry("instance", "/api/auth/refresh"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // Given
            var requestBody = "{}";
            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("detail", "Request validation failed")
                                                 .containsEntry("error", "invalid_request")
                                                 .containsEntry("instance", "/api/auth/refresh");
                             assertThatJson(json).node("fieldErrors")
                                                 .isArray()
                                                 .hasSize(1);
                             assertThatJson(json).node("fieldErrors[0]")
                                                 .isObject()
                                                 .containsEntry("field", "refreshToken")
                                                 .containsEntry("message", "The refresh token must not be empty");
                         });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyMandatoryFields() {
            // Given
            var requestBody = """
                    {
                    "refreshToken": ""
                    }
                    """;
            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("detail", "Request validation failed")
                                                 .containsEntry("error", "invalid_request")
                                                 .containsEntry("instance", "/api/auth/refresh");
                             assertThatJson(json).node("fieldErrors")
                                                 .isArray()
                                                 .hasSize(1);
                             assertThatJson(json).node("fieldErrors[0]")
                                                 .isObject()
                                                 .containsEntry("field", "refreshToken")
                                                 .containsEntry("message", "The refresh token must not be empty");
                         });
        }

    }

    @Nested
    class RevokeAuthentication {

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_NonJsonRequestBody() {
            // Given
            var requestBody = "";
            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                            .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Unsupported Media Type")
                                                                .containsEntry("status", 415)
                                                                .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                                .containsEntry("instance", "/api/auth/revoke"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_MissingRequestBody() {
            // Given
            var requestBody = "";
            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                            .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> assertThatJson(json).isObject()
                                                                .containsEntry("title", "Bad Request")
                                                                .containsEntry("status", 400)
                                                                .containsEntry("detail", "Failed to read request")
                                                                .containsEntry("instance", "/api/auth/revoke"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // Given
            var requestBody = "{}";
            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                            .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("detail", "Request validation failed")
                                                 .containsEntry("error", "invalid_request")
                                                 .containsEntry("instance", "/api/auth/revoke");
                             assertThatJson(json).node("fieldErrors")
                                                 .isArray()
                                                 .hasSize(1);
                             assertThatJson(json).node("fieldErrors[0]")
                                                 .isObject()
                                                 .containsEntry("field", "accessToken")
                                                 .containsEntry("message", "The access token must not be empty");
                         });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyMandatoryFields() {
            // Given
            var requestBody = """
                    {
                    "accessToken": ""
                    }
                    """;
            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                            .content(requestBody);

            // When & Then
            mockMvcTester.perform(requestBuilder)
                         .assertThat()
                         .hasStatus(HttpStatus.BAD_REQUEST)
                         .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                         .bodyJson()
                         .convertTo(String.class)
                         .satisfies(json -> {
                             assertThatJson(json).isObject()
                                                 .containsEntry("title", "Bad Request")
                                                 .containsEntry("status", 400)
                                                 .containsEntry("detail", "Request validation failed")
                                                 .containsEntry("error", "invalid_request")
                                                 .containsEntry("instance", "/api/auth/revoke");
                             assertThatJson(json).node("fieldErrors")
                                                 .isArray()
                                                 .hasSize(1);
                             assertThatJson(json).node("fieldErrors[0]")
                                                 .isObject()
                                                 .containsEntry("field", "accessToken")
                                                 .containsEntry("message", "The access token must not be empty");
                         });
        }

    }

}
