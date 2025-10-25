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
 * Represents the start date timestamp.
 * <p>
 * This value object encapsulates the start date timestamp as {@link Instant}.
 * <p>
 * It enforces structural integrity by ensuring the start date timestamp is not {@code null}.
 *
 * @param startDate the startDate instant
 * @since 0.2.0
 * @author attrigo
 */
public record StartDate(
        Instant startDate
) {

    /**
     * Constructs a new {@code StartDate} instance and validates its integrity.
     *
     * @param startDate the start date instant to validate and store
     * @throws IllegalArgumentException if the start date instant is {@code null}
     */
    public StartDate {
        validateStartDateIsNotNull(startDate);
    }

    /**
     * Factory method to create a new {@code StartDate} instance from an {@link Instant}.
     *
     * @param startDate the start date instant
     * @return a new {@code StartDate} instance
     * @throws IllegalArgumentException if the start date instant is {@code null}
     */
    public static StartDate of(Instant startDate) {
        return new StartDate(startDate);
    }

    /**
     * Factory method to create an optional {@code StartDate} instance.
     * <p>
     * Returns {@code null} if the provided start date is {@code null}, otherwise creates a valid {@code StartDate}.
     *
     * @param startDate the start date instant, may be {@code null}
     * @return a new {@code StartDate} instance if the value is not {@code null}, {@code null} otherwise
     */
    public static StartDate ofNullable(Instant startDate) {
        return startDate == null ? null : new StartDate(startDate);
    }

    /**
     * Returns the start date instant value.
     *
     * @return the start date {@link Instant}
     */
    public Instant value() {
        return this.startDate;
    }

    /**
     * Validates that the start date is not {@code null}.
     *
     * @param startDate the start date to validate
     * @throws IllegalArgumentException if the start date is {@code null}
     */
    private static void validateStartDateIsNotNull(Instant startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date must not be null");
        }
    }

}
