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

package com.bcn.asapp.authentication.application;

/**
 * Generic exception thrown when repository persistence operations fail.
 * <p>
 * Indicates that a repository port could not complete a persistence operation (save, update, delete, or query).
 * <p>
 * This is a generic application-layer exception that can be used across the application ports when repository operations fail. Specific exceptions should
 * extend this class to provide service context while allowing generic exception handling.
 *
 * @since 0.2.0
 * @author attrigo
 */
public class PersistenceException extends RuntimeException {

    /**
     * Constructs a new {@code PersistenceException} with the specified detail message and cause.
     *
     * @param message the detail message providing additional information about the exception
     * @param cause   the underlying cause of the exception
     */
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

}
