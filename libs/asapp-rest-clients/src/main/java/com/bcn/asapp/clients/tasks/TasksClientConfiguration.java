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

package com.bcn.asapp.clients.tasks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.bcn.asapp.clients.util.DefaultUriHandler;
import com.bcn.asapp.clients.util.UriHandler;

/**
 * Configuration class for the Tasks Service client beans.
 * <p>
 * This configuration is conditionally enabled only when the property {@code asapp.client.tasks.base-url} is defined, allowing consuming services to opt in to
 * Tasks Service integration by providing the service's base URL.
 * <p>
 * The configuration creates:
 * <ul>
 * <li>A {@link UriHandler} configured with the Tasks Service base URL</li>
 * <li>A {@link TasksClient} implementation for communicating with the service</li>
 * </ul>
 * <p>
 * The {@link RestClient.Builder} is expected to be provided by the consuming service and should include the necessary configuration such as JWT authentication.
 *
 * @since 0.2.0
 * @see ConditionalOnProperty
 * @see ConditionalOnMissingBean
 * @see RestClient.Builder
 * @author attrigo
 */
@Configuration
@ConditionalOnProperty(name = "asapp.client.tasks.base-url")
public class TasksClientConfiguration {

    /**
     * Creates a {@link UriHandler} bean configured with the Tasks Service base URL.
     * <p>
     * This handler is used to build URIs for Tasks Service endpoints by prepending the configured base URL to endpoint paths.
     *
     * @param taskClientBaseUrl the base URL of the Tasks Service, injected from {@code asapp.client.tasks.base-url} property
     * @return a configured {@link UriHandler} for the Tasks Service
     */
    @Bean
    @ConditionalOnMissingBean(name = "tasksServiceUriHandler")
    UriHandler tasksServiceUriHandler(@Value("${asapp.client.tasks.base-url}") String taskClientBaseUrl) {
        return new DefaultUriHandler(taskClientBaseUrl);
    }

    /**
     * Creates a {@link TasksClient} bean for interacting with the Tasks Service.
     * <p>
     * The client uses the provided {@link RestClient.Builder} to create a REST client instance. The builder should be pre-configured by the consuming service
     * with necessary configuration (e.g., {@code JwtInterceptor}).
     * <p>
     * The {@link UriHandler} specific to the Tasks Service is used to build request URIs.
     *
     * @param tasksServiceUriHandler the URI handler configured with the Tasks Service base URL
     * @param restClientBuilder      the REST client builder provided by the consuming service
     * @return a configured {@link TasksClient} implementation
     */
    @Bean
    TasksClient taskClient(UriHandler tasksServiceUriHandler, RestClient.Builder restClientBuilder) {
        return new TasksRestClient(tasksServiceUriHandler, restClientBuilder.build());
    }

}
