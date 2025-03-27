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

import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_LOGIN_FULL_PATH;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.bcn.asapp.uaa.auth.AuthService;
import com.bcn.asapp.uaa.auth.AuthenticationDTO;
import com.bcn.asapp.uaa.auth.UserCredentialsDTO;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthRestController.class)
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ObjectMapper objectMapperIncludeAlways;

    @MockitoBean
    private AuthService authServiceMock;

    private String fakeUsername;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        objectMapperIncludeAlways.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        this.fakeUsername = "IT username";
        this.fakePassword = "IT password";
    }

    @Nested
    class Login {

        @Test
        @DisplayName("GIVEN user credentials are not a valid Json WHEN login a user THEN returns HTTP response with status Unsupported Media Type And the empty body with the problem details")
        void UserCredentialsAreNotJson_Login_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userCredentialsToLogin = "";

            var requestBuilder = post(AUTH_LOGIN_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                           .content(userCredentialsToLogin);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
                   .andExpect(jsonPath("$.status", is(415)))
                   .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/login")));
        }

        @Test
        @DisplayName("GIVEN user credentials mandatory fields are not present WHEN login a user THEN returns HTTP response with status BAD_REQUEST And the empty body with the problem details")
        void UserCredentialsMandatoryFieldsAreNotPresent_Login_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(null, null);
            var userCredentialsToLoginAsJson = objectMapper.writeValueAsString(userCredentialsToLogin);

            var requestBuilder = post(AUTH_LOGIN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(userCredentialsToLoginAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The username is mandatory")))
                   .andExpect(jsonPath("$.detail", containsString("The password is mandatory")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/login")))
                   .andExpect(jsonPath("$.errors", hasSize(2)))
                   .andExpect(jsonPath(
                           "$.errors[?(@.entity == 'userCredentialsDTO' && @.field == 'username' && @.message == 'The username is mandatory')]").exists())
                   .andExpect(jsonPath(
                           "$.errors[?(@.entity == 'userCredentialsDTO' && @.field == 'password' && @.message == 'The password is mandatory')]").exists());
        }

        @Test
        @DisplayName("GIVEN user credentials mandatory fields are empty WHEN login a user THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void UserCredentialsFieldsAreEmpty_Login_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO("", "");
            var userCredentialsToLoginAsJson = objectMapperIncludeAlways.writeValueAsString(userCredentialsToLogin);

            var requestBuilder = post(AUTH_LOGIN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(userCredentialsToLoginAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The username is mandatory")))
                   .andExpect(jsonPath("$.detail", containsString("The password is mandatory")))
                   .andExpect(jsonPath("$.instance", is("/v1/auth/login")))
                   .andExpect(jsonPath("$.errors", hasSize(2)))
                   .andExpect(jsonPath(
                           "$.errors[?(@.entity == 'userCredentialsDTO' && @.field == 'username' && @.message == 'The username is mandatory')]").exists())
                   .andExpect(jsonPath(
                           "$.errors[?(@.entity == 'userCredentialsDTO' && @.field == 'password' && @.message == 'The password is mandatory')]").exists());
        }

        @Test
        @DisplayName("GIVEN user credentials are valid WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
        void UserCredentialsAreValid_Login_ReturnsStatusCreatedAndBodyWithGeneratedAuthentication() throws Exception {
            // Given
            given(authServiceMock.login(any(UserCredentialsDTO.class))).willReturn(new AuthenticationDTO("IT Token"));

            // When & Then
            var userCredentialsToLogin = new UserCredentialsDTO(fakeUsername, fakePassword);
            var userCredentialsToLoginAsJson = objectMapper.writeValueAsString(userCredentialsToLogin);

            var requestBuilder = post(AUTH_LOGIN_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                           .content(userCredentialsToLoginAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.jwt").exists());
        }

    }

}
