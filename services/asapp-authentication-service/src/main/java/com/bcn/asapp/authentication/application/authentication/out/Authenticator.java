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

import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;

/**
 * Port for authenticating users based on credentials.
 * <p>
 * Defines the contract for validating user credentials and producing authenticated user.
 *
 * @since 0.2.0
 * @author attrigo
 */
@FunctionalInterface
public interface Authenticator {

    /**
     * Authenticates a user based on the provided authentication request.
     * <p>
     * Validates the user's credentials and returns an authenticated user with identity and role information.
     *
     * @param authenticationRequest the {@link UserAuthentication} containing unauthenticated credentials
     * @return the {@link UserAuthentication} containing authenticated user data with ID and role
     */
    UserAuthentication authenticate(UserAuthentication authenticationRequest);

}
