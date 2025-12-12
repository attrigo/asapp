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

package com.bcn.asapp.users.infrastructure.security;

import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ROLE;
import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;

import java.util.Map;

import org.springframework.util.Assert;

/**
 * Represents a decoded JWT with its type, subject, and claims.
 * <p>
 * Encapsulates only the required JWT parts to perform the decoding and validation of an JWT.
 *
 * @param encodedToken the serialized JWT
 * @param type         the token type
 * @param subject      the subject
 * @param claims       the JWT claims
 * @since 0.2.0
 * @author attrigo
 */
public record DecodedJwt(
        String encodedToken,
        String type,
        String subject,
        Map<String, Object> claims
) {

    /**
     * Constructs a new {@code DecodedJwt} instance and validates its integrity.
     * <p>
     * Validates that all required fields are present.
     *
     * @param encodedToken the serialized JWT
     * @param type         the token type
     * @param subject      the subject
     * @param claims       the JWT claims
     * @throws IllegalArgumentException if any validation fails
     */
    public DecodedJwt {
        Assert.hasText(encodedToken, "Encoded token must not be blank");
        Assert.hasText(type, "Type must not be blank");
        Assert.hasText(subject, "Subject must not be blank");
        Assert.notEmpty(claims, "Claims must not be empty");
    }

    /**
     * Checks if this decoded JWT is an access token.
     *
     * @return {@code true} if this is an access token, {@code false} otherwise
     */
    public Boolean isAccessToken() {
        return ACCESS_TOKEN_TYPE.equals(this.type) && ACCESS_TOKEN_USE.equals(this.claims.get(TOKEN_USE));
    }

    /**
     * Extracts the role from the JWT claims.
     *
     * @return the role from claims, or {@code null} if neither present nor a {@link String}
     */
    public String roleClaim() {
        return this.claims.get(ROLE) instanceof String roleClaim ? roleClaim : null;
    }

}
