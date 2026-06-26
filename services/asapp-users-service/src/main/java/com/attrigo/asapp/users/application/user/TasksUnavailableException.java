/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.attrigo.asapp.users.application.user;

/**
 * Signals that the tasks-service could not be reached or returned a server error, so task data is temporarily unavailable.
 * <p>
 * Thrown by the tasks gateway adapter when a downstream outage (5xx, I/O failure, or open circuit) is detected, and caught by the application service to
 * degrade the user read gracefully.
 *
 * @since 0.4.0
 * @author attrigo
 */
public class TasksUnavailableException extends RuntimeException {

    /**
     * Constructs a new {@code TasksUnavailableException} with the given message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying downstream failure
     */
    public TasksUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
