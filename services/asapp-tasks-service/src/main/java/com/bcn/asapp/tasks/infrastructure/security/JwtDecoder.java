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

package com.bcn.asapp.tasks.infrastructure.security;

import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.ROLE_CLAIM_NAME;
import static com.bcn.asapp.tasks.infrastructure.security.DecodedToken.TOKEN_USE_CLAIM_NAME;

import java.util.Map;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Component for decoding and validating JWT tokens.
 * <p>
 * Parses JWT tokens, verifies their signatures using HMAC-SHA, and constructs {@link DecodedToken} objects.
 *
 * @since 0.2.0
 * @see Jwts
 * @author attrigo
 */
@Component
public class JwtDecoder {

    private static final Logger logger = LoggerFactory.getLogger(JwtDecoder.class);

    private final SecretKey secretKey;

    /**
     * Constructs a new {@code JwtDecoder} with the configured secret key.
     *
     * @param jwtSecret the base64-encoded JWT secret from configuration
     */
    public JwtDecoder(@Value("${asapp.security.jwt-secret}") String jwtSecret) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /**
     * Decodes and validates an encoded JWT token.
     * <p>
     * Parses the token, verifies its signature, and constructs a {@link DecodedToken}.
     *
     * @param token the encoded token to decode
     * @return the decoded and validated {@link DecodedToken}
     * @throws io.jsonwebtoken.JwtException if the token is invalid, malformed, or expired
     */
    public DecodedToken decode(String token) {
        logger.trace("Decoding token {}", token);

        var jwsClaims = parseToken(token);

        return buildDecodedToken(token, jwsClaims);
    }

    /**
     * Parses and verifies the token signature.
     *
     * @param token the token to parse
     * @return the parsed {@link Jws} containing claims
     * @throws io.jsonwebtoken.JwtException if parsing or verification fails
     */
    private Jws<Claims> parseToken(String token) {
        logger.trace("Parsing token {}", token);

        return Jwts.parser()
                   .verifyWith(secretKey)
                   .build()
                   .parse(token)
                   .accept(Jws.CLAIMS);
    }

    /**
     * Builds a {@link DecodedToken} from parsed JWT claims.
     *
     * @param token     the original encoded token
     * @param jwsClaims the parsed JWT claims
     * @return the constructed {@link DecodedToken} object
     */
    private DecodedToken buildDecodedToken(String token, Jws<Claims> jwsClaims) {
        logger.trace("Building decoded token with encoded token {} and claims {}", token, jwsClaims);

        var tokenHeader = jwsClaims.getHeader();
        var tokenPayload = jwsClaims.getPayload();

        var type = tokenHeader.getType();
        var subject = tokenPayload.getSubject();
        var tokenUseClaim = tokenPayload.get(TOKEN_USE_CLAIM_NAME, String.class);
        var roleClaim = tokenPayload.get(ROLE_CLAIM_NAME, String.class);
        var claims = Map.of(TOKEN_USE_CLAIM_NAME, tokenUseClaim, ROLE_CLAIM_NAME, roleClaim);

        return new DecodedToken(token, type, subject, claims);
    }

}
