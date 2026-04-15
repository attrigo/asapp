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

package com.bcn.asapp.users.infrastructure.user.in;

import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_ROOT_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
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
import org.springframework.security.test.context.support.WithMockUser;

import com.bcn.asapp.users.testutil.WebMvcTestContext;

/**
 * Tests {@link UserRestController} request validation and error responses.
 * <p>
 * Coverage:
 * <li>Validates path parameter format (UUID required for user IDs)</li>
 * <li>Validates request content type (JSON required for POST/PUT operations)</li>
 * <li>Validates request body presence and structure</li>
 * <li>Validates mandatory field constraints (first name, last name, email, phone number)</li>
 * <li>Validates email format and phone number format patterns</li>
 * <li>Returns RFC 7807 Problem Details for all validation failures</li>
 * <li>Tests all HTTP endpoints (GET by ID, POST, PUT, DELETE)</li>
 */
@WithMockUser
class UserRestControllerIT extends WebMvcTestContext {

    @Nested
    class GetUserById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_MissingUserId() {
            // Given
            var requestBuilder = get(USERS_ROOT_PATH + "/");

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.NOT_FOUND)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> assertThatJson(json).isObject()
                                                          .containsEntry("title", "Not Found")
                                                          .containsEntry("status", 404)
                                                          .containsEntry("detail", "No static resource api/users.")
                                                          .containsEntry("instance", "/api/users/"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidUserId() {
            // Given
            var userId = 1L;
            var requestBuilder = get(USERS_GET_BY_ID_FULL_PATH, userId);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> assertThatJson(json).isObject()
                                                          .containsEntry("title", "Bad Request")
                                                          .containsEntry("status", 400)
                                                          .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                          .containsEntry("instance", "/api/users/1"));
        }

    }

    @Nested
    class CreateUser {

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_NonJsonRequestBody() {
            // Given
            var requestBody = "";
            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                             .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> assertThatJson(json).isObject()
                                                          .containsEntry("title", "Unsupported Media Type")
                                                          .containsEntry("status", 415)
                                                          .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                          .containsEntry("instance", "/api/users"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_MissingRequestBody() {
            // Given
            var requestBody = "";
            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> assertThatJson(json).isObject()
                                                          .containsEntry("title", "Bad Request")
                                                          .containsEntry("status", 400)
                                                          .containsEntry("detail", "Failed to read request")
                                                          .containsEntry("instance", "/api/users"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // Given
            var requestBody = "{}";
            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> {
                       assertThatJson(json).isObject()
                                           .containsEntry("title", "Bad Request")
                                           .containsEntry("status", 400)
                                           .containsEntry("instance", "/api/users");
                       assertThatJson(json).inPath("detail")
                                           .asString()
                                           .contains("The first name must not be empty")
                                           .contains("The last name must not be empty")
                                           .contains("The email must not be empty")
                                           .contains("The phone number must not be empty");
                   //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "createUserRequest", "field", "firstName", "message", "The first name must not be empty"),
                                                  Map.of("entity", "createUserRequest", "field", "lastName", "message", "The last name must not be empty"),
                                                  Map.of("entity", "createUserRequest", "field", "email", "message", "The email must not be empty"),
                                                  Map.of("entity", "createUserRequest", "field", "phoneNumber", "message", "The phone number must not be empty")
                                          );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyMandatoryFields() {
            // Given
            var requestBody = """
                    {
                    "first_name": "",
                    "last_name": "",
                    "email": "",
                    "phone_number": ""
                    }
                    """;
            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> {
                       assertThatJson(json).isObject()
                                           .containsEntry("title", "Bad Request")
                                           .containsEntry("status", 400)
                                           .containsEntry("instance", "/api/users");
                       assertThatJson(json).inPath("detail")
                                           .asString()
                                           .contains("The first name must not be empty")
                                           .contains("The last name must not be empty")
                                           .contains("The email must not be empty")
                                           .contains("The email must be a valid email address")
                                           .contains("The phone number must not be empty")
                                           .contains("The phone number must be a valid phone number");
                   //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "createUserRequest", "field", "firstName", "message", "The first name must not be empty"),
                                                  Map.of("entity", "createUserRequest", "field", "lastName", "message", "The last name must not be empty"),
                                                  Map.of("entity", "createUserRequest", "field", "email", "message", "The email must not be empty"),
                                                  Map.of("entity", "createUserRequest", "field", "email", "message", "The email must be a valid email address"),
                                                  Map.of("entity", "createUserRequest", "field", "phoneNumber", "message", "The phone number must not be empty"),
                                                  Map.of("entity", "createUserRequest", "field", "phoneNumber", "message", "The phone number must be a valid phone number")
                                          );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidEmailField() {
            // Given
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "invalid_email",
                    "phone_number": "%s"
                    }
                    """.formatted("FirstName", "LastName", "555 555 555");
            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> {
                       assertThatJson(json).isObject()
                                           .containsEntry("title", "Bad Request")
                                           .containsEntry("status", 400)
                                           .containsEntry("instance", "/api/users");
                       assertThatJson(json).inPath("detail")
                                           .asString()
                                           .contains("The email must be a valid email address");
                   //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "createUserRequest", "field", "email", "message", "The email must be a valid email address")
                                          );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidPhoneNumberField() {
            // Given
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "%s",
                    "phone_number": "invalid_phone_number"
                    }
                    """.formatted("FirstName", "LastName", "user@asapp.com");
            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> {
                       assertThatJson(json).isObject()
                                           .containsEntry("title", "Bad Request")
                                           .containsEntry("status", 400)
                                           .containsEntry("instance", "/api/users");
                       assertThatJson(json).inPath("detail")
                                           .asString()
                                           .contains("The phone number must be a valid phone number");
                   //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "createUserRequest", "field", "phoneNumber", "message", "The phone number must be a valid phone number")
                                          );
                       //@formatter:on
                   });
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_MissingUserId() {
            // Given
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "%s",
                    "phone_number": "%s"
                    }
                    """.formatted("New FirstName", "New LastName", "new_user@asapp.com", "666 666 666");
            var requestBuilder = put(USERS_ROOT_PATH + "/").contentType(MediaType.APPLICATION_JSON)
                                                           .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.NOT_FOUND)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> assertThatJson(json).isObject()
                                                          .containsEntry("title", "Not Found")
                                                          .containsEntry("status", 404)
                                                          .containsEntry("detail", "No static resource api/users.")
                                                          .containsEntry("instance", "/api/users/"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidUserId() {
            // Given
            var userId = 1L;
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "%s",
                    "phone_number": "%s"
                    }
                    """.formatted("New FirstName", "New LastName", "new_user@asapp.com", "666 666 666");
            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userId).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> assertThatJson(json).isObject()
                                                          .containsEntry("title", "Bad Request")
                                                          .containsEntry("status", 400)
                                                          .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                          .containsEntry("instance", "/api/users/1"));
        }

        @Test
        void ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetail_NonJsonRequestBody() {
            // Given
            var userId = UUID.fromString("82bb9f78-4851-4f5b-a252-412995b26864");
            var requestBody = "";
            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userId).contentType(MediaType.TEXT_PLAIN)
                                                                          .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> assertThatJson(json).isObject()
                                                          .containsEntry("title", "Unsupported Media Type")
                                                          .containsEntry("status", 415)
                                                          .containsEntry("detail", "Content-Type 'text/plain' is not supported.")
                                                          .containsEntry("instance", "/api/users/" + userId));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_MissingRequestBody() {
            // Given
            var userId = UUID.fromString("82bb9f78-4851-4f5b-a252-412995b26864");
            var requestBody = "";
            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userId).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> assertThatJson(json).isObject()
                                                          .containsEntry("title", "Bad Request")
                                                          .containsEntry("status", 400)
                                                          .containsEntry("detail", "Failed to read request")
                                                          .containsEntry("instance", "/api/users/" + userId));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyRequestBody() {
            // Given
            var userId = UUID.fromString("82bb9f78-4851-4f5b-a252-412995b26864");
            var requestBody = "{}";
            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userId).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> {
                       assertThatJson(json).isObject()
                                           .containsEntry("title", "Bad Request")
                                           .containsEntry("status", 400)
                                           .containsEntry("instance", "/api/users/" + userId);
                       assertThatJson(json).inPath("detail")
                                           .asString()
                                           .contains("The first name must not be empty")
                                           .contains("The last name must not be empty")
                                           .contains("The email must not be empty")
                                           .contains("The phone number must not be empty");
                   //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "updateUserRequest", "field", "firstName", "message", "The first name must not be empty"),
                                                  Map.of("entity", "updateUserRequest", "field", "lastName", "message", "The last name must not be empty"),
                                                  Map.of("entity", "updateUserRequest", "field", "email", "message", "The email must not be empty"),
                                                  Map.of("entity", "updateUserRequest", "field", "phoneNumber", "message", "The phone number must not be empty")
                                          );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_EmptyMandatoryFields() {
            // Given
            var userId = UUID.fromString("82bb9f78-4851-4f5b-a252-412995b26864");
            var requestBody = """
                    {
                    "first_name": "",
                    "last_name": "",
                    "email": "",
                    "phone_number": ""
                    }
                    """;
            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userId).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> {
                       assertThatJson(json).isObject()
                                           .containsEntry("title", "Bad Request")
                                           .containsEntry("status", 400)
                                           .containsEntry("instance", "/api/users/" + userId);
                       assertThatJson(json).inPath("detail")
                                           .asString()
                                           .contains("The first name must not be empty")
                                           .contains("The last name must not be empty")
                                           .contains("The email must not be empty")
                                           .contains("The email must be a valid email address")
                                           .contains("The phone number must not be empty")
                                           .contains("The phone number must be a valid phone number");
                   //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "updateUserRequest", "field", "firstName", "message", "The first name must not be empty"),
                                                  Map.of("entity", "updateUserRequest", "field", "lastName", "message", "The last name must not be empty"),
                                                  Map.of("entity", "updateUserRequest", "field", "email", "message", "The email must not be empty"),
                                                  Map.of("entity", "updateUserRequest", "field", "email", "message", "The email must be a valid email address"),
                                                  Map.of("entity", "updateUserRequest", "field", "phoneNumber", "message", "The phone number must not be empty"),
                                                  Map.of("entity", "updateUserRequest", "field", "phoneNumber", "message", "The phone number must be a valid phone number")
                                          );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidEmailField() {
            // Given
            var userId = UUID.fromString("82bb9f78-4851-4f5b-a252-412995b26864");
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "invalid_email",
                    "phone_number": "%s"
                    }
                    """.formatted("New FirstName", "New LastName", "666 666 666");
            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userId).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> {
                       assertThatJson(json).isObject()
                                           .containsEntry("title", "Bad Request")
                                           .containsEntry("status", 400)
                                           .containsEntry("instance", "/api/users/" + userId);
                       assertThatJson(json).inPath("detail")
                                           .asString()
                                           .contains("The email must be a valid email address");
                   //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "updateUserRequest", "field", "email", "message", "The email must be a valid email address")
                                          );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidPhoneNumberField() {
            // Given
            var userId = UUID.fromString("82bb9f78-4851-4f5b-a252-412995b26864");
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "%s",
                    "phone_number": "invalid_phone_number"
                    }
                    """.formatted("New FirstName", "New LastName", "new_user@asapp.com");
            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, userId).contentType(MediaType.APPLICATION_JSON)
                                                                          .content(requestBody);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> {
                       assertThatJson(json).isObject()
                                           .containsEntry("title", "Bad Request")
                                           .containsEntry("status", 400)
                                           .containsEntry("instance", "/api/users/" + userId);
                       assertThatJson(json).inPath("detail")
                                           .asString()
                                           .contains("The phone number must be a valid phone number");
                   //@formatter:off
                       assertThatJson(json).inPath("errors")
                                          .isArray()
                                          .containsOnly(
                                                  Map.of("entity", "updateUserRequest", "field", "phoneNumber", "message", "The phone number must be a valid phone number")
                                          );
                       //@formatter:on
                   });
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_MissingUserId() {
            // Given
            var requestBuilder = delete(USERS_ROOT_PATH + "/");

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.NOT_FOUND)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> assertThatJson(json).isObject()
                                                          .containsEntry("title", "Not Found")
                                                          .containsEntry("status", 404)
                                                          .containsEntry("detail", "No static resource api/users.")
                                                          .containsEntry("instance", "/api/users/"));
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_InvalidUserId() {
            // Given
            var userId = 1L;
            var requestBuilder = delete(USERS_DELETE_BY_ID_FULL_PATH, userId);

            // When & Then
            mockMvc.perform(requestBuilder)
                   .assertThat()
                   .hasStatus(HttpStatus.BAD_REQUEST)
                   .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                   .bodyJson()
                   .convertTo(String.class)
                   .satisfies(json -> assertThatJson(json).isObject()
                                                          .containsEntry("title", "Bad Request")
                                                          .containsEntry("status", 400)
                                                          .containsEntry("detail", "Failed to convert 'id' with value: '1'")
                                                          .containsEntry("instance", "/api/users/1"));
        }

    }

}
