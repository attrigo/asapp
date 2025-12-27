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

package com.bcn.asapp.authentication.application.authentication;

/**
 * Base exception for authentication failures in the application layer.
 * <p>
 * Indicates a failures during authentication workflows such as token generation, persistence, activation, or deactivation.
 * <p>
 * This is a generic application-layer exception that can be used across the application ports when any authentication flow fails. Specific exceptions should
 * extend this class to provide service context while allowing generic exception handling.
 *
 * @since 0.2.0
 * @author attrigo
 */
public class AuthenticationException extends RuntimeException {

    /**
     * Constructs a new {@code AuthenticationException} with the specified detail message.
     *
     * @param message the detail message providing additional information about the exception
     */
    public AuthenticationException(String message) {
        super(message);
    }

}
