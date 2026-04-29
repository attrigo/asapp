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

package com.bcn.asapp.discovery.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.Set;

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import com.bcn.asapp.discovery.security.web.HttpBasicAuthenticationEntryPoint;

/**
 * Sets up security for the application using Spring Security.
 * <p>
 * Defines security filters and specifies the behavior of authentication and authorization across the application.
 * <p>
 * The class creates two Security Filter Chains, one to protect the management (Actuator) endpoints, and another to protect the Eureka server endpoints; the
 * declaration order of the filter chains is important, the first one to match will be used.
 * <p>
 * The purpose of the Spring Security Filter Chain is to provide a series of security filters that are executed in a specific order during each HTTP request.
 * These filters handle various security concerns such as authentication, authorization, session management, and CSRF protection. The filter chain ensures that
 * security-related operations are applied consistently across the application before any request is processed, enabling secure access control and protection of
 * resources.
 *
 * @since 0.3.0
 * @see SecurityFilterChain
 * @author attrigo
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    /**
     * Set of management (Actuator) URL patterns that are excluded from authentication checks.
     */
    public static final Set<String> MANAGEMENT_WHITELIST_URLS = Set.of("/readyz", "/livez");

    private final HttpBasicAuthenticationEntryPoint httpBasicAuthenticationEntryPoint;

    /**
     * Constructs a new {@code SecurityConfiguration} with required dependencies.
     *
     * @param httpBasicAuthenticationEntryPoint the authentication entry point
     */
    SecurityConfiguration(HttpBasicAuthenticationEntryPoint httpBasicAuthenticationEntryPoint) {
        this.httpBasicAuthenticationEntryPoint = httpBasicAuthenticationEntryPoint;
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
     * <li>Configures a custom {@link HttpBasicAuthenticationEntryPoint} that returns HTTP 401 with an empty body on authentication failure.</li>
     * <li>Enforces stateless session management.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to configure HTTP security
     * @return the configured {@link SecurityFilterChain}
     */
    @Bean
    @Order(1)
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
        http.httpBasic(basic -> basic.authenticationEntryPoint(httpBasicAuthenticationEntryPoint));
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
     * <li>Configures no authentication for the incoming requests that matches the public management endpoints {@code MANAGEMENT_WHITELIST_URLS}.</li>
     * <li>Configures HTTP Basic authentication requirements for the incoming requests that matches {@literal /**}.</li>
     * <li>Configures a custom {@link HttpBasicAuthenticationEntryPoint} that returns HTTP 401 with an empty body on authentication failure.</li>
     * <li>Enforces stateless session management.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to configure HTTP security
     * @return the configured {@link SecurityFilterChain}
     */
    @Bean
    @Order(2)
    DefaultSecurityFilterChain rootFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(MANAGEMENT_WHITELIST_URLS.toArray(String[]::new))
                .permitAll();
            auth.anyRequest()
                .authenticated();
        });
        http.httpBasic(basic -> basic.authenticationEntryPoint(httpBasicAuthenticationEntryPoint));
        http.sessionManagement(session -> session.sessionCreationPolicy(STATELESS));

        return http.build();
    }

}
