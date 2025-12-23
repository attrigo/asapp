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
 * Generic exception thrown when a compensating transaction fails.
 * <p>
 * Indicates a critical failure where the system could not roll back changes after an operation failure, potentially leaving data in an inconsistent state
 * across storage systems.
 * <p>
 * This exception should trigger alerts and manual intervention.
 * <p>
 * This is a generic application-layer exception that can be used across the application ports for saga-based workflows. Specific exceptions should extend this
 * class to provide service context while allowing generic exception handling.
 *
 * @since 0.2.0
 * @author attrigo
 */
public class CompensatingTransactionException extends RuntimeException {

    /**
     * Constructs a new {@code CompensatingTransactionException} with the specified detail message and cause.
     *
     * @param message the detail message providing additional information about the exception
     * @param cause   the underlying cause of the exception
     */
    public CompensatingTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

}
