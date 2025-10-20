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

package com.bcn.asapp.authentication.application.authentication.out;

import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;

/**
 * Port for refreshing authentications.
 * <p>
 * Defines the contract for generating new JWT tokens from existing authentications.
 *
 * @since 0.2.0
 * @author attrigo
 */
@FunctionalInterface
public interface JwtAuthenticationRefresher {

    /**
     * Refreshes a JWT authentication with new tokens.
     * <p>
     * Generates new access and refresh tokens while maintaining the same authentication session.
     *
     * @param authentication the existing {@link JwtAuthentication} to refresh
     * @return the {@link JwtAuthentication} containing new access and refresh tokens
     */
    JwtAuthentication refreshAuthentication(JwtAuthentication authentication);

}
