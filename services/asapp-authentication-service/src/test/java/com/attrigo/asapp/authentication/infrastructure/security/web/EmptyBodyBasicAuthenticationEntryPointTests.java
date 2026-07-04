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

package com.attrigo.asapp.authentication.infrastructure.security.web;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

/**
 * Tests {@link EmptyBodyBasicAuthenticationEntryPoint} rendering of a 401 response with a Basic authentication challenge on authentication failure.
 */
class EmptyBodyBasicAuthenticationEntryPointTests {

    private final EmptyBodyBasicAuthenticationEntryPoint emptyBodyBasicAuthenticationEntryPoint = new EmptyBodyBasicAuthenticationEntryPoint();

    @Nested
    class Commence {

        @Test
        void ReturnsUnauthorizedWithBasicChallengeEmptyBody_AuthenticationFails() throws Exception {
            // Given
            var request = new MockHttpServletRequest("GET", "/actuator/beans");
            var response = new MockHttpServletResponse();
            var authenticationException = new InsufficientAuthenticationException("Full authentication is required");

            // When
            emptyBodyBasicAuthenticationEntryPoint.commence(request, response, authenticationException);

            // Then
            var responseBody = response.getContentAsString();
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(response.getStatus()).as("status").isEqualTo(HttpStatus.UNAUTHORIZED.value());
                softly.assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).as("WWW-Authenticate header").isEqualTo("Basic realm=\"actuator\"");
                softly.assertThat(responseBody).as("response body").isBlank();
                // @formatter:on
            });
        }

    }

}
