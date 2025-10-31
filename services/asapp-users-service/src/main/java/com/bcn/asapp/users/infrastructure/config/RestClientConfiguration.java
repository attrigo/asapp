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

package com.bcn.asapp.users.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.bcn.asapp.users.infrastructure.security.client.JwtInterceptor;

/**
 * Configuration class for HTTP REST clients.
 * <p>
 * Configures a {@link RestClient.Builder} bean with interceptors for outgoing HTTP requests, enabling automatic JWT authentication propagation.
 *
 * @since 0.2.0
 * @see RestClient.Builder
 * @see JwtInterceptor
 * @author attrigo
 */
@Configuration
public class RestClientConfiguration {

    /**
     * Creates a {@link RestClient.Builder} bean with JWT authentication support.
     * <p>
     * Configures the builder with a {@link JwtInterceptor} that automatically injects JWT Bearer tokens into the Authorization header of all outgoing HTTP
     * requests.
     *
     * @return the configured {@link RestClient.Builder}
     */
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                         .requestInterceptor(new JwtInterceptor());
    }

}
