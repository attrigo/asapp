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

package com.bcn.asapp.uaa.domain.authentication;

import java.util.Arrays;

/**
 * Defines the supported JWT (JSON Web Token) types within the authentication system.
 * <p>
 * Each type has an associated string identifier following JWT best practices for token type discrimination.
 *
 * @since 0.2.0
 * @author attrigo
 */
public enum JwtType {

    /**
     * Access token type for authorizing API requests.
     * <p>
     * Type identifier: {@code at+jwt}
     */
    ACCESS_TOKEN("at+jwt"),

    /**
     * Refresh token type for getting new access tokens.
     * <p>
     * Type identifier: {@code rt+jwt}
     */
    REFRESH_TOKEN("rt+jwt");

    private final String type;

    /**
     * Constructs a new {@code JwtType} with the specified type identifier.
     *
     * @param type the string identifier for this JWT type
     */
    JwtType(String type) {
        this.type = type;
    }

    /**
     * Returns the string identifier for this JWT type.
     *
     * @return the type identifier
     */
    public String type() {
        return this.type;
    }

    /**
     * Resolves a {@code JwtType} from its string identifier.
     *
     * @param type the string identifier to resolve
     * @return the matching {@link JwtType}
     * @throws IllegalArgumentException if no matching type is found
     */
    public static JwtType ofType(String type) {
        return Arrays.stream(JwtType.values())
                     .filter(jwtType -> jwtType.type()
                                               .equals(type))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("Invalid JWT type: " + type));
    }

}
