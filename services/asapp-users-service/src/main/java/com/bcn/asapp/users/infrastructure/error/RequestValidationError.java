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

package com.bcn.asapp.users.infrastructure.error;

/**
 * Represents a single validation error in an HTTP request.
 * <p>
 * Contains the field name and the validation message.
 *
 * @param field   the field name that failed validation
 * @param message the validation error message
 * @since 0.4.0
 * @author attrigo
 */
public record RequestValidationError(
        String field,
        String message
) {

    /**
     * Factory method to create a new {@code RequestValidationError} instance.
     *
     * @param field   the field name that failed validation
     * @param message the validation error message
     * @return a new {@code RequestValidationError} instance
     */
    static RequestValidationError of(String field, String message) {
        return new RequestValidationError(field, message);
    }

}
