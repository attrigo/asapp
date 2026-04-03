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

package com.bcn.asapp.authentication.domain.authentication;

/**
 * Represents an encoded JWT string.
 * <p>
 * This value object encapsulates an encoded JWT value as {@link String}.
 * <p>
 * It enforces structural integrity by ensuring the token is not blank and follows the JWT format (three Base64URL-encoded segments separated by dots).
 *
 * @param token the encoded JWT value
 * @since 0.2.0
 * @author attrigo
 */
public record EncodedToken(
        String token
) {

    /**
     * Constructs a new {@code EncodedToken} instance and validates its integrity.
     *
     * @param token the encoded token to validate and store
     * @throws InvalidEncodedTokenException if the token is {@code null} or blank
     * @throws InvalidEncodedTokenException if the token does not follow the JWT format
     */
    public EncodedToken {
        validateTokenIsNotBlank(token);
        validateTokenIsJwtFormat(token);
    }

    /**
     * Factory method to create a new {@code EncodedToken} instance.
     *
     * @param token the encoded token string
     * @return a new {@code EncodedToken} instance
     * @throws InvalidEncodedTokenException if the token is {@code null} or blank or does not follow the JWT format
     */
    public static EncodedToken of(String token) {
        return new EncodedToken(token);
    }

    /**
     * Returns the encoded token value.
     *
     * @return the encoded JWT {@link String}
     */
    public String value() {
        return this.token;
    }

    /**
     * Validates that the token is not {@code null} or blank.
     *
     * @param token the token to validate
     * @throws InvalidEncodedTokenException if the token is {@code null} or blank
     */
    private static void validateTokenIsNotBlank(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidEncodedTokenException("Encoded token must not be null or empty");
        }
    }

    /**
     * Validates that the token follows the JWT format (three Base64URL-encoded segments separated by dots).
     *
     * @param token the token to validate
     * @throws InvalidEncodedTokenException if the token does not follow the JWT format
     */
    private static void validateTokenIsJwtFormat(String token) {
        if (!token.matches("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")) {
            throw new InvalidEncodedTokenException("Encoded token must be a valid JWT format");
        }
    }

}
