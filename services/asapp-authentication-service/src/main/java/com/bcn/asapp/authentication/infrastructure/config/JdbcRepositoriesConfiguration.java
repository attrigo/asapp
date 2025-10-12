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

import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Configuration class for enabling Spring Data JDBC repositories.
 * <p>
 * Activates Spring Data JDBC repository support, allowing automatic implementation of repository interfaces for database access.
 *
 * @since 0.2.0
 * @see EnableJdbcRepositories
 * @author attrigo
 */
@EnableJdbcRepositories
public class JdbcRepositoriesConfiguration {

}
