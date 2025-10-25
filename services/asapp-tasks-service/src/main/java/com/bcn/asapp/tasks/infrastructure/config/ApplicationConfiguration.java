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

package com.bcn.asapp.tasks.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import com.bcn.asapp.tasks.application.ApplicationService;

/**
 * Configuration class for the application layer.
 * <p>
 * Enables component scanning for application services annotated with {@link ApplicationService}, allowing them to be discovered and registered as Spring beans
 * without direct Spring dependencies in the application layer.
 *
 * @since 0.2.0
 * @see ComponentScan
 * @author attrigo
 */
@Configuration
@ComponentScan(basePackages = "com.bcn.asapp.tasks.application", includeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = { ApplicationService.class }) })
public class ApplicationConfiguration {

}
