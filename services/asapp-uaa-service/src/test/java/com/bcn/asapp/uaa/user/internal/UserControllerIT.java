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

package com.bcn.asapp.uaa.user.internal;

import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_ROOT_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bcn.asapp.dto.user.UserDTO;
import com.bcn.asapp.uaa.config.SecurityConfiguration;
import com.bcn.asapp.uaa.security.authentication.verifier.JwtVerifier;
import com.bcn.asapp.uaa.security.web.JwtAuthenticationEntryPoint;
import com.bcn.asapp.uaa.security.web.JwtAuthenticationFilter;
import com.bcn.asapp.uaa.user.UserService;

@WebMvcTest(UserRestController.class)
@Import(value = { SecurityConfiguration.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class })
@WithMockUser
class UserControllerIT {

    public static final String USERS_ROOT_PATH_WITH_FINAL_SLASH = USERS_ROOT_PATH + "/";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtVerifier jwtVerifierMock;

    @MockitoBean
    private UserService userServiceMock;

    private UUID fakeUserId;

    private String fakeUserUsername;

    private String fakeUserPassword;

    private String fakeUserRole;

    @BeforeEach
    void beforeEach() {
        this.fakeUserId = UUID.randomUUID();
        this.fakeUserUsername = "TEST USERNAME";
        this.fakeUserPassword = "TEST PASSWORD";
        this.fakeUserRole = "USER";
    }

    @Nested
    class GetUserById {

        @Test
        @WithAnonymousUser
        @DisplayName("GIVEN JWT is not present WHEN get user by id THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotPresent_GetUserById_ReturnsStatusUnauthorizedAndEmptyBody() throws Exception {
            // When & Then
            var idToFind = fakeUserId;

            var requestBuilder = get(USERS_GET_BY_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN user id is empty WHEN get a user by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
        void UserIdIsEmpty_GetUserById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var requestBuilder = get(USERS_ROOT_PATH_WITH_FINAL_SLASH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Not Found")))
                   .andExpect(jsonPath("$.status", is(404)))
                   .andExpect(jsonPath("$.detail", startsWith("No static resource")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH)));
        }

        @Test
        @DisplayName("GIVEN user id is not a valid UUID WHEN get a user by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserIdIsNotUUID_GetUserById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToFound = 1L;

            var requestBuilder = get(USERS_GET_BY_ID_FULL_PATH, idToFound);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH + idToFound)));
        }

        @Test
        @DisplayName("GIVEN user id does not exists WHEN get a user by id THEN returns HTTP response with status NOT_FOUND And without body")
        void UserIdNotExists_GetUserById_ReturnsStatusNotFoundAndWithoutBody() throws Exception {
            // Given
            given(userServiceMock.findById(any(UUID.class))).willReturn(Optional.empty());

            // When & Then
            var idToFind = fakeUserId;

            var requestBuilder = get(USERS_GET_BY_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN user id exists WHEN get a user by id THEN returns HTTP response with status OK And the body with the user found")
        void UserIdExists_GetUserById_ReturnsStatusOkAndBodyWithUserFound() throws Exception {
            // Given
            var fakeUser = new UserDTO(fakeUserId, fakeUserUsername, fakeUserPassword, fakeUserRole);
            given(userServiceMock.findById(any(UUID.class))).willReturn(Optional.of(fakeUser));

            // When & Then
            var idToFind = fakeUserId;

            var requestBuilder = get(USERS_GET_BY_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.id", is(fakeUserId.toString())))
                   .andExpect(jsonPath("$.username", is(fakeUserUsername)))
                   .andExpect(jsonPath("$.password", is("********")))
                   .andExpect(jsonPath("$.role", is(fakeUserRole)));
        }

    }

    @Nested
    class GetAllUsers {

        @Test
        @WithAnonymousUser
        @DisplayName("GIVEN JWT is not present WHEN get all users THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotPresent_GetAllUsers_ReturnsStatusUnauthorizedAndEmptyBody() throws Exception {
            // When & Then
            var requestBuilder = get(USERS_GET_ALL_FULL_PATH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN there are not users WHEN get all users THEN returns HTTP response with status OK And an empty body")
        void ThereAreNotUsers_GetAllUsers_ReturnsStatusOkAndEmptyBody() throws Exception {
            // Given
            given(userServiceMock.findAll()).willReturn(Collections.emptyList());

            // When & Then
            var requestBuilder = get(USERS_GET_ALL_FULL_PATH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("GIVEN there are users WHEN get all users THEN returns HTTP response with status OK And the body with all users found")
        void ThereAreUsers_GetAllUsers_ReturnsStatusOkAndBodyWithUsersFound() throws Exception {
            var fakeUserId1 = UUID.randomUUID();
            var fakeUserId2 = UUID.randomUUID();
            var fakeUserId3 = UUID.randomUUID();
            var fakeUserUsername1 = fakeUserUsername + " 1";
            var fakeUserUsername2 = fakeUserUsername + " 2";
            var fakeUserUsername3 = fakeUserUsername + " 3";
            var fakeUserPassword1 = fakeUserPassword + " 1";
            var fakeUserPassword2 = fakeUserPassword + " 2";
            var fakeUserPassword3 = fakeUserPassword + " 3";

            // Given
            var fakeUser1 = new UserDTO(fakeUserId1, fakeUserUsername1, fakeUserPassword1, fakeUserRole);
            var fakeUser2 = new UserDTO(fakeUserId2, fakeUserUsername2, fakeUserPassword2, fakeUserRole);
            var fakeUser3 = new UserDTO(fakeUserId3, fakeUserUsername3, fakeUserPassword3, fakeUserRole);
            var fakeUsers = List.of(fakeUser1, fakeUser2, fakeUser3);
            given(userServiceMock.findAll()).willReturn(fakeUsers);

            // When & Then
            var requestBuilder = get(USERS_GET_ALL_FULL_PATH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$[0].id", is(fakeUserId1.toString())))
                   .andExpect(jsonPath("$[0].username", is(fakeUserUsername1)))
                   .andExpect(jsonPath("$[0].password", is("********")))
                   .andExpect(jsonPath("$[0].role", is(fakeUserRole)))
                   .andExpect(jsonPath("$[1].username", is(fakeUserUsername2)))
                   .andExpect(jsonPath("$[1].password", is("********")))
                   .andExpect(jsonPath("$[1].role", is(fakeUserRole)))
                   .andExpect(jsonPath("$[2].username", is(fakeUserUsername3)))
                   .andExpect(jsonPath("$[2].password", is("********")))
                   .andExpect(jsonPath("$[2].role", is(fakeUserRole)));
        }

    }

    @WithAnonymousUser
    @Nested
    class CreateUser {

        @Test
        @DisplayName("GIVEN user fields are not a valid Json WHEN create a user THEN returns HTTP response with status Unsupported Media Type And the body with the problem details")
        void UserFieldsAreNotJson_CreateUser_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userToCreate = "";

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                             .content(userToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
                   .andExpect(jsonPath("$.status", is(415)))
                   .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH)));
        }

        @Test
        @DisplayName("GIVEN user fields are not present WHEN create a user THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserFieldsAreNotPresent_CreateUser_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userToCreate = "";

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(userToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH)));
        }

        @Test
        @DisplayName("GIVEN user mandatory fields are not present WHEN create a user THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserMandatoryFieldsAreNotPresent_CreateUser_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userToCreate = """
                    {
                    "username": "",
                    "password": "",
                    "role": ""
                    }
                    """;

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(userToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The username must not be empty")))
                   .andExpect(jsonPath("$.detail", containsString("The password must not be empty")))
                   .andExpect(jsonPath("$.detail", containsString("The role must be a valid Role")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH)))
                   .andExpect(jsonPath("$.errors", hasSize(3)))
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'username' && @.message == 'The username must not be empty')]").exists())
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'password' && @.message == 'The password must not be empty')]").exists())
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'role' && @.message == 'The role must be a valid Role')]").exists());
        }

        @Test
        @DisplayName("GIVEN user mandatory fields are empty WHEN create a user THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserMandatoryFieldsAreEmpty_CreateUser_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userToCreate = """
                    {
                    "username": "",
                    "password": "",
                    "role": ""
                    }
                    """;

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(userToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The username must not be empty")))
                   .andExpect(jsonPath("$.detail", containsString("The password must not be empty")))
                   .andExpect(jsonPath("$.detail", containsString("The role must be a valid Role")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH)))
                   .andExpect(jsonPath("$.errors", hasSize(3)))
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'username' && @.message == 'The username must not be empty')]").exists())
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'password' && @.message == 'The password must not be empty')]").exists())
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'role' && @.message == 'The role must be a valid Role')]").exists());
        }

        @Test
        @DisplayName("GIVEN user role field is invalid WHEN create a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserRoleFieldIsInvalid_CreateUser_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userToCreate = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "TEST"
                    }
                    """.formatted(fakeUserUsername, fakeUserPassword);

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(userToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The role must be a valid Role")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH)))
                   .andExpect(jsonPath("$.errors[0].entity", is("userDTO")))
                   .andExpect(jsonPath("$.errors[0].field", is("role")))
                   .andExpect(jsonPath("$.errors[0].message", is("The role must be a valid Role")));
        }

        @Test
        @DisplayName("GIVEN user fields are valid WHEN create a user THEN returns HTTP response with status CREATED And the body with the user created")
        void UserFieldsAreValid_CreateUser_ReturnsStatusCreatedAndBodyWithUserCreated() throws Exception {
            // Given
            var fakeUser = new UserDTO(fakeUserId, fakeUserUsername, fakeUserPassword, fakeUserRole);
            given(userServiceMock.create(any(UserDTO.class))).willReturn(fakeUser);

            // When & Then
            var userToCreate = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted(fakeUserUsername, fakeUserPassword, fakeUserRole);

            var requestBuilder = post(USERS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                             .content(userToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isCreated())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.id", is(fakeUserId.toString())))
                   .andExpect(jsonPath("$.username", is(fakeUserUsername)))
                   .andExpect(jsonPath("$.password", is("********")))
                   .andExpect(jsonPath("$.role", is(fakeUserRole)));
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        @WithAnonymousUser
        @DisplayName("GIVEN JWT is not present WHEN update a user by id THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotPresent_UpdateUserById_ReturnsStatusUnauthorizedAndEmptyBody() throws Exception {
            // When & Then
            var idToUpdate = fakeUserId;
            var userToUpdate = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted(fakeUserUsername, fakeUserPassword, fakeUserRole);

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(userToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN user id is empty WHEN update a user by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
        void UserIdIsEmpty_UpdateUserById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userToUpdate = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted(fakeUserUsername, fakeUserPassword, fakeUserRole);

            var requestBuilder = put(USERS_ROOT_PATH_WITH_FINAL_SLASH).contentType(MediaType.APPLICATION_JSON)
                                                                      .content(userToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Not Found")))
                   .andExpect(jsonPath("$.status", is(404)))
                   .andExpect(jsonPath("$.detail", startsWith("No static resource")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH)));
        }

        @Test
        @DisplayName("GIVEN user id is not a valid UUID WHEN update a user by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserIdIsNotUUID_UpdateUserById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = 1L;
            var userToUpdate = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted(fakeUserUsername, fakeUserPassword, fakeUserRole);

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(userToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN user fields are not a valid Json WHEN update a user by id THEN returns HTTP response with status Unsupported Media Type And content with the problem details")
        void NewUserDataFieldsAreNotJson_UpdateUserById_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeUserId;
            var userToUpdate = "";

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.TEXT_PLAIN)
                                                                              .content(userToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
                   .andExpect(jsonPath("$.status", is(415)))
                   .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN new user data fields are not present WHEN update a user by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewUserDataFieldsAreNotPresent_UpdateUserById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeUserId;
            var userToUpdate = "";

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(userToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN new user data mandatory fields are not present WHEN update a user by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewUserDataMandatoryFieldsAreNotPresent_UpdateUserById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeUserId;
            var userToUpdate = """
                    {
                    "username": "",
                    "password": "",
                    "role": ""
                    }
                    """;

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(userToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The username must not be empty")))
                   .andExpect(jsonPath("$.detail", containsString("The password must not be empty")))
                   .andExpect(jsonPath("$.detail", containsString("The role must be a valid Role")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)))
                   .andExpect(jsonPath("$.errors", hasSize(3)))
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'username' && @.message == 'The username must not be empty')]").exists())
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'password' && @.message == 'The password must not be empty')]").exists())
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'role' && @.message == 'The role must be a valid Role')]").exists());
        }

        @Test
        @DisplayName("GIVEN new user data mandatory fields are empty WHEN update a user by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewUserDataMandatoryFieldsAreEmpty_UpdateUserById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeUserId;
            var userToUpdate = """
                    {
                    "username": "",
                    "password": "",
                    "role": ""
                    }
                    """;

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(userToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The username must not be empty")))
                   .andExpect(jsonPath("$.detail", containsString("The password must not be empty")))
                   .andExpect(jsonPath("$.detail", containsString("The role must be a valid Role")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)))
                   .andExpect(jsonPath("$.errors", hasSize(3)))
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'username' && @.message == 'The username must not be empty')]").exists())
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'password' && @.message == 'The password must not be empty')]").exists())
                   .andExpect(jsonPath("$.errors[?(@.entity == 'userDTO' && @.field == 'role' && @.message == 'The role must be a valid Role')]").exists());
        }

        @Test
        @DisplayName("GIVEN user role field is invalid WHEN update a task THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserRoleFieldIsInvalid_UpdateUser_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeUserId;
            var userToUpdate = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "TEST"
                    }
                    """.formatted(fakeUserUsername, fakeUserPassword);

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(userToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The role must be a valid Role")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)))
                   .andExpect(jsonPath("$.errors[0].entity", is("userDTO")))
                   .andExpect(jsonPath("$.errors[0].field", is("role")))
                   .andExpect(jsonPath("$.errors[0].message", is("The role must be a valid Role")));
        }

        @Test
        @DisplayName("GIVEN user id does not exists WHEN update a user by id THEN returns HTTP response with status NOT_FOUND And without body")
        void UserIdNotExists_UpdateUserById_ReturnsStatusNotFoundAndWithoutBody() throws Exception {
            // Given
            given(userServiceMock.updateById(any(UUID.class), any(UserDTO.class))).willReturn(Optional.empty());

            // When & Then
            var idToUpdate = fakeUserId;
            var userToUpdate = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted(fakeUserUsername, fakeUserPassword, fakeUserRole);

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(userToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN user id exists WHEN update a user by id THEN returns HTTP response with status OK And the body with the user updated")
        void UserIdExists_UpdateUserById_ReturnsStatusOkAndBodyWithUserUpdated() throws Exception {
            // Given
            var fakeUser = new UserDTO(fakeUserId, fakeUserUsername, fakeUserPassword, fakeUserRole);
            given(userServiceMock.updateById(any(UUID.class), any(UserDTO.class))).willReturn(Optional.of(fakeUser));

            // When & Then
            var idToUpdate = fakeUserId;
            var userToUpdate = """
                    {
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                    }
                    """.formatted(fakeUserUsername, fakeUserPassword, fakeUserRole);

            var requestBuilder = put(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                              .content(userToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.id", is(fakeUserId.toString())))
                   .andExpect(jsonPath("$.username", is(fakeUserUsername)))
                   .andExpect(jsonPath("$.password", is("********")))
                   .andExpect(jsonPath("$.role", is(fakeUserRole)));
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        @WithAnonymousUser
        @DisplayName("GIVEN JWT is not present WHEN delete a user by id THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotPresent_DeleteUserById_ReturnsStatusUnauthorizedAndEmptyBody() throws Exception {
            // When
            var idToDelete = fakeUserId;

            var requestBuilder = delete(USERS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN user id is empty WHEN delete a user by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
        void UserIdIsEmpty_DeleteUserById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var requestBuilder = delete(USERS_ROOT_PATH_WITH_FINAL_SLASH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Not Found")))
                   .andExpect(jsonPath("$.status", is(404)))
                   .andExpect(jsonPath("$.detail", startsWith("No static resource")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH)));
        }

        @Test
        @DisplayName("GIVEN user id is not a valid UUID WHEN delete a user by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserIdIsNotUUID_DeleteUserById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToDelete = 1L;

            var requestBuilder = delete(USERS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
                   .andExpect(jsonPath("$.instance", is(USERS_ROOT_PATH_WITH_FINAL_SLASH + idToDelete)));
        }

        @Test
        @DisplayName("GIVEN user id does not exists WHEN delete a user by id THEN returns HTTP response with status NOT_FOUND And without body")
        void UserIdNotExists_DeleteUserById_ReturnsStatusNotFoundAndWithoutBody() throws Exception {
            // Given
            given(userServiceMock.deleteById(any(UUID.class))).willReturn(false);

            // When
            var idToDelete = fakeUserId;

            var requestBuilder = delete(USERS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN user id exists WHEN delete a user by id THEN returns HTTP response with status NO_CONTENT And without body")
        void UserIdExists_DeleteUserById_ReturnsStatusNoContentAndWithoutBody() throws Exception {
            // Given
            given(userServiceMock.deleteById(any(UUID.class))).willReturn(true);

            // When
            var idToDelete = fakeUserId;

            var requestBuilder = delete(USERS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNoContent())
                   .andExpect(jsonPath("$").doesNotExist());
        }

    }

}
