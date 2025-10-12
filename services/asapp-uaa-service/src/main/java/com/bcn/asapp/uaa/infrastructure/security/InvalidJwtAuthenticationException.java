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

package com.bcn.asapp.uaa.infrastructure.security;

/**
 * Exception thrown when a JWT authentication operation fails.
 * <p>
 * Indicates issues during authentication refresh or revocation processes.
 *
 * @since 0.2.0
 * @author attrigo
 */
public class InvalidJwtAuthenticationException extends RuntimeException {

    /**
     * Constructs a new {@code InvalidJwtAuthenticationException} with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public InvalidJwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

}
