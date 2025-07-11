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
 * Exception thrown when there is a violation of the integrity of the JWT (JSON Web Token) in the system.
 * <p>
 * Is typically thrown when there are integrity issues with access and refresh tokens pair.
 *
 * @since 0.2.0
 * @see AuthenticationException
 * @author ttrigo
 */
public class JwtIntegrityViolationException extends AuthenticationException {

    /**
     * Constructs a new {@code JwtIntegrityViolationException} with the specified detail message and cause.
     *
     * @param message the detail message providing additional information about the exception
     * @param cause   the underlying cause of this exception
     */
    public JwtIntegrityViolationException(String message, Throwable cause) {
        super(message, cause);
    }

}
