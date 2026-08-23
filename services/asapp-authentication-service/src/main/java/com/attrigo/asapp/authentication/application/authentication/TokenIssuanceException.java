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

package com.attrigo.asapp.authentication.application.authentication;

/**
 * Exception thrown when JWT signing or issuance operations fail.
 * <p>
 * Indicates that the token issuer port could not sign or build a token.
 * <p>
 * This typically occurs due to a cryptographic failure in the underlying signing mechanism.
 *
 * @since 0.2.0
 * @author attrigo
 */
public class TokenIssuanceException extends RuntimeException {

    /**
     * Constructs a new {@code TokenIssuanceException} with the specified detail message and cause.
     *
     * @param message the detail message providing additional information about the exception
     * @param cause   the underlying cause of the exception
     */
    public TokenIssuanceException(String message, Throwable cause) {
        super(message, cause);
    }

}
