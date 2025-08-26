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

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record Jwt(
        String token,
        JwtType type,
        String subject,
        Map<String, Object> claims,
        Instant issuedAt,
        Instant expiresAt
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
    public static final String TOKEN_USE_ACCESS_CLAIM_VALUE = "access";

    /**
     * Claim value indicating refresh token usage.
     */
    public static final String TOKEN_USE_REFRESH_CLAIM_VALUE = "refresh";

    public Jwt {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException("Token could not be null or empty");
        }
        if (type == null) {
            throw new InvalidJwtException("Type could not be null");
        }
        if (subject == null || subject.isBlank()) {
            throw new InvalidJwtException("Subject could not be null");
        }
        if (claims == null || claims.isEmpty()) {
            throw new InvalidJwtException("Claims could not be null or empty");
        }
        var optionalTokenUse = getClaim(TOKEN_USE_CLAIM_NAME, String.class);
        if (optionalTokenUse.isEmpty()) {
            throw new InvalidJwtException("Claims must contain the mandatory token use claim");
        }
        if (!TOKEN_USE_ACCESS_CLAIM_VALUE.equals(optionalTokenUse.get()) && !TOKEN_USE_REFRESH_CLAIM_VALUE.equals(optionalTokenUse.get())) {
            throw new InvalidJwtException("Invalid JWT token use claim, expected " + TOKEN_USE_ACCESS_CLAIM_VALUE + " or " + TOKEN_USE_REFRESH_CLAIM_VALUE
                    + " but was " + optionalTokenUse.get());
        }
        if (issuedAt == null) {
            throw new InvalidJwtException("Issued at timestamp could not be null");
        }
        if (expiresAt == null) {
            throw new InvalidJwtException("Expires at timestamp could not be null");
        }
        if (expiresAt.isBefore(issuedAt)) {
            throw new InvalidJwtException("Expires at timestamp must be after issued at timestamp");
        }
    }

    public Boolean isAccessToken() {
        var optionalTokenUse = getClaim(TOKEN_USE_CLAIM_NAME, String.class);

        return ACCESS_TOKEN.equals(this.type) && optionalTokenUse.isPresent() && TOKEN_USE_ACCESS_CLAIM_VALUE.equals(optionalTokenUse.get());
    }

    public Boolean isRefreshToken() {
        var optionalTokenUse = getClaim(TOKEN_USE_CLAIM_NAME, String.class);

        return REFRESH_TOKEN.equals(this.type) && optionalTokenUse.isPresent() && TOKEN_USE_REFRESH_CLAIM_VALUE.equals(optionalTokenUse.get());
    }

    // TODO: Review why IntelliJ warns about possible NPE
    public <T> Optional<T> getClaim(String claimName, Class<T> requiredType) {
        var claimValue = this.claims.get(claimName);
        if (requiredType.isInstance(claimValue)) {
            var castedClaim = requiredType.cast(claimValue);
            return Optional.of(castedClaim);
        }
        return Optional.empty();
    }

}
