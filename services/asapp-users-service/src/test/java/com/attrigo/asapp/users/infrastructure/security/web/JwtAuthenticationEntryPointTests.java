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

package com.attrigo.asapp.users.infrastructure.security.web;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import tools.jackson.databind.json.JsonMapper;

/**
 * Tests {@link JwtAuthenticationEntryPoint} rendering of a RFC 7807 401 response on authentication failure, distinguishing a missing bearer token from a
 * present-but-invalid one.
 * <p>
 * Coverage:
 * <li>Renders a bare Bearer challenge without an error code when no bearer token is supplied</li>
 * <li>Renders a Bearer challenge carrying the invalid_token error code when a bearer token is present but invalid</li>
 */
class JwtAuthenticationEntryPointTests {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint = new JwtAuthenticationEntryPoint(JsonMapper.builder()
                                                                                                                      .addMixIn(ProblemDetail.class,
                                                                                                                              ProblemDetailJacksonMixin.class)
                                                                                                                      .build());

    @Nested
    class Commence {

        @Test
        void ReturnsUnauthorizedAndProblemDetailWithBareChallenge_MissingAuthorizationHeader() throws Exception {
            // Given
            var request = new MockHttpServletRequest("GET", "/api/users");
            var response = new MockHttpServletResponse();
            var authenticationException = new InsufficientAuthenticationException("Full authentication is required");

            // When
            jwtAuthenticationEntryPoint.commence(request, response, authenticationException);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(response.getStatus()).as("status").isEqualTo(HttpStatus.UNAUTHORIZED.value());
                softly.assertThat(response.getContentType()).as("content type").startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                softly.assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate header").isEqualTo("Bearer");
                // @formatter:on
            });
            assertThatJson(response.getContentAsString()).isObject()
                                                         .containsEntry("title", "Authentication Failed")
                                                         .containsEntry("status", 401)
                                                         .containsEntry("detail", "Invalid credentials")
                                                         .doesNotContainKey("error");
        }

        @Test
        void ReturnsUnauthorizedAndProblemDetailWithBareChallenge_BearerHeaderWithoutToken() throws Exception {
            // Given
            var request = new MockHttpServletRequest("GET", "/api/users");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer");
            var response = new MockHttpServletResponse();
            var authenticationException = new InsufficientAuthenticationException("Full authentication is required");

            // When
            jwtAuthenticationEntryPoint.commence(request, response, authenticationException);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(response.getStatus()).as("status").isEqualTo(HttpStatus.UNAUTHORIZED.value());
                softly.assertThat(response.getContentType()).as("content type").startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                softly.assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate header").isEqualTo("Bearer");
                // @formatter:on
            });
            assertThatJson(response.getContentAsString()).isObject()
                                                         .containsEntry("title", "Authentication Failed")
                                                         .containsEntry("status", 401)
                                                         .containsEntry("detail", "Invalid credentials")
                                                         .doesNotContainKey("error");
        }

        @Test
        void ReturnsUnauthorizedAndProblemDetailWithErrorCodeChallenge_InvalidToken() throws Exception {
            // Given
            var request = new MockHttpServletRequest("GET", "/api/users");
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid_bearer_token");
            var response = new MockHttpServletResponse();
            var authenticationException = new InsufficientAuthenticationException("Full authentication is required");

            // When
            jwtAuthenticationEntryPoint.commence(request, response, authenticationException);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(response.getStatus()).as("status").isEqualTo(HttpStatus.UNAUTHORIZED.value());
                softly.assertThat(response.getContentType()).as("content type").startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                softly.assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate header").isEqualTo("Bearer error=\"invalid_token\"");
                // @formatter:on
            });
            assertThatJson(response.getContentAsString()).isObject()
                                                         .containsEntry("title", "Authentication Failed")
                                                         .containsEntry("status", 401)
                                                         .containsEntry("detail", "Invalid credentials")
                                                         .containsEntry("error", "invalid_token");
        }

    }

}
