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

package com.bcn.asapp.uaa.security.authentication;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when a JWT (JSON Web Token) is not found during any authentication process.
 * <p>
 * Is typically thrown when the expected JWT is missing or unavailable.
 *
 * @since 0.2.0
 * @see AuthenticationException
 * @author ttrigo
 */
public class JwtNotFoundException extends AuthenticationException {

    /**
     * Constructs a new {@code JwtNotFoundException} with the specified detail message.
     *
     * @param message the detail message providing additional information about the exception
     */
    public JwtNotFoundException(String message) {
        super(message);
    }

}
