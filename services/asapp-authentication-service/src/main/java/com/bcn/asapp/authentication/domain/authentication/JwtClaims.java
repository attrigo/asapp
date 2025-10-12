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

import java.util.Map;
import java.util.Optional;

/**
 * Represents a collection of JWT claims.
 * <p>
 * This value object wraps a map of claims ensuring they are not {@code null} or empty, and provides type-safe claim retrieval.
 * <p>
 * The internal map is immutable to prevent modification.
 *
 * @param claims the map of claim names to claim values
 * @since 0.2.0
 * @author attrigo
 */
public record JwtClaims(
        Map<String, Object> claims
) {

    /**
     * Constructs a new {@code JwtClaims} instance and validates its integrity.
     * <p>
     * Creates an immutable copy of the provided claims map.
     *
     * @param claims the claims map to validate and store
     * @throws IllegalArgumentException if the claims map is {@code null} or empty
     */
    public JwtClaims {
        validateClaims(claims);

        claims = Map.copyOf(claims);
    }

    /**
     * Factory method to create a new {@code JwtClaims} instance from a map.
     *
     * @param claims the map of claims
     * @return a new {@code JwtClaims} instance
     * @throws IllegalArgumentException if the claims map is {@code null} or empty
     */
    public static JwtClaims of(Map<String, Object> claims) {
        return new JwtClaims(claims);
    }

    /**
     * Factory method to create a new {@code JwtClaims} instance from two key-value pairs.
     *
     * @param key1   the first claim key
     * @param value1 the first claim value
     * @param key2   the second claim key
     * @param value2 the second claim value
     * @return a new {@code JwtClaims} instance
     * @throws IllegalArgumentException if any key or value is {@code null}
     */
    public static JwtClaims of(String key1, Object value1, String key2, Object value2) {
        validateKeyValueClaims(key1, value1, key2, value2);

        return new JwtClaims(Map.of(key1, value1, key2, value2));
    }

    /**
     * Retrieves a claim value with type safety.
     * <p>
     * Returns the claim value cast to the required type if present and of the correct type, otherwise returns an {@link Optional#empty}.
     *
     * @param <T>          the expected type of the claim value
     * @param claimName    the name of the claim to retrieve
     * @param requiredType the class of the expected type
     * @return an {@link Optional} containing the claim value if present and of correct type, {@link Optional#empty} otherwise
     */
    public <T> Optional<T> claim(String claimName, Class<T> requiredType) {
        return this.claims.entrySet()
                          .stream()
                          .filter(entry -> entry.getKey()
                                                .equals(claimName))
                          .findFirst()
                          .map(entry -> requiredType.isInstance(entry.getValue()) ? requiredType.cast(entry.getValue()) : null);
    }

    /**
     * Returns the claims map.
     *
     * @return an immutable {@link Map} of claim names to claim values
     */
    public Map<String, Object> value() {
        return this.claims;
    }

    /**
     * Validates that the claims map is not {@code null} or empty.
     *
     * @param claims the claims map to validate
     * @throws IllegalArgumentException if the claims map is {@code null} or empty
     */
    private static void validateClaims(Map<String, Object> claims) {
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("Claim could not be null or empty");
        }
    }

    /**
     * Validates that key-value pairs are not {@code null}.
     *
     * @param key1   the first claim key
     * @param value1 the first claim value
     * @param key2   the second claim key
     * @param value2 the second claim value
     * @throws IllegalArgumentException if any key or value is {@code null}
     */
    private static void validateKeyValueClaims(String key1, Object value1, String key2, Object value2) {
        if (key1 == null || value1 == null || key2 == null || value2 == null) {
            throw new IllegalArgumentException("Claim keys and values must be not null");
        }
    }

}
