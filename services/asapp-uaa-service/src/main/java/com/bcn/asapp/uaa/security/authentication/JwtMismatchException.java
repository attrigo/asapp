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
 * Exception thrown when the JSON Web Token (JWT) cannot be matched with the one managed by the system.
 * <p>
 * Is typically thrown when a JWT does not belong to the expected user.
 *
 * @author ttrigo
 * @since 0.2.0
 */
public class JwtMismatchException extends AuthenticationException {

    /**
     * Constructs a new {@code JwtMismatchException} with the specified detail message.
     *
     * @param message the detail message providing additional information about the exception
     */
    public JwtMismatchException(String message) {
        super(message);
    }

}
