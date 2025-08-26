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

package com.bcn.asapp.uaa.infrastructure.authentication.core;

import java.time.Instant;
import java.util.Map;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import com.bcn.asapp.uaa.domain.authentication.InvalidJwtException;
import com.bcn.asapp.uaa.domain.authentication.Jwt;
import com.bcn.asapp.uaa.domain.authentication.JwtType;

/**
 * Decodes and validates JWT (JSON Web Token) strings into {@link Jwt} objects.
 * <p>
 * This component is responsible for parsing JWT strings, verifying their signatures using a secret key, and handling various validation errors that might occur
 * during the process. It serves as a core component in the JWT-based authentication system.
 * <p>
 * The decoder uses HMAC-SHA algorithm with a secret key derived from a Base64-encoded string provided through system properties.
 *
 * @author ttrigo
 * @see io.jsonwebtoken.Jwts
 * @since 0.2.0
 */
@Component
public class JwtDecoder {

    private static final Logger logger = LoggerFactory.getLogger(JwtDecoder.class);

    /**
     * Secret key used for JWT signature verification.
     */
    private final SecretKey secretKey;

    /**
     * Constructs a new {@code JwtDecoder} with the specified JWT secret.
     * <p>
     * The constructor initializes the secret key by decoding the provided Base64-encoded JWT secret and creating an HMAC-SHA key for signature verification.
     *
     * @param jwtSecret The Base64-encoded secret key used for JWT signature verification
     */
    public JwtDecoder(@Value("${asapp.security.jwt-secret}") String jwtSecret) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public Jwt decode(String token) {
        try {
            var jwsClaims = Jwts.parser()
                                .verifyWith(secretKey)
                                .build()
                                .parse(token)
                                .accept(Jws.CLAIMS);

            var tokenHeader = jwsClaims.getHeader();
            var tokenPayload = jwsClaims.getPayload();
            var tokenType = JwtType.valueOf(tokenHeader.getType());
            var tokenSubject = tokenPayload.getSubject();
            var tokenClaims = (Map<String, Object>) tokenPayload;
            var tokenIssuedAt = Instant.ofEpochMilli(tokenPayload.getIssuedAt()
                                                                 .getTime());
            var tokenExpiration = Instant.ofEpochMilli(tokenPayload.getExpiration()
                                                                   .getTime());
            return new Jwt(token, tokenType, tokenSubject, tokenClaims, tokenIssuedAt, tokenExpiration);

        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature", e);
            throw new InvalidJwtException("Invalid JWT signature", e);
        } catch (MalformedJwtException e) {
            logger.warn("JWT is malformed", e);
            throw new InvalidJwtException("JWT is malformed", e);
        } catch (ExpiredJwtException e) {
            logger.info("JWT is expired", e);
            throw new InvalidJwtException("JWT is expired", e);
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT is not supported", e);
            throw new InvalidJwtException("JWT is not supported", e);
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims are null or empty", e);
            throw new InvalidJwtException("JWT claims are null or empty", e);
        } catch (JwtException e) {
            logger.error("Unexpected error while verifying JWT", e);
            throw new InvalidJwtException("Error verifying JWT token", e);
        }
    }

}
