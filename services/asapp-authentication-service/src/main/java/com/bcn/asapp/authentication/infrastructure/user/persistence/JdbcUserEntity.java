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

package com.bcn.asapp.authentication.infrastructure.user.persistence;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing a user in the database.
 * <p>
 * Maps to the {@code users} table and provides persistence representation for user domain entities.
 *
 * @param id       the user's unique identifier
 * @param username the user's username; must not be blank
 * @param password the user's encoded password; must not be blank
 * @param role     the user's role; must not be {@code null}
 * @since 0.2.0
 * @author attrigo
 */
@Table("users")
public record JdbcUserEntity(
        @Id UUID id,
        @NotBlank String username,
        @NotBlank String password,
        @NotNull String role
) {}
