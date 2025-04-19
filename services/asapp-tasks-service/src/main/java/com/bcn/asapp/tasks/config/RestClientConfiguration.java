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
package com.bcn.asapp.tasks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.bcn.asapp.tasks.config.security.JwtInterceptor;

/**
 * REST client configuration.
 * <p>
 * This configuration class creates and configures a {@link RestClient.Builder} bean that can be used throughout the application to create REST clients.
 *
 * @author ttrigo
 * @see RestClient
 * @since 0.2.0
 */
@Configuration
public class RestClientConfiguration {

    /**
     * Provides a pre-configured {@link RestClient.Builder}.
     * <p>
     * The builder is configured with:
     * <ul>
     * <li>A {@link JwtInterceptor} to automatically handle JWT authentication for all requests</li>
     * </ul>
     *
     * @return an instance of {@link RestClient.Builder}.
     */
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                         .requestInterceptor(new JwtInterceptor());
    }

}
