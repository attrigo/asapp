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

package com.bcn.asapp.authentication.infrastructure.authentication.in;

import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationMother.anAuthenticatedJwtAuthentication;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REFRESH_TOKEN_FULL_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REVOKE_FULL_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_TOKEN_FULL_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;

import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.in.command.AuthenticateCommand;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.AuthenticateRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.RefreshAuthenticationRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.RevokeAuthenticationRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.AuthenticateResponse;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.RefreshAuthenticationResponse;
import com.bcn.asapp.authentication.testutil.RestDocsConstrainedFields;
import com.bcn.asapp.authentication.testutil.RestDocsWebMvcTestContext;

/**
 * Tests {@link AuthenticationRestController} REST API documentation.
 * <p>
 * Coverage:
 * <li>Generates API documentation snippets for all authentication endpoints and error responses</li>
 * <li>Documents request fields and response fields</li>
 * <li>Covers successful request and response flows for each HTTP operation</li>
 * <li>Covers error responses for validation failures, invalid credentials, server errors, and service unavailability</li>
 */
class AuthenticationRestControllerDocumentationIT extends RestDocsWebMvcTestContext {

    @Nested
    class Authenticate {

        @Test
        void DocumentsAuthenticate() throws Exception {
            // Given
            var fields = new RestDocsConstrainedFields(AuthenticateRequest.class);
            var jwtAuthentication = anAuthenticatedJwtAuthentication();
            var username = "user@asapp.com";
            var password = "TEST@09_password?!";
            var requestBody = """
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                    """.formatted(username, password);
            var authenticateCommand = new AuthenticateCommand(username, password);
            var response = new AuthenticateResponse("sample.access.token", "sample.refresh.token");

            given(jwtAuthenticationMapper.toAuthenticateCommand(any(AuthenticateRequest.class))).willReturn(authenticateCommand);
            given(authenticateUseCase.authenticate(any(AuthenticateCommand.class))).willReturn(jwtAuthentication);
            given(jwtAuthenticationMapper.toAuthenticateResponse(any(JwtAuthentication.class))).willReturn(response);

            // When & Then
            mockMvc.perform(post(AUTH_TOKEN_FULL_PATH).contentType(APPLICATION_JSON)
                                                      .content(requestBody))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("authenticate",
                           requestFields(
                                   fields.withPath("username").description("The user's username in email format"),
                                   fields.withPath("password").description("The user's raw password")
                           ),
                           responseFields(
                                   fieldWithPath("access_token").description("The generated access token"),
                                   fieldWithPath("refresh_token").description("The generated refresh token")
                           )
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class RefreshAuthentication {

        @Test
        void DocumentsRefreshAuthentication() throws Exception {
            // Given
            var fields = new RestDocsConstrainedFields(RefreshAuthenticationRequest.class);
            var jwtAuthentication = anAuthenticatedJwtAuthentication();
            var encodedToken = jwtAuthentication.refreshToken();
            var requestBody = """
                    {
                        "refresh_token": "%s"
                    }
                    """.formatted(encodedToken.encodedTokenValue());
            var response = new RefreshAuthenticationResponse("sample.access.token", "sample.refresh.token");

            given(refreshAuthenticationUseCase.refreshAuthentication(anyString())).willReturn(jwtAuthentication);
            given(jwtAuthenticationMapper.toRefreshAuthenticationResponse(any(JwtAuthentication.class))).willReturn(response);

            // When & Then
            mockMvc.perform(post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(APPLICATION_JSON)
                                                              .content(requestBody))
                   .andExpect(status().isOk())
                   .andDo(
                   // @formatter:off
                       document("refresh-authentication",
                           requestFields(
                                   fields.withPath("refresh_token", "refreshToken").description("The refresh token to renew the session")
                           ),
                           responseFields(
                                   fieldWithPath("access_token").description("The new access token"),
                                   fieldWithPath("refresh_token").description("The new refresh token")
                           )
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class RevokeAuthentication {

        @Test
        void DocumentsRevokeAuthentication() throws Exception {
            // Given
            var fields = new RestDocsConstrainedFields(RevokeAuthenticationRequest.class);
            var jwtAuthentication = anAuthenticatedJwtAuthentication();
            var encodedToken = jwtAuthentication.accessToken();
            var requestBody = """
                    {
                        "access_token": "%s"
                    }
                    """.formatted(encodedToken.encodedTokenValue());

            willDoNothing().given(revokeAuthenticationUseCase)
                           .revokeAuthentication(anyString());

            // When & Then
            mockMvc.perform(post(AUTH_REVOKE_FULL_PATH).contentType(APPLICATION_JSON)
                                                       .content(requestBody))
                   .andExpect(status().isNoContent())
                   .andDo(
                   // @formatter:off
                       document("revoke-authentication",
                           requestFields(
                                   fields.withPath("access_token", "accessToken").description("The access token identifying the session to revoke")
                           )
                       )
                       // @formatter:on
                   );
        }

    }

    @Nested
    class Errors {

        @Test
        void DocumentsValidationFailure() throws Exception {
            // When & Then
            mockMvc.perform(post(AUTH_TOKEN_FULL_PATH).contentType(APPLICATION_JSON)
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

        @Test
        void DocumentsInvalidCredentials() throws Exception {
            // Given
            var requestBody = """
                    {
                        "username": "user@asapp.com",
                        "password": "wrong-password"
                    }
                    """;

            given(jwtAuthenticationMapper.toAuthenticateCommand(any(AuthenticateRequest.class))).willReturn(
                    new AuthenticateCommand("user@asapp.com", "wrong-password"));
            given(authenticateUseCase.authenticate(any(AuthenticateCommand.class))).willThrow(new AuthenticationNotFoundException("Authentication not found"));

            // When & Then
            mockMvc.perform(post(AUTH_TOKEN_FULL_PATH).contentType(APPLICATION_JSON)
                                                      .content(requestBody))
                   .andExpect(status().isUnauthorized())
                   .andDo(
                   // @formatter:off
                       document("error-invalid-credentials",
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
        void DocumentsInternalServerError() throws Exception {
            // Given
            var requestBody = """
                    {
                        "username": "user@asapp.com",
                        "password": "TEST@09_password?!"
                    }
                    """;

            given(jwtAuthenticationMapper.toAuthenticateCommand(any(AuthenticateRequest.class))).willReturn(
                    new AuthenticateCommand("user@asapp.com", "TEST@09_password?!"));
            given(authenticateUseCase.authenticate(any(AuthenticateCommand.class))).willThrow(new DataRetrievalFailureException("Database error"));

            // When & Then
            mockMvc.perform(post(AUTH_TOKEN_FULL_PATH).contentType(APPLICATION_JSON)
                                                      .content(requestBody))
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
            var requestBody = """
                    {
                        "username": "user@asapp.com",
                        "password": "TEST@09_password?!"
                    }
                    """;

            given(jwtAuthenticationMapper.toAuthenticateCommand(any(AuthenticateRequest.class))).willReturn(
                    new AuthenticateCommand("user@asapp.com", "TEST@09_password?!"));
            given(authenticateUseCase.authenticate(any(AuthenticateCommand.class))).willThrow(
                    new TokenStoreException("Token store unavailable", new RuntimeException()));

            // When & Then
            mockMvc.perform(post(AUTH_TOKEN_FULL_PATH).contentType(APPLICATION_JSON)
                                                      .content(requestBody))
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
