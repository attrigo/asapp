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

package com.bcn.asapp.authentication.infrastructure.user.in;

import static com.bcn.asapp.authentication.testutil.fixture.UserMother.anActiveUser;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.authentication.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
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
import org.springframework.security.test.context.support.WithMockUser;

import com.bcn.asapp.authentication.application.user.in.command.CreateUserCommand;
import com.bcn.asapp.authentication.application.user.in.command.UpdateUserCommand;
import com.bcn.asapp.authentication.domain.user.User;
import com.bcn.asapp.authentication.infrastructure.user.in.request.CreateUserRequest;
import com.bcn.asapp.authentication.infrastructure.user.in.request.UpdateUserRequest;
import com.bcn.asapp.authentication.infrastructure.user.in.response.CreateUserResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.GetAllUsersResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.GetUserByIdResponse;
import com.bcn.asapp.authentication.infrastructure.user.in.response.UpdateUserResponse;
import com.bcn.asapp.authentication.testutil.RestDocsWebMvcTestContext;

/**
 * Tests {@link UserRestController} REST API documentation.
 * <p>
 * Coverage:
 * <li>Generates API documentation snippets for all user endpoints</li>
 * <li>Documents path parameters, request fields, and response fields</li>
 * <li>Covers successful request and response flows for each HTTP operation</li>
 */
@WithMockUser
class UserRestControllerDocIT extends RestDocsWebMvcTestContext {

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
            mockMvc.perform(get(USERS_GET_BY_ID_FULL_PATH, userIdValue).accept(APPLICATION_JSON))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-user-by-id",
                           pathParameters(parameterWithName("id").description("The user's unique identifier")),
                           responseFields(
                                   fieldWithPath("user_id").description("The user's unique identifier"),
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
            mockMvc.perform(get(USERS_GET_ALL_FULL_PATH).accept(APPLICATION_JSON))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("get-all-users",
                           responseFields(
                                   fieldWithPath("[].user_id").description("The user's unique identifier"),
                                   fieldWithPath("[].username").description("The user's username in email format"),
                                   fieldWithPath("[].password").description("The user's masked password"),
                                   fieldWithPath("[].role").description("The user's role")
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
            var user = anActiveUser();
            var userIdValue = user.getId()
                                  .value();
            var usernameValue = user.getUsername()
                                    .value();
            var passwordValue = "***";
            var roleName = user.getRole()
                               .name();
            var createUserCommand = new CreateUserCommand(usernameValue, passwordValue, roleName);
            var requestBody = """
                    {
                        "username": "%s",
                        "password": "%s",
                        "role": "%s"
                    }
                    """.formatted(userIdValue, passwordValue, roleName);
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
                                   fieldWithPath("username").description("The user's username in email format"),
                                   fieldWithPath("password").description("The user's raw password"),
                                   fieldWithPath("role").description("The user's role")
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
            var user = anActiveUser();
            var userIdValue = user.getId()
                                  .value();
            var usernameValue = user.getUsername()
                                    .value();
            var passwordValue = "***";
            var roleName = user.getRole()
                               .name();
            var updateUserCommand = new UpdateUserCommand(userIdValue, usernameValue, passwordValue, roleName);
            var requestBody = """
                    {
                        "username": "%s",
                        "password": "%s",
                        "role": "%s"
                    }
                    """.formatted(usernameValue, passwordValue, roleName);
            var response = new UpdateUserResponse(userIdValue);

            given(userMapperMock.toUpdateUserCommand(any(UUID.class), any(UpdateUserRequest.class))).willReturn(updateUserCommand);
            given(updateUserUseCase.updateUserById(any(UpdateUserCommand.class))).willReturn(Optional.of(user));
            given(userMapperMock.toUpdateUserResponse(any(User.class))).willReturn(response);

            // When & Then
            mockMvc.perform(put(USERS_UPDATE_BY_ID_FULL_PATH, userIdValue).contentType(APPLICATION_JSON)
                                                                          .content(requestBody))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("update-user-by-id",
                           pathParameters(parameterWithName("id").description("The user's unique identifier")),
                           requestFields(
                                   fieldWithPath("username").description("The user's username in email format"),
                                   fieldWithPath("password").description("The user's new raw password"),
                                   fieldWithPath("role").description("The user's role")
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
            var user = anActiveUser();
            var userIdValue = user.getId()
                                  .value();

            given(deleteUserUseCase.deleteUserById(any(UUID.class))).willReturn(true);

            // When & Then
            mockMvc.perform(delete(USERS_DELETE_BY_ID_FULL_PATH, userIdValue))
                   .andExpect(status().isNoContent())
                   .andDo(
                   // @formatter:off
                       document("delete-user-by-id",
                       pathParameters(parameterWithName("id").description("The user's unique identifier")))
                       // @formatter:on
                   );
        }

    }

}
