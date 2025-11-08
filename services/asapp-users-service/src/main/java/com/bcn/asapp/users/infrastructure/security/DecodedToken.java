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

import java.util.Map;

import org.springframework.util.Assert;

/**
 * Represents a decoded JWT token with its type, subject, and claims.
 * <p>
 * Encapsulates only the required JWT parts to perform the decoding and validation of an JWT.
 *
 * @param encodedToken the serialized JWT token
 * @param type         the token type
 * @param subject      the subject
 * @param claims       the JWT claims
 * @since 0.2.0
 * @author attrigo
 */
public record DecodedToken(
        String encodedToken,
        String type,
        String subject,
        Map<String, Object> claims
) {

    /**
     * Access token type for authorizing API requests.
     */
    public static final String ACCESS_TOKEN_TYPE = "at+jwt";

    /**
     * Refresh token type for authorizing API requests.
     */
    public static final String REFRESH_TOKEN_TYPE = "rt+jwt";

    /**
     * Claim name for the role.
     */
    public static final String ROLE_CLAIM_NAME = "role";

    /**
     * Claim name indicating token usage.
     */
    public static final String TOKEN_USE_CLAIM_NAME = "token_use";

    /**
     * Claim value indicating access token usage.
     */
    public static final String ACCESS_TOKEN_USE_CLAIM_VALUE = "access";

    /**
     * Claim value indicating refresh token usage.
     */
    public static final String REFRESH_TOKEN_USE_CLAIM_VALUE = "refresh";

    /**
     * Constructs a new {@code DecodedToken} instance and validates its integrity.
     * <p>
     * Validates that all required fields are present.
     *
     * @param encodedToken the serialized JWT token
     * @param type         the token type
     * @param subject      the subject
     * @param claims       the JWT claims
     * @throws IllegalArgumentException if any validation fails
     */
    public DecodedToken {
        Assert.hasText(encodedToken, "Encoded token must not be blank");
        Assert.hasText(type, "Type must not be blank");
        Assert.hasText(subject, "Subject must not be blank");
        Assert.notEmpty(claims, "Claim must not be empty");
    }

    /**
     * Checks if this decoded token is an access token.
     *
     * @return {@code true} if this is an access token, {@code false} otherwise
     */
    public Boolean isAccessToken() {
        return ACCESS_TOKEN_TYPE.equals(this.type) && ACCESS_TOKEN_USE_CLAIM_VALUE.equals(this.claims.get(TOKEN_USE_CLAIM_NAME));
    }

    /**
     * Extracts the role from the JWT claims.
     *
     * @return the role from claims, or {@code null} if neither present nor a {@link String}
     */
    public String roleClaim() {
        return this.claims.get(ROLE_CLAIM_NAME) instanceof String roleClaim ? roleClaim : null;
    }

}
