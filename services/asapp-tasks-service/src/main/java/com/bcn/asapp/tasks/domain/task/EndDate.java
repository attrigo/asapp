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

package com.bcn.asapp.tasks.domain.task;

import java.time.Instant;

/**
 * Represents the end date timestamp.
 * <p>
 * This value object encapsulates the end date timestamp as {@link Instant}.
 * <p>
 * It enforces structural integrity by ensuring the end date timestamp is not {@code null}.
 *
 * @param endDate the endDate instant
 * @since 0.2.0
 * @author attrigo
 */
public record EndDate(
        Instant endDate
) {

    /**
     * Constructs a new {@code EndDate} instance and validates its integrity.
     *
     * @param endDate the end date instant to validate and store
     * @throws IllegalArgumentException if the end date instant is {@code null}
     */
    public EndDate {
        validateEndDateIsNotNull(endDate);
    }

    /**
     * Factory method to create a new {@code EndDate} instance from an {@link Instant}.
     *
     * @param endDate the end date instant
     * @return a new {@code EndDate} instance
     * @throws IllegalArgumentException if the end date instant is {@code null}
     */
    public static EndDate of(Instant endDate) {
        return new EndDate(endDate);
    }

    /**
     * Factory method to create an optional {@code EndDate} instance.
     * <p>
     * Returns {@code null} if the provided end date is {@code null}, otherwise creates a valid {@code EndDate}.
     *
     * @param endDate the end date instant, may be {@code null}
     * @return a new {@code EndDate} instance if the value is not {@code null}, {@code null} otherwise
     */
    public static EndDate ofNullable(Instant endDate) {
        return endDate == null ? null : new EndDate(endDate);
    }

    /**
     * Returns the end date instant value.
     *
     * @return the end date {@link Instant}
     */
    public Instant value() {
        return this.endDate;
    }

    /**
     * Validates that the end date is not {@code null}.
     *
     * @param endDate the end date to validate
     * @throws IllegalArgumentException if the end date is {@code null}
     */
    private static void validateEndDateIsNotNull(Instant endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("End date must not be null");
        }
    }

}
