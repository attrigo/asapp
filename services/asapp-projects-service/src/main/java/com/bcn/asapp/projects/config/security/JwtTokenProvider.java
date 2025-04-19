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
package com.bcn.asapp.projects.config.security;

import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

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

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;

    /**
     * Main constructor.
     *
     * @param jwtSecret the jwt secret used to sign the JWT (from application properties).
     */
    public JwtTokenProvider(@Value("${asapp.jwt-secret}") String jwtSecret) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /**
     * Extracts the username (subject) from the provided JWT.
     *
     * @param token the JWT, must not be {@literal null}.
     * @return an {@link Optional} containing the username (subject) if present, or an empty {@link Optional} if not.
     * @throws IllegalArgumentException if the token is null or empty.
     */
    public Optional<String> getUsername(String token) {
        Assert.hasText(token, "Token must not be null or empty");

        var subject = Jwts.parser()
                          .verifyWith(secretKey)
                          .build()
                          .parseSignedClaims(token)
                          .getPayload()
                          .getSubject();

        return Optional.ofNullable(subject);
    }

    /**
     * Extracts the list of authorities (roles) from the provided JWT.
     *
     * @param token the JWT, must not be {@literal null}.
     * @return a list containing the role associated with the token, or an empty list if no role claim is found.
     * @throws IllegalArgumentException if the token is null or empty.
     */
    public List<String> getAuthorities(String token) {
        Assert.hasText(token, "Token must not be null or empty");

        var roleClaim = Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload()
                            .get("role");

        if (roleClaim != null) {
            return List.of(roleClaim.toString());
        }

        return List.of();
    }

    /**
     * Validates the provided JWT by checking its signature.
     *
     * @param token the JWT to validate.
     * @return true if the token is valid, otherwise false.
     */
    public boolean validateToken(String token) {

        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

            return true;

        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }

}
