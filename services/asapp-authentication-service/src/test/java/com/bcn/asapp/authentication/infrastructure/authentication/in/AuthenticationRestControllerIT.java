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

package com.bcn.asapp.authentication.infrastructure.authentication.in;

import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REFRESH_TOKEN_FULL_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REVOKE_FULL_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_TOKEN_FULL_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.bcn.asapp.authentication.testutil.WebMvcTestContext;

class AuthenticationRestControllerIT extends WebMvcTestContext {

    @Nested
    class Authenticate {

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_RequestBodyNotJson() {
            // When & Then
            var requestBody = "";

            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                           .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Unsupported Media Type")
                                                  .containsEntry("status", 415)
                                                  .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                  .containsEntry("instance", "/api/auth/token");
                   });

        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMissingInRequest() {
            // When & Then
            var requestBody = "";

            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("detail", "Failed to read request")
                                                  .containsEntry("instance", "/api/auth/token");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // When & Then
            var requestBody = "{}";

            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("instance", "/api/auth/token");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The username must not be empty", "The password must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "authenticateRequest", "field", "username", "message", "The username must not be empty"),
                                                          Map.of("entity", "authenticateRequest", "field", "password", "message", "The password must not be empty")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyEmptyMandatoryFields() {
            // When & Then
            var requestBody = """
                    {
                    "username": "",
                    "password": ""
                    }
                    """;

            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("instance", "/api/auth/token");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The username must not be empty", "The password must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "authenticateRequest", "field", "username", "message", "The username must not be empty"),
                                                          Map.of("entity", "authenticateRequest", "field", "password", "message", "The password must not be empty")
                                                  );
                       //@formatter:on
                   });
        }

    }

    @Nested
    class RefreshAuthentication {

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_RequestBodyNotJson() {
            // When & Then
            var requestBody = "";

            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                                   .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Unsupported Media Type")
                                                  .containsEntry("status", 415)
                                                  .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                  .containsEntry("instance", "/api/auth/refresh");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMissingInRequest() {
            // When & Then
            var requestBody = "";

            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("detail", "Failed to read request")
                                                  .containsEntry("instance", "/api/auth/refresh");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // When & Then
            var requestBody = "{}";

            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("instance", "/api/auth/refresh");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The refresh token must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "refreshAuthenticationRequest", "field", "refreshToken", "message", "The refresh token must not be empty")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyEmptyMandatoryFields() {
            // When & Then
            var requestBody = """
                    {
                    "refresh_token": ""
                    }
                    """;

            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("instance", "/api/auth/refresh");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The refresh token must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "refreshAuthenticationRequest", "field", "refreshToken", "message", "The refresh token must not be empty")
                                                  );
                       //@formatter:on
                   });
        }

    }

    @Nested
    class RevokeAuthentication {

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_RequestBodyNotJson() {
            // When & Then
            var requestBody = "";

            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                            .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Unsupported Media Type")
                                                  .containsEntry("status", 415)
                                                  .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                  .containsEntry("instance", "/api/auth/revoke");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMissingInRequest() {
            // When & Then
            var requestBody = "";

            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                            .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("detail", "Failed to read request")
                                                  .containsEntry("instance", "/api/auth/revoke");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // When & Then
            var requestBody = "{}";

            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                            .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("instance", "/api/auth/revoke");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The access token must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "revokeAuthenticationRequest", "field", "accessToken", "message", "The access token must not be empty")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyEmptyMandatoryFields() {
            // When & Then
            var requestBody = """
                    {
                    "access_token": ""
                    }
                    """;

            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                            .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Bad Request")
                                                  .containsEntry("status", 400)
                                                  .containsEntry("instance", "/api/auth/revoke");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The access token must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "revokeAuthenticationRequest", "field", "accessToken", "message", "The access token must not be empty")
                                                  );
                       //@formatter:on
                   });
        }

    }

}
