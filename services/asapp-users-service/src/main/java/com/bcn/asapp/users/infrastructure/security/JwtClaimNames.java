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

package com.bcn.asapp.users.infrastructure.security;

/**
 * JWT Claim names and values used in authentication tokens.
 * <p>
 * Defines the contract for JWT structure across the application.
 * <p>
 * This class contains application-specific JWT claims that define how the application structures its authentication tokens.
 *
 * @since 0.2.0
 * @author attrigo
 */
public final class JwtClaimNames {

    /**
     * Claim name for the user role.
     */
    public static final String ROLE = "role";

    /**
     * Claim name indicating the token usage type.
     */
    public static final String TOKEN_USE = "token_use";

    /**
     * Claim value indicating access token usage.
     */
    public static final String ACCESS_TOKEN_USE = "access";

    /**
     * Claim value indicating refresh token usage.
     */
    public static final String REFRESH_TOKEN_USE = "refresh";

    private JwtClaimNames() {}

}
