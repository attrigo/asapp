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

package com.bcn.asapp.authentication.infrastructure.security;

import static com.bcn.asapp.authentication.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;

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

import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.JwtType;
import com.bcn.asapp.authentication.domain.authentication.Subject;

/**
 * Component for decoding and validating JWT tokens.
 * <p>
 * Parses encoded JWT tokens, verifies their signatures using HMAC-SHA, and constructs domain {@link Jwt} objects with validated claims and metadata.
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
     * Parses the token, verifies its signature, and constructs a domain {@link Jwt} object with all claims and metadata.
     *
     * @param encodedToken the {@link EncodedToken} to decode
     * @return the decoded and validated {@link Jwt}
     * @throws io.jsonwebtoken.JwtException if the token is invalid, malformed, or expired
     */
    public Jwt decode(EncodedToken encodedToken) {
        logger.trace("Decoding token {}", encodedToken);

        var jwsClaims = parseToken(encodedToken);

        return buildJwt(encodedToken, jwsClaims);
    }

    /**
     * Parses and verifies the token signature.
     *
     * @param encodedToken the {@link EncodedToken} to parse
     * @return the parsed {@link Jws} containing claims
     * @throws io.jsonwebtoken.JwtException if parsing or verification fails
     */
    private Jws<Claims> parseToken(EncodedToken encodedToken) {
        logger.trace("Parsing token {}", encodedToken);

        return Jwts.parser()
                   .verifyWith(secretKey)
                   .build()
                   .parse(encodedToken.value())
                   .accept(Jws.CLAIMS);
    }

    /**
     * Builds a domain {@link Jwt} from parsed JWT claims.
     *
     * @param encodedToken the original {@link EncodedToken}
     * @param jwsClaims    the parsed JWT claims
     * @return the constructed {@link Jwt} domain object
     */
    private Jwt buildJwt(EncodedToken encodedToken, Jws<Claims> jwsClaims) {
        logger.trace("Building JWT with encoded token {} and claims {}", encodedToken, jwsClaims);

        var tokenHeader = jwsClaims.getHeader();
        var tokenPayload = jwsClaims.getPayload();

        var type = JwtType.ofType(tokenHeader.getType());
        var subject = Subject.of(tokenPayload.getSubject());
        var claims = JwtClaims.of(buildClaimsFromPayload(tokenPayload));
        var issued = Issued.of(tokenPayload.getIssuedAt());
        var expiration = Expiration.of(tokenPayload.getExpiration());

        return Jwt.of(encodedToken, type, subject, claims, issued, expiration);
    }

    /**
     * Extracts custom claims from the JWT payload.
     *
     * @param payload the JWT claims payload
     * @return a {@link Map} containing token_use and role claims
     */
    private Map<String, Object> buildClaimsFromPayload(Claims payload) {
        var tokenUseClaim = payload.get(TOKEN_USE_CLAIM_NAME, String.class);
        var roleClaim = payload.get(ROLE_CLAIM_NAME, String.class);

        return Map.of(TOKEN_USE_CLAIM_NAME, tokenUseClaim, ROLE_CLAIM_NAME, roleClaim);
    }

}
