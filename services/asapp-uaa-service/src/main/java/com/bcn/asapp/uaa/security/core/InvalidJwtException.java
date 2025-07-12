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

import io.jsonwebtoken.JwtException;

/**
 * Exception thrown when an invalid JWT (JSON Web Token) is encountered.
 * <p>
 * Is typically thrown when a JWT is malformed, expired, or fails validation.
 *
 * @since 0.2.0
 * @see JwtException
 * @author ttrigo
 */
public class InvalidJwtException extends JwtException {

    /**
     * Constructs a new {@code InvalidJwtException} with the specified detail message.
     *
     * @param message the detail message providing additional information about the exception
     */
    public InvalidJwtException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code InvalidJwtException} with the specified detail message and cause.
     *
     * @param message the detail message providing additional information about the exception
     * @param cause   the underlying cause of the exception
     */
    public InvalidJwtException(String message, Throwable cause) {
        super(message, cause);
    }

}
