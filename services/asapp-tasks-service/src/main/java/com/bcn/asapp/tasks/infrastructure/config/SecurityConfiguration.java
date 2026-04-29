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

package com.bcn.asapp.tasks.infrastructure.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.bcn.asapp.tasks.infrastructure.security.web.JwtAuthenticationEntryPoint;
import com.bcn.asapp.tasks.infrastructure.security.web.JwtAuthenticationFilter;

/**
 * Sets up security for the application using Spring Security.
 * <p>
 * Defines security filters and specifies the behavior of authentication and authorization across the application.
 * <p>
 * The class creates three Security Filter Chains, one to protect the API endpoints, one to protect the management (Actuator) endpoints, and another one to
 * protect the root path; the declaration order of the filter chains is important, the first one to match will be used.
 * <p>
 * The purpose of the Spring Security Filter Chain is to provide a series of security filters that are executed in a specific order during each HTTP request.
 * These filters handle various security concerns such as authentication, authorization, session management, and CSRF protection. The filter chain ensures that
 * security-related operations are applied consistently across the application before any request is processed, enabling secure access control and protection of
 * resources.
 *
 * @since 0.2.0
 * @see SecurityFilterChain
 * @author attrigo
 */
@Configuration(proxyBeanMethods = false)
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
     * Set of root URL patterns that are excluded from authentication checks.
     */
    public static final Set<String> ROOT_WHITELIST_URLS = Set.of("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");

    /**
     * Set of management (Actuator) URL patterns that are excluded from authentication checks.
     */
    public static final Set<String> MANAGEMENT_WHITELIST_URLS = Set.of("/readyz", "/livez");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final String managementUsername;

    private final String managementPassword;

    /**
     * Constructs a new {@code SecurityConfiguration} with required dependencies.
     *
     * @param jwtAuthenticationFilter     the JWT authentication filter
     * @param jwtAuthenticationEntryPoint the JWT authentication entry point
     * @param managementUsername          the management user username
     * @param managementPassword          the management user password
     */
    SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            @Value("${spring.security.user.name}") String managementUsername, @Value("${spring.security.user.password}") String managementPassword) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.managementUsername = managementUsername;
        this.managementPassword = managementPassword;
    }

    /**
     * Creates a {@link SecurityFilterChain} bean to protect any API (business) endpoints.
     * <p>
     * Applies the following configurations to the filter chain:
     * <ul>
     * <li>Disables CSRF.</li>
     * <li>Configures JWT authentication for the incoming requests that matches {@literal /api/**}.</li>
     * <li>Adds the exception handler, which is invoked when any authentication fails.</li>
     * <li>Adds the JWT authentication filter.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to configure HTTP security
     * @return the configured {@link SecurityFilterChain}
     */
    @Bean
    @Order(1)
    DefaultSecurityFilterChain apiFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable);
        http.securityMatcher(API_MATCHER)
            .authorizeHttpRequests(auth -> auth.anyRequest()
                                               .authenticated());
        http.sessionManagement(session -> session.sessionCreationPolicy(STATELESS));
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint));

        return http.build();
    }

    /**
     * Creates a {@link SecurityFilterChain} bean to protect any management (Actuator) endpoints.
     * <p>
     * Applies the following configurations to the filter chain:
     * <ul>
     * <li>Disables CSRF.</li>
     * <li>Disables CORS.</li>
     * <li>Configures no authentication for the incoming requests that matches the {@literal /actuator/health}.</li>
     * <li>Configures HTTP Basic authentication for the incoming requests that matches {@literal /actuator/**}.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to configure HTTP security
     * @return the configured {@link SecurityFilterChain}
     */
    @Bean
    @Order(2)
    DefaultSecurityFilterChain actuatorFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(AbstractHttpConfigurer::disable);
        http.securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(EndpointRequest.to(HealthEndpoint.class))
                    .permitAll();
                auth.anyRequest()
                    .authenticated();
            });
        http.httpBasic(Customizer.withDefaults());
        http.userDetailsService(managementUserDetailsManager());
        http.sessionManagement(session -> session.sessionCreationPolicy(STATELESS));

        return http.build();
    }

    /**
     * Creates a {@link SecurityFilterChain} bean to protect the root path of the application, in other words secures any endpoint that does not match any of
     * the previous filter chains.
     * <p>
     * Applies the following configurations to the filter chain:
     * <ul>
     * <li>Disables CSRF.</li>
     * <li>Configures no authentication for the incoming requests that matches the public API documentation endpoints {@code ROOT_WHITELIST_URLS}.</li>
     * <li>Configures no authentication for the incoming requests that matches the public management endpoints {@code MANAGEMENT_WHITELIST_URLS}.</li>
     * <li>Configures JWT authentication for the incoming requests that matches {@literal /**}.</li>
     * <li>Adds the exception handler, which is invoked when any authentication fails.</li>
     * <li>Adds the JWT authentication filter.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to configure HTTP security.
     * @return the configured {@link SecurityFilterChain}.
     */
    @Bean
    @Order(3)
    DefaultSecurityFilterChain rootFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable);
        http.securityMatcher(ROOT_MATCHER)
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(ROOT_WHITELIST_URLS.toArray(String[]::new))
                    .permitAll();
                auth.requestMatchers(MANAGEMENT_WHITELIST_URLS.toArray(String[]::new))
                    .permitAll();
                auth.anyRequest()
                    .authenticated();
            });
        http.sessionManagement(session -> session.sessionCreationPolicy(STATELESS));
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint));

        return http.build();
    }

    /**
     * Creates a {@link AuthenticationManager} bean.
     * <p>
     * The AuthenticationManager is responsible for authenticating task credentials and determining whether the authentication request is valid, delegating the
     * process to the appropriate authentication providers.
     *
     * @param authenticationConfiguration the configuration used to build the authentication manager
     * @return the {@link AuthenticationManager}
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Creates an {@link InMemoryUserDetailsManager} with the management user credentials.
     * <p>
     * Used by {@link #actuatorFilterChain} to authenticate actuator requests independently of the application's main {@link UserDetailsService}.
     *
     * @return the {@link InMemoryUserDetailsManager} holding the management user
     */
    private InMemoryUserDetailsManager managementUserDetailsManager() {
        var user = User.withUsername(managementUsername)
                       .password("{noop}" + managementPassword)
                       .build();
        return new InMemoryUserDetailsManager(user);
    }

}
