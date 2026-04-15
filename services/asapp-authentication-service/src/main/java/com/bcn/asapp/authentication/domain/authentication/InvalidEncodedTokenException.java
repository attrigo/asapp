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

package com.bcn.asapp.authentication.domain.authentication;

/**
 * Exception thrown when an encoded token fails domain validation.
 * <p>
 * This exception is thrown during authentication operations when a token does not meet the required format (valid JWT structure).
 *
 * @since 0.2.0
 * @author attrigo
 */
public class InvalidEncodedTokenException extends RuntimeException {

    /**
     * Constructs a new {@code InvalidEncodedTokenException} with the specified detail message.
     *
     * @param message the detail message explaining why the encoded token is invalid
     */
    public InvalidEncodedTokenException(String message) {
        super(message);
    }

}
