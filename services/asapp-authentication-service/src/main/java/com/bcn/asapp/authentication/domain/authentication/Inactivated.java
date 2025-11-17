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

/**
 * Represents the timestamp when a JWT authentication inactive.
 * <p>
 * Value object that wraps an {@link Instant} representing when an authentication session became inactive.
 *
 * @param inactivated the inactivated detection timestamp
 * @since 0.2.0
 * @author attrigo
 */
public record Inactivated(
        Instant inactivated
) {

    /**
     * Factory method to create an {@code Inactivated} with current timestamp.
     *
     * @return a new {@code Inactivated} instance with current time
     */
    public static Inactivated now() {
        return new Inactivated(Instant.now());
    }

    /**
     * Factory method to create an {@code Inactivated} from an {@link Instant}.
     *
     * @param inactivated the instant timestamp
     * @return a new {@code Inactivated} instance
     * @throws IllegalArgumentException if instant is {@code null}
     */
    public static Inactivated of(Instant inactivated) {
        return new Inactivated(inactivated);
    }

    /**
     * Factory method to create an {@code Inactivated} from a nullable {@link Instant}.
     * <p>
     * Returns {@code null} if the instant is {@code null}.
     *
     * @param inactivated the instant timestamp, or {@code null}
     * @return a new {@code Inactivated} instance, or {@code null} if instant is {@code null}
     */
    public static Inactivated ofNullable(Instant inactivated) {
        return inactivated == null ? null : new Inactivated(inactivated);
    }

    /**
     * Returns the inactivated instant value.
     *
     * @return the {@link Instant} when the authentication became inactive
     */
    public Instant value() {
        return this.inactivated;
    }

}
