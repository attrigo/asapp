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
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.users.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static com.bcn.asapp.users.testutil.fixture.UserMother.aUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import com.bcn.asapp.users.application.user.in.command.CreateUserCommand;
import com.bcn.asapp.users.application.user.in.command.UpdateUserCommand;
import com.bcn.asapp.users.application.user.in.result.UserWithTasksResult;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.infrastructure.user.in.request.CreateUserRequest;
import com.bcn.asapp.users.infrastructure.user.in.request.UpdateUserRequest;
import com.bcn.asapp.users.infrastructure.user.in.response.CreateUserResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.GetAllUsersResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.GetUserByIdResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.UpdateUserResponse;
import com.bcn.asapp.users.testutil.RestDocsConstrainedFields;
import com.bcn.asapp.users.testutil.RestDocsWebMvcTestContext;

/**
 * Tests {@link UserRestController} REST API documentation.
 * <p>
 * Coverage:
 * <li>Generates API documentation snippets for all user endpoints and error responses</li>
 * <li>Documents path parameters, request fields, and response fields</li>
 * <li>Covers successful request and response flows for each HTTP operation</li>
 * <li>Covers error responses for validation failures, unauthorized access, not found, and server errors</li>
 */
@WithMockUser
class UserRestControllerDocumentationIT extends RestDocsWebMvcTestContext {

    @Nested
    class GetUserById {

        @Test
        void DocumentsGetUserById_UserFound() throws Exception {
            // Given
            var user = aUser();
            var userIdValue = user.getId()
                                  .value();
            var firstNameValue = user.getFirstName()
                                     .value();
            var lastNameValue = user.getLastName()
                                    .value();
            var emailValue = user.getEmail()
                                 .value();
            var phoneNumberValue = user.getPhoneNumber()
                                       .value();
            var taskIds = List.<UUID>of();
            var userWithTasksResult = new UserWithTasksResult(user, List.of());
            var response = new GetUserByIdResponse(userIdValue, firstNameValue, lastNameValue, emailValue, phoneNumberValue, taskIds);

            given(readUserUseCase.getUserById(any(UUID.class))).willReturn(Optional.of(userWithTasksResult));
            given(userMapper.toGetUserByIdResponse(any(UserWithTasksResult.class))).willReturn(response);

            // When & Then
            mockMvc.perform(get(USERS_GET_BY_ID_FULL_PATH, userIdValue).accept(APPLICATION_JSON)
                                                                       .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-user-by-id",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           pathParameters(parameterWithName("id").description("The user's unique identifier")),
                           responseFields(
                                   fieldWithPath("user_id").description("The user's unique identifier"),
                                   fieldWithPath("first_name").description("The user's first name"),
                                   fieldWithPath("last_name").description("The user's last name"),
                                   fieldWithPath("email").description("The user's email address"),
                                   fieldWithPath("phone_number").description("The user's phone number"),
                                   fieldWithPath("task_ids").description("The identifiers of tasks associated with the user")
                           )
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class GetAllUsers {

        @Test
        void DocumentsGetAllUsers() throws Exception {
            // Given
            var user = aUser();
            var userIdValue = user.getId()
                                  .value();
            var firstNameValue = user.getFirstName()
                                     .value();
            var lastNameValue = user.getLastName()
                                    .value();
            var emailValue = user.getEmail()
                                 .value();
            var phoneNumberValue = user.getPhoneNumber()
                                       .value();
            var response = new GetAllUsersResponse(userIdValue, firstNameValue, lastNameValue, emailValue, phoneNumberValue);

            given(readUserUseCase.getAllUsers()).willReturn(List.of(user));
            given(userMapper.toGetAllUsersResponse(any(User.class))).willReturn(response);

            // When & Then
            mockMvc.perform(get(USERS_GET_ALL_FULL_PATH).accept(APPLICATION_JSON)
                                                        .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-all-users",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           responseFields(
                                   fieldWithPath("[].user_id").description("The user's unique identifier"),
                                   fieldWithPath("[].first_name").description("The user's first name"),
                                   fieldWithPath("[].last_name").description("The user's last name"),
                                   fieldWithPath("[].email").description("The user's email address"),
                                   fieldWithPath("[].phone_number").description("The user's phone number")
                           )
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class CreateUser {

        @Test
        void DocumentsCreateUser() throws Exception {
            // Given
            var fields = new RestDocsConstrainedFields(CreateUserRequest.class);
            var user = aUser();
            var userIdValue = user.getId()
                                  .value();
            var firstNameValue = user.getFirstName()
                                     .value();
            var lastNameValue = user.getLastName()
                                    .value();
            var emailValue = user.getEmail()
                                 .value();
            var phoneNumberValue = user.getPhoneNumber()
                                       .value();
            var requestBody = """
                    {
                        "first_name": "%s",
                        "last_name": "%s",
                        "email": "%s",
                        "phone_number": "%s"
                    }
                    """.formatted(firstNameValue, lastNameValue, emailValue, phoneNumberValue);
            var createUserCommand = new CreateUserCommand(firstNameValue, lastNameValue, emailValue, phoneNumberValue);
            var response = new CreateUserResponse(userIdValue);

            given(userMapper.toCreateUserCommand(any(CreateUserRequest.class))).willReturn(createUserCommand);
            given(createUserUseCase.createUser(any(CreateUserCommand.class))).willReturn(user);
            given(userMapper.toCreateUserResponse(any(User.class))).willReturn(response);

            // When & Then
            mockMvc.perform(post(USERS_CREATE_FULL_PATH).contentType(APPLICATION_JSON)
                                                        .content(requestBody))
                   .andExpect(status().isCreated())
                   .andDo(
                   // @formatter:off
                       document("create-user",
                           requestFields(
                                   fields.withPath("first_name", "firstName").description("The user's first name"),
                                   fields.withPath("last_name", "lastName").description("The user's last name"),
                                   fields.withPath("email").description("The user's email address"),
                                   fields.withPath("phone_number", "phoneNumber").description("The user's phone number")
                           ),
                           responseFields(fieldWithPath("user_id").description("The created user's unique identifier"))
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        void DocumentsUpdateUserById_UserFound() throws Exception {
            // Given
            var fields = new RestDocsConstrainedFields(UpdateUserRequest.class);
            var user = aUser();
            var userIdValue = user.getId()
                                  .value();
            var firstNameValue = user.getFirstName()
                                     .value();
            var lastNameValue = user.getLastName()
                                    .value();
            var emailValue = user.getEmail()
                                 .value();
            var phoneNumberValue = user.getPhoneNumber()
                                       .value();
            var requestBody = """
                    {
                        "first_name": "%s",
                        "last_name": "%s",
                        "email": "%s",
                        "phone_number": "%s"
                    }
                    """.formatted(firstNameValue, lastNameValue, emailValue, phoneNumberValue);
            var updateUserCommand = new UpdateUserCommand(userIdValue, firstNameValue, lastNameValue, emailValue, phoneNumberValue);
            var response = new UpdateUserResponse(userIdValue);

            given(userMapper.toUpdateUserCommand(any(UUID.class), any(UpdateUserRequest.class))).willReturn(updateUserCommand);
            given(updateUserUseCase.updateUserById(any(UpdateUserCommand.class))).willReturn(Optional.of(user));
            given(userMapper.toUpdateUserResponse(any(User.class))).willReturn(response);

            // When & Then
            mockMvc.perform(put(USERS_UPDATE_BY_ID_FULL_PATH, userIdValue).contentType(APPLICATION_JSON)
                                                                          .content(requestBody)
                                                                          .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("update-user-by-id",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           pathParameters(parameterWithName("id").description("The user's unique identifier")),
                           requestFields(
                                   fields.withPath("first_name", "firstName").description("The user's first name"),
                                   fields.withPath("last_name", "lastName").description("The user's last name"),
                                   fields.withPath("email").description("The user's email address"),
                                   fields.withPath("phone_number", "phoneNumber").description("The user's phone number")
                           ),
                           responseFields(fieldWithPath("user_id").description("The updated user's unique identifier"))
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        void DocumentsDeleteUserById() throws Exception {
            // Given
            var user = aUser();
            var userIdValue = user.getId()
                                  .value();

            given(deleteUserUseCase.deleteUserById(any(UUID.class))).willReturn(true);

            // When & Then
            mockMvc.perform(delete(USERS_DELETE_BY_ID_FULL_PATH, userIdValue).header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isNoContent())
                   .andDo(
                   // @formatter:off
                       document("delete-user-by-id",
                           requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                           pathParameters(parameterWithName("id").description("The user's unique identifier")))
                       // @formatter:on
                   );
        }

    }

    @Nested
    class Errors {

        @Test
        void DocumentsValidationFailure() throws Exception {
            // When & Then
            mockMvc.perform(post(USERS_CREATE_FULL_PATH).contentType(APPLICATION_JSON)
                                                        .content("{}"))
                   .andExpect(status().isBadRequest())
                   .andDo(
                   // @formatter:off
                       document("error-validation-failure",
                           relaxedResponseFields(
                               fieldWithPath("title").description("Short summary of the problem type"),
                               fieldWithPath("status").description("HTTP status code"),
                               fieldWithPath("detail").description("Human-readable explanation of the problem"),
                               fieldWithPath("errors").description("List of validation errors"),
                               fieldWithPath("errors[].entity").description("Entity that failed validation"),
                               fieldWithPath("errors[].field").description("Field that failed validation"),
                               fieldWithPath("errors[].message").description("Validation error message")
                           )
                       )
                   // @formatter:on
                   );
        }

        @WithAnonymousUser
        @Test
        void DocumentsUnauthorized() throws Exception {
            // Given
            var userIdValue = UUID.fromString("00000000-0000-0000-0000-000000000001");

            // When & Then
            mockMvc.perform(get(USERS_GET_BY_ID_FULL_PATH, userIdValue).accept(APPLICATION_JSON))
                   .andExpect(status().isUnauthorized())
                   .andDo(document("error-unauthorized"));
        }

        @Test
        void DocumentsNotFound() throws Exception {
            // Given
            var userIdValue = UUID.fromString("00000000-0000-0000-0000-000000000001");

            given(readUserUseCase.getUserById(any(UUID.class))).willReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get(USERS_GET_BY_ID_FULL_PATH, userIdValue).accept(APPLICATION_JSON))
                   .andExpect(status().isNotFound())
                   .andDo(document("error-not-found"));
        }

        @Test
        void DocumentsInternalServerError() throws Exception {
            // Given
            var userIdValue = UUID.fromString("00000000-0000-0000-0000-000000000001");

            given(readUserUseCase.getUserById(any(UUID.class))).willThrow(new DataRetrievalFailureException("Database error"));

            // When & Then
            mockMvc.perform(get(USERS_GET_BY_ID_FULL_PATH, userIdValue).accept(APPLICATION_JSON))
                   .andExpect(status().isInternalServerError())
                   .andDo(
                   // @formatter:off
                       document("error-internal-server-error",
                           relaxedResponseFields(
                               fieldWithPath("title").description("Short summary of the problem type"),
                               fieldWithPath("status").description("HTTP status code"),
                               fieldWithPath("detail").description("Human-readable explanation of the problem"),
                               fieldWithPath("error").description("Machine-readable error code")
                           )
                       )
                   // @formatter:on
                   );
        }

    }

}
