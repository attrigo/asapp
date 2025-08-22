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

package com.bcn.asapp.uaa.infrastructure.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.bcn.asapp.uaa.infrastructure.authentication.web.JwtAuthenticationConfigurer;

/**
 * Security configuration.
 * <p>
 * Sets up security for the application using Spring Security.
 * <p>
 * Defines security filters and specifies the behavior of authentication and authorization across the application.
 * <p>
 * The class creates three Security Filter Chain, one to protect the API endpoints, one to protect the Actuator endpoints, and another one to protect the root
 * path; the declaration order of the filter chains is important, the first one to match will be used.
 * <p>
 * The purpose of the Spring Security Filter Chain is to provide a series of security filters that are executed in a specific order during each HTTP request.
 * These filters handle various security concerns such as authentication, authorization, session management, and CSRF protection. The filter chain ensures that
 * security-related operations are applied consistently across the application before any request is processed, enabling secure access control and protection of
 * resources.
 *
 * @author ttrigo
 * @see SecurityFilterChain
 * @see AuthenticationManager
 * @see PasswordEncoder
 * @see PasswordEncoderFactories
 * @since 0.2.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    /**
     * The base API URL pattern, this pattern matches all paths under `/api/`.
     **/
    private static final String API_MATCHER = "/api/**";

    /**
     * The base root URL pattern, this pattern matches all paths under `/`.
     */
    private static final String ROOT_MATCHER = "/**";

    /**
     * Application probes URL patterns.
     */
    private static final String[] PROBES_URLS = { "/readyz", "/livez" };

    /**
     * Array of API URL patterns that are excluded from authentication and authorization checks.
     */
    private static final String[] API_WHITELIST_URLS = { "/api/auth/**" };

    /**
     * Array of API POST URL patterns that are excluded from authentication and authorization checks.
     */
    private static final String[] API_WHITELIST_POST_URLS = { "/api/users/**" };

    /**
     * Array of root URL patterns that are excluded from authentication and authorization checks.
     */
    private static final String[] ROOT_WHITELIST_URLS = { "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html" };

    private final JwtAuthenticationConfigurer jwtAuthenticationConfigurer;

    /**
     * Constructs a new {@code SecurityConfiguration} with the specified dependencies.
     *
     * @param authenticationEntryPoint the authentication entry point for handling authentication exceptions
     * @param authenticationFilter     the authentication filter to handle token authentication
     */
    public SecurityConfiguration(JwtAuthenticationConfigurer jwtAuthenticationConfigurer) {
        this.jwtAuthenticationConfigurer = jwtAuthenticationConfigurer;
    }

    /**
     * Creates a {@link SecurityFilterChain} bean to protect any API (business) endpoints.
     * <p>
     * Mainly these are the configurations applied to this filter chain:
     * <ul>
     * <li>Disables CSRF.</li>
     * <li>Configures the authentication requirements for the incoming requests that matches {@literal /api/**}.</li>
     * <li>Adds the exception handler, which is invoked when any authentication fails.</li>
     * <li>Adds the JWT authentication filter.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to configure HTTP security
     * @return an instance of {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(API_MATCHER);
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(API_WHITELIST_URLS)
                .permitAll();
            auth.requestMatchers(HttpMethod.POST, API_WHITELIST_POST_URLS)
                .permitAll();
            auth.anyRequest()
                .authenticated();
        });
        http.with(jwtAuthenticationConfigurer, withDefaults());

        return http.build();
    }

    /**
     * Creates a {@link SecurityFilterChain} bean to protect any management (Actuator) endpoints.
     * <p>
     * Mainly these are the configurations applied to this filter chain:
     * <ul>
     * <li>Disables CSRF.</li>
     * <li>Disables CORS.</li>
     * <li>Configures the authentication requirements for the incoming requests that matches {@literal /actuator/**}.</li>
     * <li>Adds the exception handler, which is invoked when any authentication fails.</li>
     * <li>Adds the JWT authentication filter.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to configure HTTP security
     * @return an instance of {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    @Order(2)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(EndpointRequest.toAnyEndpoint());
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(EndpointRequest.to(HealthEndpoint.class))
                .permitAll();
            auth.anyRequest()
                .authenticated();
        });
        http.with(jwtAuthenticationConfigurer, withDefaults());

        return http.build();
    }

    /**
     * Creates a {@link SecurityFilterChain} bean to protect the root path of the application, in other words secures any endpoint that does not match any of
     * the previous filter chains.
     * <p>
     * Mainly these are the configurations applied to this filter chain:
     * <ul>
     * <li>Disables CSRF.</li>
     * <li>Configures the authentication requirements for the incoming requests that matches {@literal /**}.</li>
     * <li>Adds the exception handler, which is invoked when any authentication fails.</li>
     * <li>Adds the JWT authentication filter.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to configure HTTP security.
     * @return an instance of {@link SecurityFilterChain}.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain rootFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(ROOT_MATCHER);
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(ROOT_WHITELIST_URLS)
                .permitAll();
            auth.requestMatchers(PROBES_URLS)
                .permitAll();
            auth.anyRequest()
                .authenticated();
        });
        http.with(jwtAuthenticationConfigurer, withDefaults());

        return http.build();
    }

    /**
     * Creates a {@link AuthenticationManager} bean.
     * <p>
     * The AuthenticationManager is responsible for authenticating user credentials and determining whether the authentication request is valid, delegating the
     * process to the appropriate authentication providers.
     *
     * @param authenticationConfiguration the configuration used to build the authentication manager
     * @return an instance of {@link AuthenticationManager}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Creates a {@link PasswordEncoder} bean.
     * <p>
     * The PasswordEncoder is responsible for encoding passwords and verifying their validity during authentication by comparing encoded passwords.
     * <p>
     * Uses {@link PasswordEncoderFactories#createDelegatingPasswordEncoder()} which creates a {@link DelegatingPasswordEncoder} that supports multiple encoding
     * schemes.
     *
     * @return an instance of {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
