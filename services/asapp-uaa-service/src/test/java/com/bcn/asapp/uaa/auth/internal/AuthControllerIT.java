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
package com.bcn.asapp.uaa.auth.internal;

import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_REFRESH_TOKEN_FULL_PATH;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_REVOKE_FULL_PATH;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_TOKEN_FULL_PATH;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.bcn.asapp.uaa.auth.AccessTokenDTO;
import com.bcn.asapp.uaa.auth.AuthService;
import com.bcn.asapp.uaa.auth.JwtAuthenticationDTO;
import com.bcn.asapp.uaa.auth.RefreshTokenDTO;
import com.bcn.asapp.uaa.auth.UserCredentialsDTO;
import com.bcn.asapp.uaa.config.SecurityConfiguration;
import com.bcn.asapp.uaa.security.authentication.verifier.JwtVerifier;
import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.security.web.JwtAuthenticationEntryPoint;
import com.bcn.asapp.uaa.security.web.JwtAuthenticationFilter;
import com.bcn.asapp.uaa.testutil.JwtFaker;

@Import(value = { SecurityConfiguration.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class })
@WebMvcTest(AuthRestController.class)
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ObjectMapper objectMapperIncludeAlways;

    @MockitoBean
    private JwtVerifier jwtVerifierMock;

    @MockitoBean
    private AuthService authServiceMock;

    private JwtFaker jwtFaker;

    private String fakeUsername;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.objectMapperIncludeAlways.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        this.jwtFaker = new JwtFaker();

        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
    }

    @Nested
    @WithAnonymousUser
    class Authenticate {

        @Test
        @DisplayName("GIVEN user credentials fields are not a valid Json WHEN authenticate a user THEN returns HTTP response with status Unsupported Media Type And the empty body with the problem details")
        void UserCredentialsFieldsAreNotJson_Authenticate_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userCredentialsToAuthenticate = "";

            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                           .content(userCredentialsToAuthenticate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
                   .andExpect(jsonPath("$.status", is(415)))
                   .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/token")));
        }

        @Test
        @DisplayName("GIVEN user credentials fields are not present WHEN authenticate a user THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserCredentialsFieldsAreNotPresent_Authenticate_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userCredentialsToAuthenticate = "";

            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(userCredentialsToAuthenticate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/token")));
        }

        @Test
        @DisplayName("GIVEN user credentials mandatory fields are not present WHEN authenticate a user THEN returns HTTP response with status BAD_REQUEST And the empty body with the problem details")
        void UserCredentialsMandatoryFieldsAreNotPresent_Authenticate_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userCredentialsToAuthenticate = new UserCredentialsDTO(null, null);
            var userCredentialsToAuthenticateAsJson = objectMapper.writeValueAsString(userCredentialsToAuthenticate);

            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(userCredentialsToAuthenticateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The username must not be empty")))
                   .andExpect(jsonPath("$.detail", containsString("The password must not be empty")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/token")))
                   .andExpect(jsonPath("$.errors", hasSize(2)))
                   .andExpect(jsonPath(
                           "$.errors[?(@.entity == 'userCredentialsDTO' && @.field == 'username' && @.message == 'The username must not be empty')]").exists())
                   .andExpect(jsonPath(
                           "$.errors[?(@.entity == 'userCredentialsDTO' && @.field == 'password' && @.message == 'The password must not be empty')]").exists());
        }

        @Test
        @DisplayName("GIVEN user credentials mandatory fields are empty WHEN authenticate a user THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserCredentialsFieldsAreEmpty_Authenticate_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userCredentialsToAuthenticate = new UserCredentialsDTO("", "");
            var userCredentialsToAuthenticateAsJson = objectMapperIncludeAlways.writeValueAsString(userCredentialsToAuthenticate);

            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(userCredentialsToAuthenticateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The username must not be empty")))
                   .andExpect(jsonPath("$.detail", containsString("The password must not be empty")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/token")))
                   .andExpect(jsonPath("$.errors", hasSize(2)))
                   .andExpect(jsonPath(
                           "$.errors[?(@.entity == 'userCredentialsDTO' && @.field == 'username' && @.message == 'The username must not be empty')]").exists())
                   .andExpect(jsonPath(
                           "$.errors[?(@.entity == 'userCredentialsDTO' && @.field == 'password' && @.message == 'The password must not be empty')]").exists());
        }

        @Test
        @DisplayName("GIVEN user credentials fields are valid WHEN authenticate a user THEN returns HTTP response with status OK And the body with the generated authentication")
        void UserCredentialsFieldsAreValid_Authenticate_ReturnsStatusOkAndBodyWithGeneratedAuthentication() throws Exception {
            // Given
            var fakeAccessToken = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeRefreshToken = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeAuthentication = new JwtAuthenticationDTO(new AccessTokenDTO(fakeAccessToken), new RefreshTokenDTO(fakeRefreshToken));
            given(authServiceMock.authenticate(any(UserCredentialsDTO.class))).willReturn(fakeAuthentication);

            // When & Then
            var userCredentialsToAuthenticate = new UserCredentialsDTO(fakeUsername, fakePassword);
            var userCredentialsToAuthenticateAsJson = objectMapper.writeValueAsString(userCredentialsToAuthenticate);

            var requestBuilder = post(AUTH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(userCredentialsToAuthenticateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.access_token", is(fakeAccessToken)))
                   .andExpect(jsonPath("$.refresh_token", is(fakeRefreshToken)));
        }

    }

    @Nested
    @WithAnonymousUser
    class RefreshAuthentication {

        @Test
        @DisplayName("GIVEN refresh token is not a valid Json WHEN refresh an authentication THEN returns HTTP response with status Unsupported Media Type And the empty body with the problem details")
        void RefreshTokenIsNotJson_RefreshAuthentication_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var refreshTokenToRefresh = "";

            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                                   .content(refreshTokenToRefresh);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
                   .andExpect(jsonPath("$.status", is(415)))
                   .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/refresh-token")));
        }

        @Test
        @DisplayName("GIVEN refresh token is not present WHEN refresh an authentication THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void RefreshTokenIsNotPresent_RefreshAuthentication_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var refreshTokenToRefresh = "";

            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(refreshTokenToRefresh);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/refresh-token")));
        }

        @Test
        @DisplayName("GIVEN refresh token is valid WHEN refresh an authentication THEN returns HTTP response with status OK And the body with the new authentication")
        void RefreshTokenIsValid_RefreshAuthentication_ReturnsStatusOkAndBodyWithNewAuthentication() throws Exception {
            // Given
            var fakeAccessToken = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeRefreshToken = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeAuthentication = new JwtAuthenticationDTO(new AccessTokenDTO(fakeAccessToken), new RefreshTokenDTO(fakeRefreshToken));
            given(authServiceMock.refreshAuthentication(any(RefreshTokenDTO.class))).willReturn(fakeAuthentication);

            // When & Then
            var refreshToken = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var refreshTokenToRefresh = new RefreshTokenDTO(refreshToken);
            var refreshTokenToRefreshAsJson = objectMapper.writeValueAsString(refreshTokenToRefresh);

            var requestBuilder = post(AUTH_REFRESH_TOKEN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                   .content(refreshTokenToRefreshAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.access_token", is(fakeAccessToken)))
                   .andExpect(jsonPath("$.refresh_token", is(fakeRefreshToken)));
        }

    }

    @Nested
    @WithAnonymousUser
    class RevokeAuthentication {

        @Test
        @DisplayName("GIVEN access token is not a valid Json WHEN revoke an authentication THEN returns HTTP response with status Unsupported Media Type And the empty body with the problem details")
        void AccessTokenIsNotJson_RevokeAuthentication_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var accessTokenToRevoke = "";

            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                            .content(accessTokenToRevoke);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
                   .andExpect(jsonPath("$.status", is(415)))
                   .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/revoke")));
        }

        @Test
        @DisplayName("GIVEN access token is not present WHEN revoke an authentication THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void AccessTokenIsNotPresent_RevokeAuthentication_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var accessTokenToRevoke = "";

            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                            .content(accessTokenToRevoke);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/revoke")));
        }

        @Test
        @DisplayName("GIVEN access token is valid WHEN revoke an authentication THEN returns HTTP response with status OK And an empty body")
        void RefreshTokenIsValid_RevokeAuthentication_ReturnsStatusOkAndEmptyBody() throws Exception {
            // When & Then
            var accessToken = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var accessTokenToRevoke = new RefreshTokenDTO(accessToken);
            var accessTokenToRevokeAsJson = objectMapper.writeValueAsString(accessTokenToRevoke);

            var requestBuilder = post(AUTH_REVOKE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                            .content(accessTokenToRevokeAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$").doesNotExist());
        }

    }

}
