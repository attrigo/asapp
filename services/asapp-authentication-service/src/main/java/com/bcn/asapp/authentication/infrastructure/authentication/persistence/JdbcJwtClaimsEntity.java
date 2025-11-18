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

package com.bcn.asapp.authentication.infrastructure.authentication.persistence;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

/**
 * Entity representing JWT claims.
 * <p>
 * Embeddable component for storing JWT claims data within {@link JdbcJwtEntity}.
 *
 * @param claims the map of claim names to claim values; must not be {@code null}
 * @since 0.2.0
 * @author attrigo
 */
public record JdbcJwtClaimsEntity(
        @NotNull Map<String, Object> claims
) {}
