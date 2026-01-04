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

package com.bcn.asapp.authentication.infrastructure.user.in;

import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_ROOT_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import com.bcn.asapp.authentication.testutil.WebMvcTestContext;

@WithMockUser
class UserRestControllerIT extends WebMvcTestContext {

    @Nested
    class GetUserById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_UserIdMissingInPath() {
            // When & Then
            var requestBuilder = get(USERS_ROOT_PATH + "/");
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.NOT_FOUND)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Not Found")
                                                  .containsEntry("status", 404)
                                                  .containsEntry("detail", "No static resource api/users.")
                                                  .containsEntry("instance", "/api/users/");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidUserIdPath() {
            // When & Then
            var userIdPath = 1L;

            var requestBuilder = get(USERS_GET_BY_ID_FULL_PATH, userIdPath);
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
                                                  .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                  .containsEntry("instance", "/api/users/1");
                   });
        }

    }

    @WithAnonymousUser
    @Nested
    class CreateUser {

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_RequestBodyNotJson() {
            // When & Then
            var requestBody = "";

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
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
                                                  .containsEntry("instance", "/api/users");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMissingInRequest() {
            // When & Then
            var requestBody = "";

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // When & Then
            var requestBody = "{}";

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The username must not be empty")
                                                  .contains("The password must not be empty")
                                                  .contains("The role must be a valid Role");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createUserRequest", "field", "username", "message", "The username must not be empty"),
                                                          Map.of("entity", "createUserRequest", "field", "password", "message", "The password must not be empty"),
                                                          Map.of("entity", "createUserRequest", "field", "role", "message", "The role must be a valid Role")
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
                    "password": "",
                    "role": ""
                    }
                    """;

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The username must not be empty")
                                                  .contains("The username must be a valid email address")
                                                  .contains("The password must not be empty")
                                                  .contains("The password must be between 8 and 64 characters")
                                                  .contains("The role must be a valid Role");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createUserRequest", "field", "username", "message", "The username must not be empty"),
                                                          Map.of("entity", "createUserRequest", "field", "username", "message","The username must be a valid email address"),
                                                          Map.of("entity", "createUserRequest", "field", "password", "message","The password must not be empty"),
                                                          Map.of("entity", "createUserRequest", "field", "password", "message", "The password must be between 8 and 64 characters"),
                                                          Map.of("entity", "createUserRequest", "field", "role", "message", "The role must be a valid Role")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidRequestBodyUsernameField() {
            // When & Then
            var requestBody = """
                    {
                    "username": "invalid_username",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted("TEST@09_password?!", USER.name());

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The username must be a valid email address");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createUserRequest", "field", "username", "message", "The username must be a valid email address")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidRequestBodyPasswordField() {
            // When & Then
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "invalid",
                    "role": "%s"
                    }
                    """.formatted("user@asapp.com", USER.name());

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The password must be between 8 and 64 characters");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createUserRequest", "field", "password", "message", "The password must be between 8 and 64 characters")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidRequestBodyRoleField() {
            // When & Then
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "invalid_role"
                    }
                    """.formatted("user@asapp.com", "TEST@09_password?!");

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users");
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The role must be a valid Role");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createUserRequest", "field", "role", "message", "The role must be a valid Role")
                                                  );
                       //@formatter:on
                   });
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_UserIdMissingInPath() {
            // When & Then
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted("new_user@asapp.com", "new_test#Password12", ADMIN.name());

            var requestBuilder = put(USERS_ROOT_PATH + "/").contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.NOT_FOUND)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Not Found")
                                                  .containsEntry("status", 404)
                                                  .containsEntry("detail", "No static resource api/users.")
                                                  .containsEntry("instance", "/api/users/");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidUserIdPath() {
            // When & Then
            var userIdPath = 1L;
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted("new_user@asapp.com", "new_test#Password12", ADMIN.name());

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                  .containsEntry("instance", "/api/users/1");
                   });
        }

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_RequestBodyNotJson() {
            // When & Then
            var userIdPath = UUID.fromString("5b0e9f8d-4a7c-4d2e-b3f8-9d6e8c7f5a4b");
            var requestBody = "";

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath).contentType(MediaType.TEXT_PLAIN)
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
                                                  .containsEntry("instance", "/api/users/" + userIdPath);
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMissingInRequest() {
            // When & Then
            var userIdPath = UUID.fromString("6c1f0a9e-5b8d-4e3f-c4a9-0e7f9d8e6b5c");
            var requestBody = "";

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users/" + userIdPath);
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // When & Then
            var userIdPath = UUID.fromString("7d2a1b0f-6c9e-4f4a-d5b0-1f8a0e9f7d6e");
            var requestBody = "{}";

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users/" + userIdPath);
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The username must not be empty")
                                                  .contains("The password must not be empty")
                                                  .contains("The role must be a valid Role");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateUserRequest", "field", "username", "message", "The username must not be empty"),
                                                          Map.of("entity", "updateUserRequest", "field", "password", "message", "The password must not be empty"),
                                                          Map.of("entity", "updateUserRequest", "field", "role", "message", "The role must be a valid Role")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyEmptyMandatoryFields() {
            // When & Then
            var userIdPath = UUID.fromString("8e3b2c1a-7d0f-4a5b-e6c1-2a9b1f0e8d7f");
            var requestBody = """
                    {
                    "username": "",
                    "password": "",
                    "role": ""
                    }
                    """;

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users/" + userIdPath);
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The username must not be empty")
                                                  .contains("The username must be a valid email address")
                                                  .contains("The password must not be empty")
                                                  .contains("The password must be between 8 and 64 characters")
                                                  .contains("The role must be a valid Role");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateUserRequest", "field", "username", "message", "The username must not be empty"),
                                                          Map.of("entity", "updateUserRequest", "field", "username", "message", "The username must be a valid email address"),
                                                          Map.of("entity", "updateUserRequest", "field", "password", "message", "The password must not be empty"),
                                                          Map.of("entity", "updateUserRequest", "field", "password", "message", "The password must be between 8 and 64 characters"),
                                                          Map.of("entity", "updateUserRequest", "field", "role", "message", "The role must be a valid Role")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidRequestBodyUsernameField() {
            // When & Then
            var userIdPath = UUID.fromString("9f4c3d2b-8e1a-4b6c-f7d2-3b0c2a1f9e8a");
            var requestBody = """
                    {
                    "username": "invalid_username",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted("TEST@09_password?!", ADMIN.name());

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users/" + userIdPath);
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The username must be a valid email address");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateUserRequest", "field", "username", "message", "The username must be a valid email address")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidRequestBodyPasswordField() {
            // When & Then
            var userIdPath = UUID.fromString("0a5d4e3c-9f2b-4c7d-a8e3-4d1c3b2a0f9b");
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "invalid",
                    "role": "%s"
                    }
                    """.formatted("new_user@asapp.com", ADMIN.name());

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users/" + userIdPath);
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The password must be between 8 and 64 characters");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateUserRequest", "field", "password", "message", "The password must be between 8 and 64 characters")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidRequestBodyRoleField() {
            // When & Then
            var userIdPath = UUID.fromString("1b6e5f4d-0a3c-4d8e-b9f4-5e2d4c3b2a1c");
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "invalid_role"
                    }
                    """.formatted("new_user@asapp.com", "new_test#Password12");

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath).contentType(MediaType.APPLICATION_JSON)
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
                                                  .containsEntry("instance", "/api/users/" + userIdPath);
                       assertThatJson(jsonContent).inPath("detail")
                                                  .asString()
                                                  .contains("The role must be a valid Role");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateUserRequest", "field", "role", "message", "The role must be a valid Role")
                                                  );
                       //@formatter:on
                   });
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_UserIdMissingInPath() {
            // When & Then
            var requestBuilder = delete(USERS_ROOT_PATH + "/");
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.NOT_FOUND)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(jsonContent -> {
                       assertThatJson(jsonContent).isObject()
                                                  .containsEntry("type", "about:blank")
                                                  .containsEntry("title", "Not Found")
                                                  .containsEntry("status", 404)
                                                  .containsEntry("detail", "No static resource api/users.")
                                                  .containsEntry("instance", "/api/users/");
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidUserIdPath() {
            // When & Then
            var userIdPath = 1L;

            var requestBuilder = delete(USERS_DELETE_BY_ID_FULL_PATH, userIdPath);
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
                                                  .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                  .containsEntry("instance", "/api/users/1");
                   });
        }

    }

}
