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
import java.util.Set;

import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.TimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.bcn.asapp.users.infrastructure.security.client.JwtInterceptor;

/**
 * Configuration class for HTTP REST clients.
 * <p>
 * Registers two {@link RestClient.Builder} beans:
 * <ul>
 * <li>A {@link Primary} plain builder used by Eureka and any unqualified injection point — no interceptors, no custom factory.</li>
 * <li>A {@link LoadBalanced} builder for service-to-service calls — carries a redirect-disabled factory and {@link JwtInterceptor}.</li>
 * </ul>
 * Both builders apply Boot's auto-configured defaults via {@link RestClientBuilderConfigurer}.
 * <p>
 * A {@link DefaultEurekaClientHttpRequestFactorySupplier} bean gives Eureka its own isolated HTTP factory, independent of any {@link RestClient.Builder} beans
 * in the context.
 *
 * @since 0.2.0
 * @see RestClient
 * @see LoadBalanced
 * @see DefaultEurekaClientHttpRequestFactorySupplier
 * @author attrigo
 */
@Configuration
public class RestClientConfiguration {

    /**
     * Creates a plain {@link RestClient.Builder} bean used by any unqualified injection point.
     * <p>
     * No interceptors or custom factory are applied — only Boot's auto-configured defaults via {@link RestClientBuilderConfigurer}.
     *
     * @param configurer Boot's auto-configured {@link RestClientBuilderConfigurer}
     * @return a plain {@link RestClient.Builder} carrying Boot defaults only
     */
    @Bean
    @Primary
    RestClient.Builder restClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    /**
     * Creates a {@link LoadBalanced} {@link RestClient.Builder} for service-to-service HTTP communication.
     * <p>
     * The {@link LoadBalanced} qualifier instructs Spring Cloud to apply a {@code LoadBalancerInterceptor} to this builder, enabling Eureka-based service-name
     * resolution at request time. The builder is also configured with redirect disabled and a {@link JwtInterceptor} that injects Bearer tokens into outgoing
     * requests.
     *
     * @param configurer Boot's auto-configured {@link RestClientBuilderConfigurer}
     * @return a {@link RestClient.Builder} pre-configured with redirect disabled and JWT propagation
     */
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        var httpClient = HttpClient.newBuilder()
                                   .followRedirects(HttpClient.Redirect.NEVER)
                                   .build();
        return configurer.configure(RestClient.builder())
                         .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                         .requestInterceptor(new JwtInterceptor());
    }

    /**
     * Creates a {@link RestClient} bean ready for service-to-service HTTP communication.
     * <p>
     * Builds the final client from the {@link LoadBalanced @LoadBalanced} {@link RestClient.Builder}, which Spring Cloud has already equipped with a
     * {@code LoadBalancerInterceptor} for Eureka-based service-name resolution.
     *
     * @param loadBalancedRestClientBuilder the load-balanced builder provided by {@link #loadBalancedRestClientBuilder(RestClientBuilderConfigurer)}
     * @return the configured {@link RestClient}
     */
    @Bean
    RestClient restClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        return loadBalancedRestClientBuilder.build();
    }

    /**
     * Provides Eureka with its own isolated {@link org.springframework.http.client.ClientHttpRequestFactory}, preventing it from picking up any application
     * {@link RestClient.Builder} beans.
     *
     * @return the default Eureka HTTP request factory supplier
     */
    @Bean
    DefaultEurekaClientHttpRequestFactorySupplier defaultEurekaClientHttpRequestFactorySupplier() {
        return new DefaultEurekaClientHttpRequestFactorySupplier(new TimeoutProperties(), Set.of());
    }

}
