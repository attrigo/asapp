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
 * Represents the issued-at timestamp of a JWT token.
 * <p>
 * This value object encapsulates the issued timestamp as {@link Instant}.
 * <p>
 * It enforces structural integrity by ensuring the issued timestamp is not {@code null}.
 *
 * @param issued the instant when the token was issued
 * @since 0.2.0
 * @author attrigo
 */
public record Issued(
        Instant issued
) {

    /**
     * Constructs a new {@code Issued} instance and validates its integrity.
     *
     * @param issued the issued instant to validate and store
     * @throws IllegalArgumentException if the issued instant is {@code null}
     */
    public Issued {
        validateIssuedIsNotNull(issued);
    }

    /**
     * Factory method to create a new {@code Issued} instance with the current time.
     *
     * @return a new {@code Issued} instance representing the current instant
     */
    public static Issued now() {
        return new Issued(Instant.now());
    }

    /**
     * Factory method to create a new {@code Issued} instance from an {@link Instant}.
     *
     * @param issuedInstant the issued instant
     * @return a new {@code Issued} instance
     * @throws IllegalArgumentException if the issued instant is {@code null}
     */
    public static Issued of(Instant issuedInstant) {
        return new Issued(issuedInstant);
    }

    /**
     * Factory method to create a new {@code Issued} instance from a {@link Date}.
     *
     * @param issuedDate the issued date
     * @return a new {@code Issued} instance
     * @throws IllegalArgumentException if the issued date is {@code null}
     */
    public static Issued of(Date issuedDate) {
        validateIssuedDateIsNotNull(issuedDate);

        var issuedInstant = Instant.ofEpochMilli(issuedDate.getTime());
        return new Issued(issuedInstant);
    }

    /**
     * Returns the issued instant value.
     *
     * @return the {@link Instant} when the token was issued
     */
    public Instant value() {
        return this.issued;
    }

    /**
     * Converts the issued instant to a {@link Date}.
     *
     * @return the issued instant as a {@link Date}
     */
    public Date asDate() {
        return Date.from(this.issued);
    }

    /**
     * Validates that the issued instant is not {@code null}.
     *
     * @param issued the issued instant to validate
     * @throws IllegalArgumentException if the issued instant is {@code null}
     */
    private static void validateIssuedIsNotNull(Instant issued) {
        if (issued == null) {
            throw new IllegalArgumentException("Issued must not be null");
        }
    }

    /**
     * Validates that the issued date is not {@code null}.
     *
     * @param issuedDate the issued date to validate
     * @throws IllegalArgumentException if the issued date is {@code null}
     */
    private static void validateIssuedDateIsNotNull(Date issuedDate) {
        if (issuedDate == null) {
            throw new IllegalArgumentException("Issued date must not be null");
        }
    }

}
