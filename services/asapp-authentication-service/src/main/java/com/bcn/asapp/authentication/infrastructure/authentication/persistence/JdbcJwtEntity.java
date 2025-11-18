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

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing a JWT token.
 * <p>
 * Embeddable component for storing JWT token data within {@link JdbcJwtAuthenticationEntity}.
 *
 * @param token      the encoded JWT token string; must not be blank
 * @param type       the token type identifier; must not be {@code null}
 * @param subject    the subject identifier; must not be blank
 * @param claims     the JWT claims entity; must not be {@code null}
 * @param issued     the issued-at timestamp; must not be {@code null}
 * @param expiration the expiration timestamp; must not be {@code null}
 * @since 0.2.0
 * @author attrigo
 */
public record JdbcJwtEntity(
        @NotBlank String token,
        @NotNull String type,
        @NotBlank String subject,
        @NotNull JdbcJwtClaimsEntity claims,
        @NotNull Instant issued,
        @NotNull Instant expiration
) {}
