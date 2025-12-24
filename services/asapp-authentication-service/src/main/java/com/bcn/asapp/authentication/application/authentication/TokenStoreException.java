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
 * Exception thrown when JWT store operations fail during authentication operations.
 * <p>
 * Indicates that the token store port (fast-access store) could not complete a store operation such as save, delete, or query.
 * <p>
 * This exception covers both token activation (saving tokens) and token deactivation (deleting tokens) failures.
 * <p>
 * This typically occurs due to fast-access store connectivity issues, timeout failures, or cache eviction problems.
 *
 * @since 0.2.0
 * @author attrigo
 */
public class TokenStoreException extends RuntimeException {

    /**
     * Constructs a new {@code TokenStoreException} with the specified detail message and cause.
     *
     * @param message the detail message providing additional information about the exception
     * @param cause   the underlying cause of the exception
     */
    public TokenStoreException(String message, Throwable cause) {
        super(message, cause);
    }

}
