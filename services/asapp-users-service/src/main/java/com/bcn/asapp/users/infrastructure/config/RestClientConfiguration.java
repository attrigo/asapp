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

import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.netflix.eureka.TimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

import com.bcn.asapp.users.infrastructure.security.client.JwtInterceptor;

/**
 * Configures HTTP service clients and the shared {@link RestClient} infrastructure.
 * <p>
 * Splits {@link RestClient} setup into two isolated concerns:
 * <ul>
 * <li>The declarative HTTP service client groups, configured uniformly through a {@link RestClientHttpServiceGroupConfigurer}</li>
 * <li>A {@link Primary} plain {@link RestClient.Builder} for any unqualified injection point, paired with a dedicated
 * </ul>
 * <p>
 * Also defines a {@link DefaultEurekaClientHttpRequestFactorySupplier} which keeps Eureka isolated from the application's interceptors.
 *
 * @since 0.2.0
 * @see RestClientHttpServiceGroupConfigurer
 * @author attrigo
 */
@Configuration(proxyBeanMethods = false)
public class RestClientConfiguration {

    /**
     * Creates a plain {@link RestClient.Builder} bean used by any unqualified injection point.
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
     * Configures every declarative HTTP service client group's {@link RestClient}.
     * <p>
     * Applies to each client, in order:
     * <ol>
     * <li>A JDK request factory built from the property-bound {@link HttpClientSettings} (connect/read timeouts and redirect handling come from {@code
     * spring.http.clients.*}).</li>
     * <li>The {@link JwtInterceptor}, which propagates the caller's bearer token to the downstream call.</li>
     * <li>The {@link LoadBalancerInterceptor}, when one is available.</li>
     * </ol>
     * <p>
     * The interceptor is injected as an {@link ObjectProvider} and applied only when present: Spring Cloud LoadBalancer auto-configures the bean when it is on
     * the classpath, and the interceptor then resolves a base url that targets a Eureka service id into a concrete instance host and port. Without the bean
     * (e.g. in tests), the configured base-url host is called directly.
     *
     * @param loadBalancerInterceptor provider for the optional Spring Cloud load-balancer interceptor that resolves Eureka service ids to instances
     * @param httpClientSettings      the property-bound HTTP client settings ({@code spring.http.clients.*}) carrying connect/read timeouts and redirect
     *                                handling
     * @return the group configurer for RestClient-backed HTTP services
     */
    @Bean
    RestClientHttpServiceGroupConfigurer httpServiceGroupConfigurer(ObjectProvider<LoadBalancerInterceptor> loadBalancerInterceptor,
            HttpClientSettings httpClientSettings) {
        var requestFactory = ClientHttpRequestFactoryBuilder.jdk()
                                                            .build(httpClientSettings);

        return groupConfigurer -> groupConfigurer.forEachClient((_, clientBuilder) -> {
            clientBuilder.requestFactory(requestFactory)
                         .requestInterceptor(new JwtInterceptor());
            loadBalancerInterceptor.ifAvailable(clientBuilder::requestInterceptor);
        });
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
