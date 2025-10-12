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

package com.bcn.asapp.authentication.infrastructure.authentication.out.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.NotNull;

import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;

/**
 * Entity representing a JWT authentication in the database.
 * <p>
 * Maps to the {@code jwt_authentications} table and provides persistence representation for {@link JwtAuthentication} domain entities.
 *
 * @param id           the JWT authentication's unique identifier
 * @param userId       the user's unique identifier; must not be {@code null}
 * @param accessToken  the embedded access token entity
 * @param refreshToken the embedded refresh token entity
 * @since 0.2.0
 * @author attrigo
 */
@Table("jwt_authentications")
public record JwtAuthenticationEntity(
        @Id UUID id,
        @NotNull UUID userId,
        @Embedded.Nullable(prefix = "access_token_") JwtEntity accessToken,
        @Embedded.Nullable(prefix = "refresh_token_") JwtEntity refreshToken
) {}
