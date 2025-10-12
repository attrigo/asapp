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

import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_RAW_PASSWORD;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_ROLE;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_USERNAME;
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
class UserControllerIT extends WebMvcTestContext {

    @Nested
    class GetUserById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_UserIdPathIsNotPresent() {
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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_UserIdPathIsInvalid() {
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
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_RequestBodyIsNotJson() {
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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyIsNotPresent() {
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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyIsEmpty() {
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
                                                  .contains("The username must not be empty", "The password must not be empty",
                                                          "The role must be a valid Role");
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createUserRequest", "field", "username", "message",
                                                                  "The username must not be empty"),
                                                          Map.of("entity", "createUserRequest", "field", "password", "message",
                                                                  "The password must not be empty"),
                                                          Map.of("entity", "createUserRequest", "field", "role", "message", "The role must be a valid Role"));
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMandatoryFieldsAreEmpty() {
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
                                                  .contains("The username must be a valid email address")
                                                  .contains("The password must be between 8 and 64 characters")
                                                  .contains("The role must be a valid Role");
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createUserRequest", "field", "username", "message",
                                                                  "The username must not be empty"),
                                                          Map.of("entity", "createUserRequest", "field", "username", "message",
                                                                  "The username must be a valid email address"),
                                                          Map.of("entity", "createUserRequest", "field", "password", "message",
                                                                  "The password must not be empty"),
                                                          Map.of("entity", "createUserRequest", "field", "password", "message",
                                                                  "The password must be between 8 and 64 characters"),
                                                          Map.of("entity", "createUserRequest", "field", "role", "message", "The role must be a valid Role"));
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyUsernameFieldIsInvalid() {
            // When & Then
            var requestBody = """
                    {
                    "username": "INVALID_USERNAME",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted(DEFAULT_FAKE_RAW_PASSWORD, DEFAULT_FAKE_ROLE.name());

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
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(Map.of("entity", "createUserRequest", "field", "username", "message",
                                                          "The username must be a valid email address"));
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyPasswordFieldIsInvalid() {
            // When & Then
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "INV_PWD",
                    "role": "%s"
                    }
                    """.formatted(DEFAULT_FAKE_USERNAME, DEFAULT_FAKE_ROLE.name());

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
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(Map.of("entity", "createUserRequest", "field", "password", "message",
                                                          "The password must be between 8 and 64 characters"));
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyRoleFieldIsInvalid() {
            // When & Then
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "INVALID_ROLE"
                    }
                    """.formatted(DEFAULT_FAKE_USERNAME, DEFAULT_FAKE_RAW_PASSWORD);

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
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createUserRequest", "field", "role", "message", "The role must be a valid Role"));
                   });
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_UserIdPathIsNotPresent() {
            // When & Then
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted(DEFAULT_FAKE_USERNAME, DEFAULT_FAKE_RAW_PASSWORD, DEFAULT_FAKE_ROLE.name());

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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_UserIdPathIsInvalid() {
            // When & Then
            var userIdPath = 1L;
            var userToUpdate = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted(DEFAULT_FAKE_USERNAME, DEFAULT_FAKE_RAW_PASSWORD, DEFAULT_FAKE_ROLE.name());

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userIdPath).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(userToUpdate);
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
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_RequestBodyIsNotJson() {
            // When & Then
            var userIdPath = UUID.randomUUID();
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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyIsNotPresent() {
            // When & Then
            var userIdPath = UUID.randomUUID();
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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyIsEmpty() {
            // When & Then
            var userIdPath = UUID.randomUUID();
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
                                                  .contains("The username must not be empty", "The password must not be empty",
                                                          "The role must be a valid Role");
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateUserRequest", "field", "username", "message",
                                                                  "The username must not be empty"),
                                                          Map.of("entity", "updateUserRequest", "field", "password", "message",
                                                                  "The password must not be empty"),
                                                          Map.of("entity", "updateUserRequest", "field", "role", "message", "The role must be a valid Role"));
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMandatoryFieldsAreEmpty() {
            // When & Then
            var userIdPath = UUID.randomUUID();
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
                                                  .contains("The username must be a valid email address")
                                                  .contains("The password must be between 8 and 64 characters")
                                                  .contains("The role must be a valid Role");
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateUserRequest", "field", "username", "message",
                                                                  "The username must not be empty"),
                                                          Map.of("entity", "updateUserRequest", "field", "username", "message",
                                                                  "The username must be a valid email address"),
                                                          Map.of("entity", "updateUserRequest", "field", "password", "message",
                                                                  "The password must not be empty"),
                                                          Map.of("entity", "updateUserRequest", "field", "password", "message",
                                                                  "The password must be between 8 and 64 characters"),
                                                          Map.of("entity", "updateUserRequest", "field", "role", "message", "The role must be a valid Role"));
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyUsernameFieldIsInvalid() {
            // When & Then
            var userIdPath = UUID.randomUUID();
            var requestBody = """
                    {
                    "username": "INVALID_USERNAME",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted(DEFAULT_FAKE_RAW_PASSWORD, DEFAULT_FAKE_ROLE.name());

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
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(Map.of("entity", "updateUserRequest", "field", "username", "message",
                                                          "The username must be a valid email address"));
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyPasswordFieldIsInvalid() {
            // When & Then
            var userIdPath = UUID.randomUUID();
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "INV_PWD",
                    "role": "%s"
                    }
                    """.formatted(DEFAULT_FAKE_USERNAME, DEFAULT_FAKE_ROLE.name());

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
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(Map.of("entity", "updateUserRequest", "field", "password", "message",
                                                          "The password must be between 8 and 64 characters"));
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyRoleFieldIsInvalid() {
            // When & Then
            var userIdPath = UUID.randomUUID();
            var requestBody = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "INVALID_ROLE"
                    }
                    """.formatted(DEFAULT_FAKE_USERNAME, DEFAULT_FAKE_RAW_PASSWORD);

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
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateUserRequest", "field", "role", "message", "The role must be a valid Role"));
                   });
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_UserIdPathIsNotPresent() {
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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_UserIdPathIsInvalid() {
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
