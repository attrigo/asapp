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

package com.bcn.asapp.uaa.security.authentication;

import java.time.Instant;
import java.util.Optional;

import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.RequiredTypeException;

import com.bcn.asapp.uaa.security.core.InvalidJwtException;
import com.bcn.asapp.uaa.user.Role;

/**
 * Represents a decoded JSON Web Token (JWT), encapsulating its raw string, header, and claims (payload).
 * <p>
 * Provides utility methods to determine token type and extract claims.
 *
 * @author ttrigo
 * @since 0.2.0
 */
public class DecodedJwt {

    /**
     * Token type identifier for access tokens.
     */
    public static final String ACCESS_TOKEN_TYPE = "at+jwt";

    /**
     * Token type identifier for refresh tokens.
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
    public static final String TOKEN_USE_ACCESS_CLAIM_VALUE = "access";

    /**
     * Claim value indicating refresh token usage.
     */
    public static final String TOKEN_USE_REFRESH_CLAIM_VALUE = "refresh";

    /**
     * The raw JWT string.
     */
    private final String jwt;

    /**
     * The JWT header.
     */
    private final Header header;

    /**
     * The JWT payload.
     */
    private final Claims payload;

    /**
     * Constructs a new {@code DecodedJwt} instance with the specified JWT string, header, and payload.
     *
     * @param jwt     the raw JWT string
     * @param header  the decoded JWT header
     * @param payload the decoded JWT payload (claims)
     * @throws InvalidJwtException if any of the input parameters are invalid or missing required fields
     */
    public DecodedJwt(String jwt, Header header, Claims payload) {
        this.jwt = validateJwt(jwt);
        this.header = validateJwtHeader(header);
        this.payload = validateJwtPayload(payload);
    }

    /**
     * Determines whether this token is an access token.
     *
     * @return {@code true} if the token is an access token, {@code false} otherwise
     */
    public Boolean isAccessToken() {
        var tokenType = header.getType();
        var tokenUseClaim = payload.get(TOKEN_USE_CLAIM_NAME, String.class);

        return ACCESS_TOKEN_TYPE.equals(tokenType) && TOKEN_USE_ACCESS_CLAIM_VALUE.equals(tokenUseClaim);
    }

    /**
     * Determines whether this token is a refresh token.
     *
     * @return {@code true} if the token is a refresh token, {@code false} otherwise
     */
    public Boolean isRefreshToken() {
        var tokenType = header.getType();
        var tokenUseClaim = payload.get(TOKEN_USE_CLAIM_NAME, String.class);

        return REFRESH_TOKEN_TYPE.equals(tokenType) && TOKEN_USE_REFRESH_CLAIM_VALUE.equals(tokenUseClaim);
    }

    /**
     * Returns the raw JWT string.
     *
     * @return the JWT string
     */
    public String getJwt() {
        return jwt;
    }

    /**
     * Returns the type of the JWT from the header.
     *
     * @return the JWT type, such as {@code at+jwt} or {@code rt+jwt}
     */
    public String getType() {
        return header.getType();
    }

    /**
     * Returns the subject of the JWT from the payload.
     *
     * @return the subject claim value
     */
    public String getSubject() {
        return payload.getSubject();
    }

    /**
     * Returns the role extracted from the JWT claims.
     *
     * @return the {@link Role}
     * @throws IllegalArgumentException if the role claim value is not a valid {@code Role}
     */
    public Role getRole() {
        return Role.valueOf(payload.get(ROLE_CLAIM_NAME, String.class));
    }

    /**
     * Retrieves a specific claim from the JWT payload.
     *
     * @param <T>       the type of the claim value
     * @param claimName the name of the claim
     * @param type      the expected type of the claim value
     * @return an {@link Optional} containing the claim value if present and type-safe, otherwise an {@link Optional#empty()}
     */
    public <T> Optional<T> getClaim(String claimName, Class<T> type) {
        try {
            return Optional.ofNullable(payload.get(claimName, type));
        } catch (RequiredTypeException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the issuance timestamp of the JWT.
     *
     * @return the issuance time as an {@link Instant}
     */
    public Instant getIssuedAt() {
        return Instant.ofEpochMilli(payload.getIssuedAt()
                                           .getTime());
    }

    /**
     * Returns the expiration timestamp of the JWT.
     *
     * @return the expiration time as an {@link Instant}
     */
    public Instant getExpiresAt() {
        return Instant.ofEpochMilli(payload.getExpiration()
                                           .getTime());
    }

    /**
     * Validates the raw JWT string.
     *
     * @param jwt the JWT string to validate
     * @return the validated JWT string
     * @throws InvalidJwtException if the JWT is {@literal null} or empty
     */
    private String validateJwt(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new InvalidJwtException("JWT could not be null or empty");
        }

        return jwt;
    }

    /**
     * Validates the JWT header and its type.
     *
     * @param jwtHeader the JWT header to validate
     * @return the validated {@link Header}
     * @throws InvalidJwtException if the header is {@literal null} or has an invalid type
     */
    private Header validateJwtHeader(Header jwtHeader) {
        if (jwtHeader == null) {
            throw new InvalidJwtException("Header could not be null");
        }

        var jwtType = jwtHeader.getType();

        if (!ACCESS_TOKEN_TYPE.equals(jwtType) && !REFRESH_TOKEN_TYPE.equals(jwtType)) {
            throw new InvalidJwtException("Invalid JWT type, expected " + ACCESS_TOKEN_TYPE + " or " + REFRESH_TOKEN_TYPE + " but was " + jwtType);
        }

        return jwtHeader;
    }

    /**
     * Validates the JWT payload and its required claims.
     *
     * @param jwtPayload the JWT payload to validate
     * @return the validated {@link Claims}
     * @throws InvalidJwtException if the payload is {@literal null} or the required claims are missing or invalid
     */
    private Claims validateJwtPayload(Claims jwtPayload) {
        if (jwtPayload == null) {
            throw new InvalidJwtException("Payload could not be null");
        }

        var subject = jwtPayload.getSubject();
        var roleClaim = jwtPayload.get(ROLE_CLAIM_NAME, String.class);
        var tokenUseClaim = jwtPayload.get(TOKEN_USE_CLAIM_NAME, String.class);
        var issuedAt = jwtPayload.getIssuedAt();
        var expiresAt = jwtPayload.getExpiration();

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(roleClaim) || !StringUtils.hasText(tokenUseClaim) || issuedAt == null || expiresAt == null) {
            throw new InvalidJwtException("JWT does not contain the mandatory claims");
        }

        if (!TOKEN_USE_ACCESS_CLAIM_VALUE.equals(tokenUseClaim) && !TOKEN_USE_REFRESH_CLAIM_VALUE.equals(tokenUseClaim)) {
            throw new InvalidJwtException("Invalid JWT token use claim, expected " + TOKEN_USE_ACCESS_CLAIM_VALUE + " or " + TOKEN_USE_REFRESH_CLAIM_VALUE
                    + " but was " + tokenUseClaim);
        }

        return jwtPayload;
    }

}
