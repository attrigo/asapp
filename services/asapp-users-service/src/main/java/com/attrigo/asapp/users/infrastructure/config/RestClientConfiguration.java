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

package com.attrigo.asapp.users.infrastructure.config;

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

import com.attrigo.asapp.users.infrastructure.security.client.JwtInterceptor;

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
     * Configures every declarative HTTP client group's {@link RestClient}, applying in order:
     * <ol>
     * <li>A JDK request factory from {@link HttpClientSettings} ({@code spring.http.clients.*});</li>
     * <li>The {@link JwtInterceptor} to propagate the caller's bearer token;</li>
     * <li>The optional {@link LoadBalancerInterceptor}, when present.</li>
     * </ol>
     * <p>
     * Injected via {@link ObjectProvider}, the load-balancer interceptor resolves Eureka service ids to instances when present; otherwise the configured
     * base-url host is called directly.
     *
     * @param loadBalancerInterceptor optional Eureka-aware load-balancer interceptor
     * @param httpClientSettings      HTTP client settings bound from {@code spring.http.clients.*}, applied to the JDK request factory
     * @return the RestClient group configurer
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
