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

import static com.bcn.asapp.uaa.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.uaa.domain.authentication.JwtType.REFRESH_TOKEN;

import com.bcn.asapp.uaa.domain.user.Role;

/**
 * Represents a JSON Web Token (JWT) with complete metadata and claims.
 * <p>
 * This value object encapsulates all essential JWT parts, including the encoded token, type, subject, claims, and temporal information.
 * <p>
 * It enforces structural integrity through validation of type consistency and temporal ordering.
 *
 * @param encodedToken the serialized JWT token
 * @param type         the token type (access or refresh)
 * @param subject      the subject identifier
 * @param claims       the JWT claims
 * @param issued       the issued-at timestamp
 * @param expiration   the expiration timestamp
 * @since 0.2.0
 * @author attrigo
 */
public record Jwt(
        EncodedToken encodedToken,
        JwtType type,
        Subject subject,
        JwtClaims claims,
        Issued issued,
        Expiration expiration
) {

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
     * Constructs a new {@code Jwt} instance and validates its integrity.
     * <p>
     * Validates that all required fields are present, type consistency between JWT type and token_use claim, and temporal ordering of timestamps.
     *
     * @param encodedToken the serialized JWT token
     * @param type         the token type
     * @param subject      the subject identifier
     * @param claims       the JWT claims
     * @param issued       the issued-at timestamp
     * @param expiration   the expiration timestamp
     * @throws IllegalArgumentException if any validation fails
     */
    public Jwt {
        validateEncodedTokenIsNotNull(encodedToken);
        validateTypeIsNotNull(type);
        validateSubjectIsNotNull(subject);
        validateClaimsIsNotNull(claims);
        validateTypeConsistency(type, claims);
        validateTimestampsConsistency(issued, expiration);
    }

    /**
     * Factory method to create a new {@code Jwt} instance.
     *
     * @param encodedToken the serialized JWT token
     * @param type         the token type
     * @param subject      the subject identifier
     * @param claims       the JWT claims
     * @param issuedAt     the issued-at timestamp
     * @param expiresAt    the expiration timestamp
     * @return a new {@code Jwt} instance
     * @throws IllegalArgumentException if any validation fails
     */
    public static Jwt of(EncodedToken encodedToken, JwtType type, Subject subject, JwtClaims claims, Issued issuedAt, Expiration expiresAt) {
        return new Jwt(encodedToken, type, subject, claims, issuedAt, expiresAt);
    }

    /**
     * Checks if this JWT is an access token.
     *
     * @return {@code true} if this is an access token, {@code false} otherwise
     */
    public Boolean isAccessToken() {
        return ACCESS_TOKEN.equals(this.type);
    }

    /**
     * Checks if this JWT is a refresh token.
     *
     * @return {@code true} if this is a refresh token, {@code false} otherwise
     */
    public Boolean isRefreshToken() {
        return REFRESH_TOKEN.equals(this.type);
    }

    /**
     * Returns the encoded token string value.
     *
     * @return the encoded JWT token {@link String}
     */
    public String encodedTokenValue() {
        return this.encodedToken.value();
    }

    /**
     * Extracts the role from the JWT claims.
     *
     * @return the {@link Role} from claims, or {@code null} if not present
     */
    public Role roleClaim() {
        return this.claims.claim(ROLE_CLAIM_NAME, String.class)
                          .map(Role::valueOf)
                          .orElse(null);
    }

    /**
     * Validates that the encoded token is not {@code null}.
     *
     * @param encodedToken the encoded token to validate
     * @throws IllegalArgumentException if the encoded token is {@code null}
     */
    private static void validateEncodedTokenIsNotNull(EncodedToken encodedToken) {
        if (encodedToken == null) {
            throw new IllegalArgumentException("Encoded token must not be null");
        }
    }

    /**
     * Validates that the token type is not {@code null}.
     *
     * @param type the token type to validate
     * @throws IllegalArgumentException if the type is {@code null}
     */
    private static void validateTypeIsNotNull(JwtType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }
    }

    /**
     * Validates that the subject is not {@code null}.
     *
     * @param subject the subject to validate
     * @throws IllegalArgumentException if the subject is {@code null}
     */
    private static void validateSubjectIsNotNull(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject must not be null");
        }
    }

    /**
     * Validates that the claims are not {@code null}.
     *
     * @param claims the claims to validate
     * @throws IllegalArgumentException if the claims are {@code null}
     */
    private static void validateClaimsIsNotNull(JwtClaims claims) {
        if (claims == null) {
            throw new IllegalArgumentException("Claims must not be null");
        }
    }

    /**
     * Validates consistency between JWT type and token_use claim.
     * <p>
     * Ensures the token_use claim is present, valid, and matches the JWT type.
     *
     * @param type   the JWT type
     * @param claims the JWT claims
     * @throws IllegalArgumentException if type and token_use claim are inconsistent
     */
    private static void validateTypeConsistency(JwtType type, JwtClaims claims) {
        var tokenUseValue = claims.claim(TOKEN_USE_CLAIM_NAME, String.class)
                                  .orElseThrow(() -> new IllegalArgumentException("Claims must contain the mandatory token use claim"));

        if (!ACCESS_TOKEN_USE_CLAIM_VALUE.equals(tokenUseValue) && !REFRESH_TOKEN_USE_CLAIM_VALUE.equals(tokenUseValue)) {
            var message = String.format("Invalid JWT token use claim, expected %s or %s but was %s", ACCESS_TOKEN_USE_CLAIM_VALUE,
                    REFRESH_TOKEN_USE_CLAIM_VALUE, tokenUseValue);
            throw new IllegalArgumentException(message);
        }

        if (type == ACCESS_TOKEN && !ACCESS_TOKEN_USE_CLAIM_VALUE.equals(tokenUseValue)
                || type == REFRESH_TOKEN && !REFRESH_TOKEN_USE_CLAIM_VALUE.equals(tokenUseValue)) {
            var message = String.format("Token type %s and token_use claim %s do not match", type, tokenUseValue);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates temporal consistency between issued and expiration timestamps.
     * <p>
     * Ensures the issued timestamp precedes the expiration timestamp.
     *
     * @param issued     the issued-at timestamp
     * @param expiration the expiration timestamp
     * @throws IllegalArgumentException if expiration is before issued
     */
    private static void validateTimestampsConsistency(Issued issued, Expiration expiration) {
        if (expiration.value()
                      .isBefore(issued.value())) {
            throw new IllegalArgumentException("Issued date must be before expiration date");
        }
    }

}
