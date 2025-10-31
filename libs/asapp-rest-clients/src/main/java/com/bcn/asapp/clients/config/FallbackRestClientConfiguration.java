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

package com.bcn.asapp.clients.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Fallback configuration for REST client infrastructure.
 * <p>
 * This configuration provides a default {@link RestClient.Builder} bean when no custom builder has been defined by the consuming application.
 * <p>
 * It follows Spring Boot's autoconfiguration pattern, allowing services to either use this basic builder or override it with their own customized version.
 * <p>
 * <b>Usage Pattern:</b>
 * <ul>
 * <li>If a service <b>does not define</b> a {@link RestClient.Builder} bean, this fallback provides a basic builder with default settings</li>
 * <li>If a service <b>defines its own</b> {@link RestClient.Builder} bean, that bean takes precedence and this fallback is not created</li>
 * </ul>
 * <p>
 * <b>Typical Override Example:</b>
 *
 * <pre>
 * &#64;Configuration
 * public class CustomRestClientConfiguration {
 *
 *     &#64;Bean
 *     RestClient.Builder restClientBuilder() {
 *         return RestClient.builder()
 *                          .requestInterceptor(new JwtInterceptor()) // Custom interceptor
 *                          .defaultHeader("X-Custom-Header", "value");
 *     }
 *
 * }
 * </pre>
 * <p>
 * This fallback ensures that REST client beans in this library (such as {@code TasksServiceClient}) can always resolve a {@link RestClient.Builder} dependency,
 * whether the consuming service has provided custom configuration.
 *
 * @since 0.1.0
 * @see RestClient.Builder
 * @see ConditionalOnMissingBean
 * @author attrigo
 */
@Configuration
public class FallbackRestClientConfiguration {

    /**
     * Provides a fallback {@link RestClient.Builder} bean with default configuration.
     * <p>
     * It returns a builder with Spring's default settings, suitable for basic HTTP communication.
     * <p>
     * This bean is only created when no other {@link RestClient.Builder} bean exists in the application context, as indicated by the
     * {@link ConditionalOnMissingBean} annotation.
     * <p>
     * <b>When This Bean Is Created:</b>
     * <ul>
     * <li>The consuming application has <b>not</b> defined its own {@link RestClient.Builder} bean</li>
     * <li>REST client beans need a builder dependency</li>
     * </ul>
     * <p>
     * <b>When This Bean Is Skipped:</b>
     * <ul>
     * <li>The consuming application provides a custom {@link RestClient.Builder} with specific configuration (interceptors, headers, error handlers, etc.)</li>
     * </ul>
     * <p>
     * The builder has no custom interceptors, headers, or error handling - just Spring's out-of-the-box REST client functionality. Applications requiring
     * authentication, custom timeouts, or other HTTP client customizations should provide their own builder bean.
     *
     * @return a {@link RestClient.Builder} instance with default configuration
     */
    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

}
