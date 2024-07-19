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
package com.bcn.asapp.authorization.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * TODO
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final List<String> ALLOWED_HEADERS = List.of("Access-Control-Allow-Origin", "x-requested-with");

    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE");

    private static final List<String> ALLOWED_ALL = List.of("http://127.0.0.1:8080", "http://127.0.0.1:8081");

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ALLOWED_ALL);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        var userDetails = User.withDefaultPasswordEncoder()
                              .username("user")
                              .password("pw")
                              .roles("USER")
                              .build();

        var adminDetails = User.withDefaultPasswordEncoder()
                               .username("admin")
                               .password("pw")
                               .roles("ADMIN")
                               .build();

        return new InMemoryUserDetailsManager(userDetails, adminDetails);
    }

}
