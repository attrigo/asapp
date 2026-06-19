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

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import com.bcn.asapp.clients.tasks.TasksHttpClient;

/**
 * Registers the {@code tasks} HTTP service group.
 * <p>
 * Imports the {@link TasksHttpClient} declarative client as the {@code tasks} HTTP service group via {@link ImportHttpServices}.
 * <p>
 * The underlying Http client is configured generically by {@link RestClientConfiguration}.
 *
 * @since 0.4.0
 * @see ImportHttpServices
 * @author attrigo
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = TasksHttpClientConfiguration.TASKS_CLIENT_NAME, types = TasksHttpClient.class)
public class TasksHttpClientConfiguration {

    /**
     * Logical name identifying the tasks-service client, shared across its consumer-side wiring.
     */
    public static final String TASKS_CLIENT_NAME = "tasks";

}
