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

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;

import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

import com.bcn.asapp.authentication.domain.authentication.EncodedToken;

/**
 * Infrastructure component for decoding and validating JWTs.
 * <p>
 * Provides the infrastructure capability to parse and validate JWTs using the Nimbus JOSE+JWT library.
 * <p>
 * Parses JWTs, verifies their signatures using HMAC-SHA, and constructs {@link DecodedJwt} objects.
 *
 * @since 0.2.0
 * @see SignedJWT
 * @author attrigo
 */
@Component
public class JwtDecoder {

    private static final Logger logger = LoggerFactory.getLogger(JwtDecoder.class);

    private final byte[] secretBytes;

    /**
     * Constructs a new {@code JwtDecoder} with the configured secret key.
     *
     * @param jwtSecret the base64-encoded JWT secret from configuration
     */
    public JwtDecoder(@Value("${asapp.security.jwt-secret}") String jwtSecret) {
        this.secretBytes = Base64.getDecoder().decode(jwtSecret);
    }

    /**
     * Decodes and validates an encoded token.
     * <p>
     * Parses the token, verifies its signature and expiration, and constructs a {@link DecodedJwt}.
     *
     * @param encodedToken the {@link EncodedToken} to decode
     * @return the decoded and validated {@link DecodedJwt}
     * @throws RuntimeException if the token is invalid, malformed, or expired
     */
    public DecodedJwt decode(EncodedToken encodedToken) {
        return decode(encodedToken.value());
    }

    /**
     * Decodes and validates an encoded JWT string.
     * <p>
     * Parses the token, verifies its signature and expiration, and constructs a {@link DecodedJwt}.
     *
     * @param token the encoded token string to decode
     * @return the decoded and validated {@link DecodedJwt}
     * @throws RuntimeException if the token is invalid, malformed, or expired
     */
    public DecodedJwt decode(String token) {
        logger.debug("[JWT_DECODER] Decoding token");

        var signedJwt = parseToken(token);

        return buildDecodedJwt(token, signedJwt);
    }

    /**
     * Parses and verifies the token signature.
     *
     * @param token the token to parse
     * @return the parsed {@link SignedJWT}
     * @throws RuntimeException if parsing or signature verification fails
     */
    private SignedJWT parseToken(String token) {
        try {
            logger.trace("[JWT_DECODER] Step 1/3: Parsing JWT string and verifying signature");
            var signedJwt = SignedJWT.parse(token);
            if (!signedJwt.verify(new MACVerifier(secretBytes))) {
                throw new JOSEException("JWT signature verification failed");
            }
            return signedJwt;
        } catch (ParseException e) {
            throw new RuntimeException("Malformed JWT token", e);
        } catch (JOSEException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Builds a {@link DecodedJwt} from a parsed and signature-verified {@link SignedJWT}.
     *
     * @param token     the original encoded token string
     * @param signedJwt the parsed and verified {@link SignedJWT}
     * @return the constructed {@link DecodedJwt} object
     * @throws RuntimeException if claims extraction or expiration check fails
     */
    private DecodedJwt buildDecodedJwt(String token, SignedJWT signedJwt) {
        try {
            logger.trace("[JWT_DECODER] Step 2/3: Extracting header and payload");
            var type = signedJwt.getHeader().getType().getType();
            var claimsSet = signedJwt.getJWTClaimsSet();

            logger.trace("[JWT_DECODER] Step 3/3: Validating expiration and creating DecodedJwt");
            if (claimsSet.getExpirationTime() != null && claimsSet.getExpirationTime().before(new Date())) {
                throw new RuntimeException("JWT has expired");
            }

            var subject = claimsSet.getSubject();
            var tokenUseClaim = (String) claimsSet.getClaim(TOKEN_USE);
            var roleClaim = (String) claimsSet.getClaim(ROLE);
            var claims = Map.<String, Object>of(TOKEN_USE, tokenUseClaim, ROLE, roleClaim);

            return new DecodedJwt(token, type, subject, claims);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse JWT claims", e);
        }
    }

}
