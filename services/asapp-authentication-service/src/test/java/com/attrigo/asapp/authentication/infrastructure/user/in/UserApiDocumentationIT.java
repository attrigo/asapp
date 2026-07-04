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

package com.attrigo.asapp.authentication.infrastructure.user.in;

import static com.attrigo.asapp.authentication.testutil.fixture.UserMother.anActiveUser;
import static com.attrigo.asapp.url.authentication.UserApiUrl.USERS_CREATE_FULL_PATH;
import static com.attrigo.asapp.url.authentication.UserApiUrl.USERS_DELETE_BY_ID_FULL_PATH;
import static com.attrigo.asapp.url.authentication.UserApiUrl.USERS_GET_ALL_FULL_PATH;
import static com.attrigo.asapp.url.authentication.UserApiUrl.USERS_GET_BY_ID_FULL_PATH;
import static com.attrigo.asapp.url.authentication.UserApiUrl.USERS_UPDATE_BY_ID_FULL_PATH;
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

import com.attrigo.asapp.authentication.application.authentication.TokenStoreException;
import com.attrigo.asapp.authentication.application.user.in.command.CreateUserCommand;
import com.attrigo.asapp.authentication.application.user.in.command.UpdateUserCommand;
import com.attrigo.asapp.authentication.domain.user.User;
import com.attrigo.asapp.authentication.infrastructure.user.in.request.CreateUserRequest;
import com.attrigo.asapp.authentication.infrastructure.user.in.request.UpdateUserRequest;
import com.attrigo.asapp.authentication.infrastructure.user.in.response.CreateUserResponse;
import com.attrigo.asapp.authentication.infrastructure.user.in.response.GetAllUsersResponse;
import com.attrigo.asapp.authentication.infrastructure.user.in.response.GetUserByIdResponse;
import com.attrigo.asapp.authentication.infrastructure.user.in.response.UpdateUserResponse;
import com.attrigo.asapp.authentication.testutil.RestDocsConstrainedFields;
import com.attrigo.asapp.authentication.testutil.RestDocsWebMvcTestContext;

/**
 * Tests {@link UserApi} contract documentation.
 * <p>
 * Setup:
 * <li>Loads the web layer with a mock MVC environment and mocked service collaborators</li>
 * <li>Configures REST Docs documentation support before each test</li>
 * <p>
 * Coverage:
 * <li>Generates API documentation snippets for all user endpoints and error responses</li>
 * <li>Documents path parameters, request fields, and response fields</li>
 * <li>Covers successful request and response flows for each HTTP operation</li>
 * <li>Covers error responses for validation failures, unauthorized access, not found, server errors, and service unavailability</li>
 */
@WithMockUser
class UserApiDocumentationIT extends RestDocsWebMvcTestContext {

    @Nested
    class GetUserById {

        @Test
        void DocumentsGetUserById_UserFound() throws Exception {
            // Given
            var user = anActiveUser();
            var userIdValue = user.getId()
                                  .value();
            var usernameValue = user.getUsername()
                                    .value();
            var roleName = user.getRole()
                               .name();
            var response = new GetUserByIdResponse(userIdValue, usernameValue, "***", roleName);

            given(readUserUseCase.getUserById(any(UUID.class))).willReturn(Optional.of(user));
            given(userMapperMock.toGetUserByIdResponse(any(User.class))).willReturn(response);

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
                                           fieldWithPath("userId").description("The user's unique identifier"),
                                           fieldWithPath("username").description("The user's username in email format"),
                                           fieldWithPath("password").description("The user's masked password"),
                                           fieldWithPath("role").description("The user's role")
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
            var user = anActiveUser();
            var userIdValue = user.getId()
                                  .value();
            var usernameValue = user.getUsername()
                                    .value();
            var roleName = user.getRole()
                               .name();
            var response = new GetAllUsersResponse(userIdValue, usernameValue, "***", roleName);

            given(readUserUseCase.getAllUsers()).willReturn(List.of(user));
            given(userMapperMock.toGetAllUsersResponse(any(User.class))).willReturn(response);

            // When & Then
            mockMvc.perform(get(USERS_GET_ALL_FULL_PATH).accept(APPLICATION_JSON)
                                                        .header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                           document("get-all-users",
                                   requestHeaders(headerWithName("Authorization").description("Bearer JWT access token")),
                                   responseFields(
                                           fieldWithPath("[].userId").description("The user's unique identifier"),
                                           fieldWithPath("[].username").description("The user's username in email format"),
                                           fieldWithPath("[].password").description("The user's masked password"),
                                           fieldWithPath("[].role").description("The user's role")
                                   )
                           )
                   // @formatter:on
                   );
        }

    }

    @WithAnonymousUser
    @Nested
    class CreateUser {

        @Test
        void DocumentsCreateUser() throws Exception {
            // Given
            var fields = new RestDocsConstrainedFields(CreateUserRequest.class);
            var user = anActiveUser();
            var userIdValue = user.getId()
                                  .value();
            var usernameValue = user.getUsername()
                                    .value();
            var passwordValue = "TEST@09_password?!";
            var roleName = user.getRole()
                               .name();
            var requestBody = """
                    {
                        "username": "%s",
                        "password": "%s",
                        "role": "%s"
                    }
                    """.formatted(usernameValue, passwordValue, roleName);
            var createUserCommand = new CreateUserCommand(usernameValue, passwordValue, roleName);
            var response = new CreateUserResponse(userIdValue);

            given(userMapperMock.toCreateUserCommand(any(CreateUserRequest.class))).willReturn(createUserCommand);
            given(createUserUseCase.createUser(any(CreateUserCommand.class))).willReturn(user);
            given(userMapperMock.toCreateUserResponse(any(User.class))).willReturn(response);

            // When & Then
            mockMvc.perform(post(USERS_CREATE_FULL_PATH).contentType(APPLICATION_JSON)
                                                        .content(requestBody))
                   .andExpect(status().isCreated())
                   .andDo(
                   // @formatter:off
                           document("create-user",
                                   requestFields(
                                           fields.withPath("username").description("The user's username in email format"),
                                           fields.withPath("password").description("The user's raw password"),
                                           fields.withPath("role").description("The user's role")
                                   ),
                                   responseFields(fieldWithPath("userId").description("The created user's unique identifier"))
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
            var user = anActiveUser();
            var userIdValue = user.getId()
                                  .value();
            var usernameValue = user.getUsername()
                                    .value();
            var passwordValue = "TEST@09_password?!";
            var roleName = user.getRole()
                               .name();
            var requestBody = """
                    {
                        "username": "%s",
                        "password": "%s",
                        "role": "%s"
                    }
                    """.formatted(usernameValue, passwordValue, roleName);
            var updateUserCommand = new UpdateUserCommand(userIdValue, usernameValue, passwordValue, roleName);
            var response = new UpdateUserResponse(userIdValue);

            given(userMapperMock.toUpdateUserCommand(any(UUID.class), any(UpdateUserRequest.class))).willReturn(updateUserCommand);
            given(updateUserUseCase.updateUserById(any(UpdateUserCommand.class))).willReturn(Optional.of(user));
            given(userMapperMock.toUpdateUserResponse(any(User.class))).willReturn(response);

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
                                           fields.withPath("username").description("The user's username in email format"),
                                           fields.withPath("password").description("The user's new raw password"),
                                           fields.withPath("role").description("The user's role")
                                   ),
                                   responseFields(fieldWithPath("userId").description("The updated user's unique identifier"))
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
            var user = anActiveUser();
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
        void DocumentsPathVariableValidationFailure() throws Exception {
            // When & Then
            mockMvc.perform(get(USERS_GET_BY_ID_FULL_PATH, "not-a-uuid").accept(APPLICATION_JSON))
                   .andExpect(status().isBadRequest())
                   .andDo(
                   // @formatter:off
                           document("error-path-variable-validation-failure",
                                   relaxedResponseFields(
                                           fieldWithPath("title").description("Short summary of the problem type"),
                                           fieldWithPath("status").description("HTTP status code"),
                                           fieldWithPath("detail").description("Human-readable explanation of the problem")
                                   )
                           )
                   // @formatter:on
                   );
        }

        @WithAnonymousUser
        @Test
        void DocumentsRequestBodyValidationFailure() throws Exception {
            // When & Then
            mockMvc.perform(post(USERS_CREATE_FULL_PATH).contentType(APPLICATION_JSON)
                                                        .content("{}"))
                   .andExpect(status().isBadRequest())
                   .andDo(
                   // @formatter:off
                           document("error-request-body-validation-failure",
                                   relaxedResponseFields(
                                           fieldWithPath("title").description("Short summary of the problem type"),
                                           fieldWithPath("status").description("HTTP status code"),
                                           fieldWithPath("detail").description("Human-readable explanation of the problem"),
                                           fieldWithPath("error").description("Machine-readable error code"),
                                           fieldWithPath("fieldErrors").description("List of validation errors"),
                                           fieldWithPath("fieldErrors[].field").description("Field that failed validation"),
                                           fieldWithPath("fieldErrors[].message").description("Validation error message")
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
                   .andDo(
                   // @formatter:off
                           document("error-unauthorized",
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

        @Test
        void DocumentsServiceUnavailable() throws Exception {
            // Given
            var userIdValue = UUID.fromString("00000000-0000-0000-0000-000000000001");

            given(deleteUserUseCase.deleteUserById(any(UUID.class))).willThrow(new TokenStoreException("Token store unavailable", new RuntimeException()));

            // When & Then
            mockMvc.perform(delete(USERS_DELETE_BY_ID_FULL_PATH, userIdValue).header(AUTHORIZATION, "Bearer sample.access.token"))
                   .andExpect(status().isServiceUnavailable())
                   .andDo(
                   // @formatter:off
                           document("error-service-unavailable",
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
