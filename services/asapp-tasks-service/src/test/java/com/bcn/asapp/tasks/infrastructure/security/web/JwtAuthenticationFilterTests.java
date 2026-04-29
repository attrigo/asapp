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

package com.bcn.asapp.tasks.infrastructure.security.web;

import static com.bcn.asapp.tasks.testutil.fixture.DecodedJwtMother.decodedAccessToken;
import static com.bcn.asapp.tasks.testutil.fixture.EncodedTokenMother.anEncodedTokenBuilder;
import static com.bcn.asapp.tasks.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static com.bcn.asapp.tasks.testutil.fixture.EncodedTokenMother.encodedRefreshToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.ServletRequestPathUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bcn.asapp.tasks.infrastructure.security.AuthenticationNotFoundException;
import com.bcn.asapp.tasks.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.tasks.infrastructure.security.JwtAuthenticationToken;
import com.bcn.asapp.tasks.infrastructure.security.JwtVerifier;
import com.bcn.asapp.tasks.infrastructure.security.UnexpectedJwtTypeException;

/**
 * Tests {@link JwtAuthenticationFilter} request exclusion and JWT authentication processing.
 * <p>
 * Coverage:
 * <li>Skips filter for whitelisted URL patterns</li>
 * <li>Applies filter to protected URL patterns</li>
 * <li>Continues filter chain without authentication when bearer token is missing</li>
 * <li>Continues filter chain without authentication when authorization header is not a bearer scheme</li>
 * <li>Continues filter chain without authentication when token type is not access</li>
 * <li>Continues filter chain without authentication when authentication session is not found</li>
 * <li>Continues filter chain without authentication when token is expired</li>
 * <li>Continues filter chain without authentication when an unexpected verification error occurs</li>
 * <li>Sets authenticated security context and continues filter chain for a valid bearer token</li>
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    @Mock
    private JwtVerifier jwtVerifier;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class ShouldNotFilter {

        @ParameterizedTest
        @MethodSource("whitelistedUrls")
        void ReturnsTrue_WhitelistedUrl(String url) {
            // Given
            var request = new MockHttpServletRequest("GET", url);
            ServletRequestPathUtils.parseAndCache(request);

            // When
            var actual = filter.shouldNotFilter(request);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_ProtectedUrl() {
            // Given
            var request = new MockHttpServletRequest("GET", "/api/tasks");
            ServletRequestPathUtils.parseAndCache(request);

            // When
            var actual = filter.shouldNotFilter(request);

            // Then
            assertThat(actual).isFalse();
        }

        private static Stream<String> whitelistedUrls() {
            return Stream.of("/livez", "/readyz", "/swagger-ui/index.html", "/swagger-ui.html", "/v3/api-docs/openapi.yaml");
        }

    }

    @Nested
    class DoFilterInternal {

        @Test
        void SetsAuthenticationAndContinuesFilterChain_ValidBearerToken() throws Exception {
            // Given
            var decodedJwt = decodedAccessToken();

            given(request.getHeader("Authorization")).willReturn("Bearer " + decodedJwt.encodedToken());
            given(jwtVerifier.verifyAccessToken(any(String.class))).willReturn(decodedJwt);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            var authentication = SecurityContextHolder.getContext()
                                                      .getAuthentication();

            assertThat(authentication).as("authentication")
                                      .isNotNull();
            assertSoftly(softly -> {
            // @formatter:off
                softly.assertThat(authentication).as("authentication type").isInstanceOf(JwtAuthenticationToken.class);
                softly.assertThat(authentication.getName()).as("authenticated subject").isEqualTo(decodedJwt.subject());
            // @formatter:on
            });

            then(filterChain).should(times(1))
                             .doFilter(request, response);
        }

        @Test
        void ContinuesFilterChainWithoutAuthentication_MissingBearerToken() throws Exception {
            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext()
                                            .getAuthentication()).isNull();

            then(filterChain).should(times(1))
                             .doFilter(request, response);
        }

        @Test
        void ContinuesFilterChainWithoutAuthentication_NonBearerAuthorizationHeader() throws Exception {
            // Given
            given(request.getHeader("Authorization")).willReturn("Basic dXNlcjpwYXNz");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext()
                                            .getAuthentication()).isNull();

            then(filterChain).should(times(1))
                             .doFilter(request, response);
        }

        @Test
        void ContinuesFilterChainWithoutAuthentication_UnexpectedTokenType() throws Exception {
            // Given
            var encodedRefreshToken = encodedRefreshToken();

            given(request.getHeader("Authorization")).willReturn("Bearer " + encodedRefreshToken);
            willThrow(new UnexpectedJwtTypeException("unexpected type")).given(jwtVerifier)
                                                                        .verifyAccessToken(any(String.class));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext()
                                            .getAuthentication()).isNull();

            then(filterChain).should(times(1))
                             .doFilter(request, response);
        }

        @Test
        void ContinuesFilterChainWithoutAuthentication_AuthenticationNotFound() throws Exception {
            // Given
            var encodedAccessToken = encodedAccessToken();

            given(request.getHeader("Authorization")).willReturn("Bearer " + encodedAccessToken);
            willThrow(new AuthenticationNotFoundException("session not found")).given(jwtVerifier)
                                                                               .verifyAccessToken(any(String.class));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext()
                                            .getAuthentication()).isNull();

            then(filterChain).should(times(1))
                             .doFilter(request, response);
        }

        @Test
        void ContinuesFilterChainWithoutAuthentication_ExpiredToken() throws Exception {
            // Given
            var encodedAccessToken = anEncodedTokenBuilder().accessToken()
                                                            .expired()
                                                            .build();

            given(request.getHeader("Authorization")).willReturn("Bearer " + encodedAccessToken);
            willThrow(new InvalidJwtException("invalid token", new RuntimeException())).given(jwtVerifier)
                                                                                       .verifyAccessToken(any(String.class));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext()
                                            .getAuthentication()).isNull();

            then(filterChain).should(times(1))
                             .doFilter(request, response);
        }

        @Test
        void ContinuesFilterChainWithoutAuthentication_UnexpectedVerificationError() throws Exception {
            // Given
            var encodedAccessToken = encodedAccessToken();

            given(request.getHeader("Authorization")).willReturn("Bearer " + encodedAccessToken);
            willThrow(new RuntimeException("unexpected error")).given(jwtVerifier)
                                                               .verifyAccessToken(any(String.class));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext()
                                            .getAuthentication()).isNull();

            then(filterChain).should(times(1))
                             .doFilter(request, response);
        }

    }

}
