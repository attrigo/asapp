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

package com.bcn.asapp.uaa.application.authentication.out;

import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;

/**
 * Port for revoking authentications.
 * <p>
 * Defines the contract for removing JWT authentication sessions from the system.
 *
 * @since 0.2.0
 * @author attrigo
 */
@FunctionalInterface
public interface AuthenticationRevoker {

    /**
     * Revokes a JWT authentication session.
     * <p>
     * Removes the authentication and its associated tokens from the system, effectively invalidating the session.
     *
     * @param authentication the {@link JwtAuthentication} to revoke
     */
    void revokeAuthentication(JwtAuthentication authentication);

}
