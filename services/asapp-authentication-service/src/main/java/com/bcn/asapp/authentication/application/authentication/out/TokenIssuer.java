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

import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.Role;

/**
 * Port for issuing authentication tokens.
 * <p>
 * Defines the contract for generating access and refresh tokens based on user authentication information.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface TokenIssuer {

    /**
     * Issues an access token for an authenticated user.
     *
     * @param userAuthentication the {@link UserAuthentication} containing authenticated user data
     * @return the generated {@link Jwt} access token
     */
    Jwt issueAccessToken(UserAuthentication userAuthentication);

    /**
     * Issues an access token for a subject and role.
     * <p>
     * This method is typically used when refreshing tokens, where user authentication data is extracted from an existing token.
     *
     * @param subject the {@link Subject} identifier
     * @param role    the {@link Role} for the token
     * @return the generated {@link Jwt} access token
     */
    Jwt issueAccessToken(Subject subject, Role role);

    /**
     * Issues a refresh token for an authenticated user.
     *
     * @param userAuthentication the {@link UserAuthentication} containing authenticated user data
     * @return the generated {@link Jwt} refresh token
     */
    Jwt issueRefreshToken(UserAuthentication userAuthentication);

    /**
     * Issues a refresh token for a subject and role.
     * <p>
     * This method is typically used when refreshing tokens, where user authentication data is extracted from an existing token.
     *
     * @param subject the {@link Subject} identifier
     * @param role    the {@link Role} for the token
     * @return the generated {@link Jwt} refresh token
     */
    Jwt issueRefreshToken(Subject subject, Role role);

}
