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

import java.time.Instant;
import java.util.Date;

/**
 * Represents the expiration timestamp of a JWT token.
 * <p>
 * This value object encapsulates the expiration timestamp as {@link Instant}.
 * <p>
 * It enforces structural integrity by ensuring the expiration timestamp is not blank.
 *
 * @param expiration the instant when the token expires
 * @since 0.2.0
 * @author attrigo
 */
public record Expiration(
        Instant expiration
) {

    /**
     * Constructs a new {@code Expiration} instance and validates its integrity.
     *
     * @param expiration the expiration instant to validate and store
     * @throws IllegalArgumentException if the expiration instant is {@code null}
     */
    public Expiration {
        validateExpirationIsNotNull(expiration);
    }

    /**
     * Factory method to create a new {@code Expiration} instance from an issued time and duration.
     * <p>
     * Calculates the expiration by adding the duration to the issued instant.
     *
     * @param issued               the issued timestamp
     * @param expirationTimeMillis the duration in milliseconds until expiration
     * @return a new {@code Expiration} instance
     * @throws IllegalArgumentException if issued or expirationTimeMillis is {@code null}
     */
    public static Expiration of(Issued issued, Long expirationTimeMillis) {
        validateIssuedIsNotNull(issued);
        validateExpirationTimeMillisIsNotNull(expirationTimeMillis);

        var expirationInstant = issued.value()
                                      .plusMillis(expirationTimeMillis);
        return new Expiration(expirationInstant);
    }

    /**
     * Factory method to create a new {@code Expiration} instance from a {@link Date}.
     *
     * @param expirationDate the expiration date
     * @return a new {@code Expiration} instance
     * @throws IllegalArgumentException if the expiration date is {@code null}
     */
    public static Expiration of(Date expirationDate) {
        validateExpirationDateIsNotNull(expirationDate);

        var expirationInstant = Instant.ofEpochMilli(expirationDate.getTime());
        return new Expiration(expirationInstant);
    }

    /**
     * Returns the expiration instant value.
     *
     * @return the {@link Instant} when the token expires
     */
    public Instant value() {
        return this.expiration;
    }

    /**
     * Converts the expiration instant to a {@link Date}.
     *
     * @return the expiration instant as a {@link Date}
     */
    public Date asDate() {
        return Date.from(this.expiration);
    }

    /**
     * Validates that the expiration instant is not {@code null}.
     *
     * @param expiration the expiration instant to validate
     * @throws IllegalArgumentException if the expiration instant is {@code null}
     */
    private static void validateExpirationIsNotNull(Instant expiration) {
        if (expiration == null) {
            throw new IllegalArgumentException("Expiration must not be null");
        }
    }

    /**
     * Validates that the issued timestamp is not {@code null}.
     *
     * @param issued the issued timestamp to validate
     * @throws IllegalArgumentException if the issued timestamp is {@code null}
     */
    private static void validateIssuedIsNotNull(Issued issued) {
        if (issued == null) {
            throw new IllegalArgumentException("Issued must not be null");
        }
    }

    /**
     * Validates that the expiration time in milliseconds is not {@code null}.
     *
     * @param expirationTimeMillis the expiration time to validate
     * @throws IllegalArgumentException if the expiration time is {@code null}
     */
    private static void validateExpirationTimeMillisIsNotNull(Long expirationTimeMillis) {
        if (expirationTimeMillis == null) {
            throw new IllegalArgumentException("Expiration time in milliseconds must not be null");
        }
    }

    /**
     * Validates that the expiration date is not {@code null}.
     *
     * @param expirationDate the expiration date to validate
     * @throws IllegalArgumentException if the expiration date is {@code null}
     */
    private static void validateExpirationDateIsNotNull(Date expirationDate) {
        if (expirationDate == null) {
            throw new IllegalArgumentException("Expiration date must not be null");
        }
    }

}
