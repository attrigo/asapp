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

package com.bcn.asapp.users.infrastructure.user.in;

import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_ROOT_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_EMAIL;
import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_FIRST_NAME;
import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_LAST_NAME;
import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.DEFAULT_FAKE_PHONE_NUMBER;
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
                                                  .contains("The first name must not be empty")
                                                  .contains("The last name must not be empty")
                                                  .contains("The email must not be empty")
                                                  .contains("The phone number must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMandatoryFieldsAreEmpty() {
            // When & Then
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
                                                  .contains("The first name must not be empty")
                                                  .contains("The last name must not be empty")
                                                  .contains("The email must not be empty")
                                                  .contains("The email must be a valid email address")
                                                  .contains("The phone number must not be empty")
                                                  .contains("The phone number must be a valid phone number");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyEmailFieldIsInvalid() {
            // When & Then
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "INVALID_EMAIL",
                    "phone_number": "%s"
                    }
                    """.formatted(DEFAULT_FAKE_FIRST_NAME, DEFAULT_FAKE_LAST_NAME, DEFAULT_FAKE_PHONE_NUMBER);

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
                                                  .contains("The email must be a valid email address");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "createUserRequest", "field", "email", "message", "The email must be a valid email address")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyPhoneNumberFieldIsInvalid() {
            // When & Then
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "%s",
                    "phone_number": "INVALID_PHONE_NUMBER"
                    }
                    """.formatted(DEFAULT_FAKE_FIRST_NAME, DEFAULT_FAKE_LAST_NAME, DEFAULT_FAKE_EMAIL);

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
                                                  .contains("The phone number must be a valid phone number");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
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
        void ReturnsStatusNotFoundAndBodyWithProblemDetail_UserIdPathIsNotPresent() {
            // When & Then
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "%s",
                    "phone_number": "%s"
                    }
                    """.formatted(DEFAULT_FAKE_FIRST_NAME, DEFAULT_FAKE_LAST_NAME, DEFAULT_FAKE_EMAIL, DEFAULT_FAKE_PHONE_NUMBER);

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
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "%s",
                    "phone_number": "%s"
                    }
                    """.formatted(DEFAULT_FAKE_FIRST_NAME, DEFAULT_FAKE_LAST_NAME, DEFAULT_FAKE_EMAIL, DEFAULT_FAKE_PHONE_NUMBER);

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
                                                  .contains("The first name must not be empty")
                                                  .contains("The last name must not be empty")
                                                  .contains("The email must not be empty")
                                                  .contains("The phone number must not be empty");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyMandatoryFieldsAreEmpty() {
            // When & Then
            var userIdPath = UUID.randomUUID();
            var requestBody = """
                    {
                    "first_name": "",
                    "last_name": "",
                    "email": "",
                    "phone_number": ""
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
                                                  .contains("The first name must not be empty")
                                                  .contains("The last name must not be empty")
                                                  .contains("The email must not be empty")
                                                  .contains("The email must be a valid email address")
                                                  .contains("The phone number must not be empty")
                                                  .contains("The phone number must be a valid phone number");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
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
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyEmailFieldIsInvalid() {
            // When & Then
            var userIdPath = UUID.randomUUID();
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "INVALID_EMAIL",
                    "phone_number": "%s"
                    }
                    """.formatted(DEFAULT_FAKE_FIRST_NAME, DEFAULT_FAKE_LAST_NAME, DEFAULT_FAKE_PHONE_NUMBER);

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
                                                  .contains("The email must be a valid email address");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
                                                  .isArray()
                                                  .containsOnly(
                                                          Map.of("entity", "updateUserRequest", "field", "email", "message", "The email must be a valid email address")
                                                  );
                       //@formatter:on
                   });
        }

        @Test
        void ReturnsStatusBadRequestAndBodyWithProblemDetail_RequestBodyPhoneNumberFieldIsInvalid() {
            // When & Then
            var userIdPath = UUID.randomUUID();
            var requestBody = """
                    {
                    "first_name": "%s",
                    "last_name": "%s",
                    "email": "%s",
                    "phone_number": "INVALID_PHONE_NUMBER"
                    }
                    """.formatted(DEFAULT_FAKE_FIRST_NAME, DEFAULT_FAKE_LAST_NAME, DEFAULT_FAKE_EMAIL);

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
                                                  .contains("The phone number must be a valid phone number");
                   //@formatter:off
                       assertThatJson(jsonContent).inPath("errors")
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
