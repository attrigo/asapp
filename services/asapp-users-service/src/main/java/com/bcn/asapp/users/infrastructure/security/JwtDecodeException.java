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

package com.bcn.asapp.users.infrastructure.security;

/**
 * Exception thrown when JWT decoding or validation fails during authentication operations.
 * <p>
 * Indicates that a JWT could not be parsed, its signature could not be verified, or it has expired.
 *
 * @since 0.2.0
 * @author attrigo
 */
public class JwtDecodeException extends RuntimeException {

    /**
     * Constructs a new {@code JwtDecodeException} with the specified detail message.
     *
     * @param message the detail message providing additional information about the exception
     */
    public JwtDecodeException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code JwtDecodeException} with the specified detail message and cause.
     *
     * @param message the detail message providing additional information about the exception
     * @param cause   the underlying cause of the exception
     */
    public JwtDecodeException(String message, Throwable cause) {
        super(message, cause);
    }

}
