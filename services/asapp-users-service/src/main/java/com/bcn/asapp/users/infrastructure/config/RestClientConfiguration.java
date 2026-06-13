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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.netflix.eureka.TimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

import com.bcn.asapp.users.infrastructure.security.client.JwtInterceptor;

/**
 * Configures HTTP service clients and the shared {@link RestClient} infrastructure.
 * <p>
 * Configures every declarative HTTP service client group through a {@link RestClientHttpServiceGroupConfigurer}: a redirect-disabled JDK request factory, a
 * {@link JwtInterceptor} for bearer-token propagation, and (when Spring Cloud LoadBalancer is enabled) the {@link LoadBalancerInterceptor} for Eureka-based
 * service-name resolution.
 * <p>
 * A {@link Primary} plain {@link RestClient.Builder} is kept for Eureka and any unqualified injection point, and a
 * {@link DefaultEurekaClientHttpRequestFactorySupplier} gives Eureka its own isolated HTTP factory.
 *
 * @since 0.2.0
 * @see RestClientHttpServiceGroupConfigurer
 * @author attrigo
 */
@Configuration(proxyBeanMethods = false)
public class RestClientConfiguration {

    /**
     * Creates a plain {@link RestClient.Builder} bean used by Eureka and any unqualified injection point.
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
     * Applies to each client, in order: a redirect-disabled JDK request factory; the {@link JwtInterceptor}, which propagates the caller's bearer token to the
     * downstream call; and finally the {@link LoadBalancerInterceptor}, when one is available.
     * <p>
     * The interceptor is injected as an {@link ObjectProvider} and applied only when present: Spring Cloud LoadBalancer auto-configures the bean when it is on
     * the classpath, and the interceptor then resolves a base url that targets a Eureka service id into a concrete instance host and port. Without the bean
     * (e.g. in tests), the configured base-url host is called directly.
     *
     * @param loadBalancerInterceptor provider for the optional Spring Cloud load-balancer interceptor that resolves Eureka service ids to instances
     * @return the group configurer for RestClient-backed HTTP services
     */
    @Bean
    RestClientHttpServiceGroupConfigurer httpServiceGroupConfigurer(ObjectProvider<LoadBalancerInterceptor> loadBalancerInterceptor) {
        var httpClient = HttpClient.newBuilder()
                                   .followRedirects(HttpClient.Redirect.NEVER)
                                   .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);

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
