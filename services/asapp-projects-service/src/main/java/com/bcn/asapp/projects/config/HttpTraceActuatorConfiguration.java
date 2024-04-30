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
package com.bcn.asapp.projects.config;

import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring's Actuator HTTP traces.
 * <p>
 * By default, traces are stored in memory.
 *
 * @author ttrigo
 * @since 0.1.0
 */
@Configuration
public class HttpTraceActuatorConfiguration {

    /**
     * Provides an {@link InMemoryHttpExchangeRepository} bean.
     * <p>
     * This bean is used to store HTTP traces in memory.
     * <p>
     * These traces can be accessed from Spring Actuator endpoint.
     *
     * @return an in {@link InMemoryHttpExchangeRepository} instance.
     */
    @Bean
    InMemoryHttpExchangeRepository createTraceRepository() {
        return new InMemoryHttpExchangeRepository();
    }

}
