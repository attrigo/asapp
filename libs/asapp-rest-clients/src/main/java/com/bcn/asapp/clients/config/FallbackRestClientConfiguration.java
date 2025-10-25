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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.bcn.asapp.clients.internal.uri.DefaultUriHandler;
import com.bcn.asapp.clients.internal.uri.UriHandler;

/**
 * Fallback REST client configuration.
 *
 * @since 0.1.0
 * @author attrigo
 */
@Configuration
public class FallbackRestClientConfiguration {

    /**
     * Provides a fallback {@link RestClient.Builder} bean.
     * <p>
     * The bean does not have any custom configuration, just the default {@link RestClient.Builder}.
     * <p>
     * This bean can be used to perform HTTP calls to REST services.
     * <p>
     * The bean is only created if the involved REST service has not declared a specific {@link RestClient.Builder}.
     *
     * @return a {@link RestClient.Builder} instance.
     */
    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Provides a fallback {@link UriHandler} bean.
     * <p>
     * This bean can be used to build URIs that points to the REST tasks service.
     * <p>
     * The bean is only created if the involved REST service has not declared a specific {@link UriHandler} bean with name {@literal tasksServiceUriHandler},
     * and has declared the property {@literal asapp.tasks-service.base-url}.
     *
     * @param taskServiceURL the base URL of tasks REST service.
     * @return a {@link UriHandler} instance.
     */
    @Bean
    @ConditionalOnMissingBean(name = "tasksServiceUriHandler")
    @ConditionalOnProperty("asapp.tasks-service.base-url")
    UriHandler tasksServiceUriHandler(@Value("${asapp.tasks-service.base-url}") String taskServiceURL) {
        return new DefaultUriHandler(taskServiceURL);
    }

}
