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
package com.bcn.asapp.uaa.config.security;

import java.util.Date;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.uaa.auth.Role;

/**
 * JWT utility component that provides operations to work with JSON Web Tokens.
 *
 * @author ttrigo
 * @since 0.2.0
 * @see Keys
 * @see SecretKey
 * @see Jwts
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    private final Long jwtExpirationTime;

    /**
     * Main constructor.
     *
     * @param jwtSecret         the jwt secret used to sign the JWT (from application properties).
     * @param jwtExpirationTime the jwt expiration time in milliseconds (from application properties).
     */
    public JwtTokenProvider(@Value("${asapp.jwt-secret}") String jwtSecret, @Value("${asapp.jwt-expiration-time}") Long jwtExpirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.jwtExpirationTime = jwtExpirationTime;
    }

    /**
     * Generates a JWT token for the given authenticated user.
     * <p>
     * The token contains the username and role as claims, and has an expiration time set based on the configured expiration date.
     *
     * @param authentication the authentication object containing user details, must not be {@literal null}.
     * @return a signed JWT.
     * @throws IllegalArgumentException if the authentication is null, its username or authorities are null or empty.
     * @throws IllegalArgumentException if the authentication authority is invalid.
     */
    public String generateToken(Authentication authentication) {
        Assert.notNull(authentication, "Authentication must not be null");
        Assert.hasText(authentication.getName(), "Authentication name must not be null or empty");

        var username = authentication.getName();
        var role = authentication.getAuthorities()
                                 .stream()
                                 .findFirst()
                                 .map(GrantedAuthority::getAuthority)
                                 .map(this::mapAuthorityToRole)
                                 .orElseThrow(() -> new IllegalArgumentException("Authentication authorities must not be empty"));

        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);

        return Jwts.builder()
                   .subject(username)
                   .claim("role", role.name())
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .signWith(secretKey)
                   .compact();
    }

    /**
     * Maps the given authority string to a corresponding {@link Role} enum.
     *
     * @param authority the authority string to be mapped to a {@link Role}.
     * @return the {@link Role} enum corresponding to the given authority.
     * @throws IllegalArgumentException if the provided authority does not match any valid {@link Role}.
     */
    private Role mapAuthorityToRole(String authority) {

        try {
            return Role.valueOf(authority);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Authentication authority is not valid");
        }

    }

}
