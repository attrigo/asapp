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

package com.bcn.asapp.authentication.application.authentication.in;

import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;

/**
 * Use case for refreshing authentications in the system.
 * <p>
 * Defines the contract for authentication refresh operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface RefreshAuthenticationUseCase {

    /**
     * Refreshes an authentication using a valid refresh token.
     *
     * @param refreshToken the refresh token string
     * @return the {@link JwtAuthentication} containing new access and refresh tokens
     * @throws IllegalArgumentException if the refresh token is invalid or blank
     */
    JwtAuthentication refreshAuthentication(String refreshToken);

}
