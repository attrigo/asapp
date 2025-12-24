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

import com.bcn.asapp.authentication.application.PersistenceException;

/**
 * Exception thrown when authentication persistence operations fail.
 * <p>
 * Indicates that the {@code JwtAuthenticationRepository} port could not complete a persistence operation for authentication aggregates.
 * <p>
 * This typically occurs due to repository connectivity issues, constraint violations, or transaction failures.
 * <p>
 * This exception extends {@link PersistenceException} to provide authentication service context while allowing generic exception handling.
 *
 * @since 0.2.0
 * @author attrigo
 */
public class AuthenticationPersistenceException extends PersistenceException {

    /**
     * Constructs a new {@code AuthenticationPersistenceException} with the specified detail message and cause.
     *
     * @param message the detail message providing additional information about the exception
     * @param cause   the underlying cause of the exception
     */
    public AuthenticationPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

}
