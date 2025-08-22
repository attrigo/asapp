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

package com.bcn.asapp.uaa.application.authentication.in;

import com.bcn.asapp.uaa.application.authentication.in.command.AuthenticateCommand;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;

/**
 * Use case for authenticating users in the system.
 * <p>
 * Defines the contract for user authentication operations.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface AuthenticateUseCase {

    /**
     * Authenticates a user based on the provided credentials.
     *
     * @param authenticateCommand the {@link AuthenticateCommand} containing user credentials
     * @return the {@link JwtAuthentication} containing access and refresh tokens
     * @throws IllegalArgumentException if the username or password is invalid
     */
    JwtAuthentication authenticate(AuthenticateCommand authenticateCommand);

}
