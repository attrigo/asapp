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

package com.bcn.asapp.users.infrastructure.config;

import java.net.http.HttpClient;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import com.bcn.asapp.users.infrastructure.security.client.JwtInterceptor;

/**
 * Configuration class for HTTP REST clients.
 * <p>
 * Registers a {@link RestClientCustomizer} bean that Spring Boot applies to the shared {@link org.springframework.web.client.RestClient.Builder}, configuring
 * it for service-to-service HTTP communication with redirect disabled and automatic JWT authentication propagation.
 *
 * @since 0.2.0
 * @see RestClientCustomizer
 * @author attrigo
 */
@Configuration
public class RestClientConfiguration {

    /**
     * Creates a {@link RestClientCustomizer} bean that configures the shared {@link org.springframework.web.client.RestClient.Builder} for service-to-service
     * HTTP communication.
     * <p>
     * Applies a redirect-disabled {@link JdkClientHttpRequestFactory} and a {@link JwtInterceptor} that automatically injects JWT Bearer tokens into the
     * Authorization header of all outgoing HTTP requests.
     *
     * @return the {@link RestClientCustomizer}
     */
    @Bean
    RestClientCustomizer restClientCustomizer() {
        HttpClient httpClient = HttpClient.newBuilder()
                                          .followRedirects(HttpClient.Redirect.NEVER)
                                          .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);

        return builder -> builder.requestFactory(requestFactory)
                                 .requestInterceptor(new JwtInterceptor());
    }

}
