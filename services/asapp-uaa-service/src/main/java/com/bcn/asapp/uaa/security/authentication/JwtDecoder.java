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

import com.bcn.asapp.uaa.security.core.InvalidJwtException;

/**
 * Decodes and validates JWT (JSON Web Token) strings into {@link DecodedJwt} objects.
 *
 * <p>
 * This component is responsible for parsing JWT strings, verifying their signatures using a secret key, and handling various validation errors that might occur
 * during the process. It serves as a core component in the JWT-based authentication system.
 * <p>
 * The decoder uses HMAC-SHA algorithm with a secret key derived from a Base64-encoded string provided through system properties.
 *
 * @author ttrigo
 * @since 0.2.0
 * @see DecodedJwt
 * @see com.bcn.asapp.uaa.security.core.InvalidJwtException
 * @see io.jsonwebtoken.Jwts
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

    /**
     * Decodes and validates a JWT string.
     * <p>
     * Parses the provided JWT string, verifies its signature using the secret key, and returns a {@link DecodedJwt} object containing the validated token,
     * header, and payload.
     * <p>
     * The method handles various validation errors that might occur during the process and wraps them in an {@link InvalidJwtException} with an appropriate
     * message. The following errors are handled:
     * <ul>
     * <li>SignatureException - when the JWT signature is invalid</li>
     * <li>MalformedJwtException - when the JWT is malformed</li>
     * <li>ExpiredJwtException - when the JWT has expired</li>
     * <li>UnsupportedJwtException - when the JWT format is not supported</li>
     * <li>IllegalArgumentException - when the JWT claims are null or empty</li>
     * <li>JwtException - for any other unexpected errors during JWT verification</li>
     * </ul>
     *
     * @param token The JWT string to decode and validate
     * @return A {@link DecodedJwt} object containing the validated token, header, and payload
     * @throws InvalidJwtException If any validation error occurs during the decoding process
     */
    public DecodedJwt decode(String token) {

        try {
            var jwsClaims = Jwts.parser()
                                .verifyWith(secretKey)
                                .build()
                                .parse(token)
                                .accept(Jws.CLAIMS);

            return new DecodedJwt(token, jwsClaims.getHeader(), jwsClaims.getPayload());

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
