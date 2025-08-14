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

package com.bcn.asapp.uaa.security.core;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents an access token entity in the UAA (User Account and Authentication) service.
 * <p>
 * Encapsulates the JWT access token string along with its associated user ID, creation timestamp, and expiration timestamp.
 * <p>
 * This token is used to authenticate and authorize user requests within the system.
 *
 * @param id        the unique identifier for the access token
 * @param userId    the identifier of the user associated with this access token, must not be {@literal blank}
 * @param jwt       the JWT (JSON Web Token) string used for authenticating the user, must not be {@literal blank}
 * @param createdAt the timestamp when the access token was created, must not be {@literal null}
 * @param expiresAt the timestamp when the access token expires and becomes invalid, must not be {@literal null}
 * @since 0.2.0
 * @author ttrigo
 */
@Table("access_token")
public record AccessToken(
        @Id @Column("access_token_id") UUID id,
        @Column("user_id") @NotBlank UUID userId,
        @Column("jwt") @NotBlank String jwt,
        @Column("created_at") @NotNull Instant createdAt,
        @Column("expires_at") @NotNull Instant expiresAt
) {}
