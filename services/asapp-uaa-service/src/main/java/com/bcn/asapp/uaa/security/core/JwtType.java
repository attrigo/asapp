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

/**
 * Defines the types of JWT (JSON Web Token) used for authentication and authorization purposes.
 * <p>
 * Specifies the two standard token categories:
 * <ul>
 * <li>{@link #ACCESS_TOKEN} — used to authenticate a user and grant access to protected resources.</li>
 * <li>{@link #REFRESH_TOKEN} — used to obtain a new access token after the current one expires.</li>
 * </ul>
 *
 * @author ttrigo
 * @since 0.2.0
 */
public enum JwtType {

    /**
     * Token type used to authenticate a user and authorize access to resources.
     */
    ACCESS_TOKEN,

    /**
     * Token type used to refresh an expired access token.
     */
    REFRESH_TOKEN;
}
