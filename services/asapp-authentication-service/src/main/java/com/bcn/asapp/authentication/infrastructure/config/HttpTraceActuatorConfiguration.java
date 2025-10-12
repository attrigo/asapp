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

package com.bcn.asapp.authentication.infrastructure.config;

import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for HTTP trace actuator.
 * <p>
 * Configures an in-memory repository for storing HTTP exchange traces, enabling the Spring Boot Actuator's HTTP trace endpoint for monitoring and debugging.
 *
 * @since 0.2.0
 * @see InMemoryHttpExchangeRepository
 * @author attrigo
 */
@Configuration
public class HttpTraceActuatorConfiguration {

    /**
     * Creates an in-memory HTTP exchange repository.
     *
     * @return the {@link InMemoryHttpExchangeRepository} for storing HTTP traces
     */
    @Bean
    InMemoryHttpExchangeRepository createTraceRepository() {
        return new InMemoryHttpExchangeRepository();
    }

}
