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
package com.bcn.asapp.uaa.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration.
 * <p>
 * Sets up security for the application using Spring Security.
 * <p>
 * Defines security filters and specifies the behavior of authentication and authorization across the application.
 *
 * @author ttrigo
 * @see JwtAuthenticationEntryPoint
 * @see SecurityFilterChain
 * @see PasswordEncoder
 * @see AuthenticationManager
 * @since 0.2.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final String[] WHITELIST_URLS = { "/v1/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html" };

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Main constructor.
     *
     * @param authenticationEntryPoint the authentication entry point for handling authentication exceptions.
     */
    public SecurityConfiguration(JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    /**
     * Creates a {@link SecurityFilterChain} bean.
     * <p>
     * The purpose of the Spring Security Filter Chain is to provide a series of security filters that are executed in a specific order during each HTTP
     * request. These filters handle various security concerns such as authentication, authorization, session management, and CSRF protection. The filter chain
     * ensures that security-related operations are applied consistently across the application before any request is processed, enabling secure access control
     * and protection of resources.
     * <p>
     * Mainly these are the configurations applied to the filter chain:
     * <ul>
     * <li>Disables CSRF.</li>
     * <li>Configures the authentication requirements for the incoming request.</li>
     * <li>Adds the exception handler, which is invoked when any authentication fails.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object used to configure HTTP security.
     * @return an instance of {@link SecurityFilterChain}.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(WHITELIST_URLS)
                    .permitAll();
                auth.anyRequest()
                    .authenticated();
            })
            .httpBasic(Customizer.withDefaults());

        http.exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint));

        return http.build();
    }

    /**
     * Creates a {@link AuthenticationManager} bean.
     * <p>
     * The AuthenticationManager is responsible for authenticating user credentials and determining whether the authentication request is valid, delegating the
     * process to the appropriate authentication providers.
     *
     * @param authenticationConfiguration the configuration used to build the authentication manager.
     * @return an instance of {@link AuthenticationManager}.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Creates a {@link PasswordEncoder} bean.
     * <p>
     * The PasswordEncoder is responsible for encoding passwords and verifying their validity during authentication by comparing encoded passwords.
     *
     * @return an instance of {@link PasswordEncoder}.
     */
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
