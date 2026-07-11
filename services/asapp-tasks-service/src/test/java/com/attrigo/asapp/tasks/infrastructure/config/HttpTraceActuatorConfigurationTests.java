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

package com.attrigo.asapp.tasks.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Tests {@link HttpTraceActuatorConfiguration} conditional registration of the HTTP exchange trace repository bean.
 * <p>
 * Setup:
 * <li>Runs a web application context pre-configured with endpoint auto-configuration</li>
 * <p>
 * Coverage:
 * <li>Registers the HTTP exchange trace repository bean when the httpexchanges endpoint is exposed</li>
 * <li>Omits the HTTP exchange trace repository bean when the httpexchanges endpoint is not exposed</li>
 */
class HttpTraceActuatorConfigurationTests {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
                                                                                               .withUserConfiguration(HttpTraceActuatorConfiguration.class);

    @Nested
    class CreateTraceRepository {

        @Test
        void RegistersTraceRepository_HttpExchangesEndpointExposed() {
            // When & Then
            contextRunner.withPropertyValues("management.endpoints.web.exposure.include=httpexchanges")
                         .run(context -> assertThat(context).hasSingleBean(InMemoryHttpExchangeRepository.class));
        }

        @Test
        void OmitsTraceRepository_HttpExchangesEndpointNotExposed() {
            // When & Then
            contextRunner.withPropertyValues("management.endpoints.web.exposure.include=health,info,prometheus,sbom")
                         .run(context -> assertThat(context).doesNotHaveBean(InMemoryHttpExchangeRepository.class));
        }

    }

}
