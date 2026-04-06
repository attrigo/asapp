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
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Infrastructure component for decoding and validating JWTs using the Nimbus JOSE+JWT library.
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

    private final MACVerifier macVerifier;

    /**
     * Constructs a new {@code JwtDecoder} with the configured signature verifier.
     *
     * @param macVerifier the {@link MACVerifier} used to verify token signatures
     */
    public JwtDecoder(MACVerifier macVerifier) {
        this.macVerifier = macVerifier;
    }

    /**
     * Decodes and validates an encoded JWT string.
     * <p>
     * Parses the token, verifies its signature and expiration, and constructs a {@link DecodedJwt}.
     *
     * @param token the encoded token string to decode
     * @return the decoded and validated {@link DecodedJwt}
     * @throws JwtDecodeException if the token is invalid, malformed, or expired
     */
    public DecodedJwt decode(String token) {
        logger.debug("[JWT_DECODER] Decoding token");

        var parsedToken = parseToken(token);
        verifyToken(parsedToken);
        return buildDecodedJwt(token, parsedToken.header(), parsedToken.claimsSet());
    }

    /**
     * Parses a JWT string into a {@link ParsedToken}.
     *
     * @param token the encoded token string to parse
     * @return the parsed {@link ParsedToken} holding the signed JWT, header, and claims set
     * @throws JwtDecodeException if the token string is malformed
     */
    private ParsedToken parseToken(String token) {
        try {
            logger.trace("[JWT_DECODER] Step 1/5: Parsing JWT string");
            var signedJwt = SignedJWT.parse(token);

            return new ParsedToken(signedJwt, signedJwt.getHeader(), signedJwt.getJWTClaimsSet());

        } catch (ParseException e) {
            throw new JwtDecodeException("Malformed JWT token", e);
        }
    }

    /**
     * Verifies the signature and expiration of a parsed JWT.
     *
     * @param parsedToken the parsed JWT to verify
     * @throws JwtDecodeException if signature verification or expiration validation fails
     */
    private void verifyToken(ParsedToken parsedToken) {
        verifySignature(parsedToken.signedJwt);
        validateExpiration(parsedToken.claimsSet);
    }

    /**
     * Verifies the HMAC signature of a parsed JWT.
     *
     * @param signedJwt the parsed JWT whose signature to verify
     * @throws JwtDecodeException if the signature is invalid or verification fails
     */
    private void verifySignature(SignedJWT signedJwt) {
        try {
            logger.trace("[JWT_DECODER] Step 2/5: Verifying JWT signature");
            if (!signedJwt.verify(macVerifier)) {
                throw new JwtDecodeException("JWT signature verification failed");
            }
        } catch (JOSEException e) {
            throw new JwtDecodeException(e.getMessage(), e);
        }
    }

    /**
     * Validates that the JWT claims set carries a non-expired expiration timestamp.
     *
     * @param claimsSet the claims set of a signature-verified JWT
     * @throws JwtDecodeException if the expiration claim is absent or the token has expired
     */
    private void validateExpiration(JWTClaimsSet claimsSet) {
        logger.trace("[JWT_DECODER] Step 3/5: Validating JWT expiration");
        var expirationTime = claimsSet.getExpirationTime();
        if (expirationTime == null || expirationTime.before(new Date())) {
            throw new JwtDecodeException("JWT has expired");
        }
    }

    /**
     * Builds a {@link DecodedJwt} from a verified header and claims set.
     *
     * @param token     the original encoded token string
     * @param header    the verified JWT header
     * @param claimsSet the validated JWT claims set
     * @return the constructed {@link DecodedJwt} object
     */
    private DecodedJwt buildDecodedJwt(String token, JWSHeader header, JWTClaimsSet claimsSet) {
        logger.trace("[JWT_DECODER] Step 4/5: Extracting header and payload");
        var type = header.getType()
                         .getType();
        var subject = claimsSet.getSubject();
        var tokenUseClaim = (String) claimsSet.getClaim(TOKEN_USE);
        var roleClaim = (String) claimsSet.getClaim(ROLE);
        var claims = Map.<String, Object>of(TOKEN_USE, tokenUseClaim, ROLE, roleClaim);

        logger.trace("[JWT_DECODER] Step 5/5: Creating DecodedJwt");
        return new DecodedJwt(token, type, subject, claims);
    }

    /**
     * Holds the signed JWT alongside its eagerly extracted header and claims set.
     */
    private record ParsedToken(
            SignedJWT signedJwt,
            JWSHeader header,
            JWTClaimsSet claimsSet
    ) {}

}
