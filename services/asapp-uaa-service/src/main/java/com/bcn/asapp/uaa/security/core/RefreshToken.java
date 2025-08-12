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
import org.springframework.lang.NonNull;

/**
 * Represents a refresh token entity in the UAA (User Account and Authentication) service.
 * <p>
 * Encapsulates the JWT refresh token and its associated user ID along with expiration details.
 * <p>
 * This token is used to obtain new access tokens without requiring re-authentication.
 *
 * @param id        the unique identifier for the access token
 * @param userId    the identifier of the user associated with this access token
 * @param jwt       the JWT (JSON Web Token) string used for authenticating the user
 * @param createdAt the timestamp when the access token was created
 * @param expiresAt the timestamp when the access token expires and becomes invalid
 * @since 0.2.0
 * @author ttrigo
 */
@Table("refresh_token")
public record RefreshToken(
        @Id @Column("refresh_token_id") UUID id,
        @Column("user_id") UUID userId,
        @NonNull @Column("jwt") String jwt,
        @NonNull @Column("created_at") Instant createdAt,
        @NonNull @Column("expires_at") Instant expiresAt
) {}
