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
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;

/**
 * Port for granting authentications to users.
 * <p>
 * Defines the contract for generating JWT tokens based on authenticated user information.
 *
 * @since 0.2.0
 * @author attrigo
 */
@FunctionalInterface
public interface AuthenticationGranter {

    /**
     * Grants a JWT authentication for an authenticated user.
     * <p>
     * Generates access and refresh tokens based on the user's authentication information.
     *
     * @param authentication the {@link UserAuthentication} containing authenticated user data
     * @return the {@link JwtAuthentication} containing generated access and refresh tokens
     */
    JwtAuthentication grantAuthentication(UserAuthentication authentication);

}
