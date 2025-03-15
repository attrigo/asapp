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
package com.bcn.asapp.tasks.config;

import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Jdbc repositories configuration.
 * <p>
 * Enables the use of JDBC repositories in the application. This allows the application to use Spring Data's JDBC support for data persistence and retrieval
 * operations.
 *
 * @author ttrigo
 * @see EnableJdbcRepositories
 * @since 0.1.0
 */
@EnableJdbcRepositories
public class JdbcRepositoriesConfiguration {}
