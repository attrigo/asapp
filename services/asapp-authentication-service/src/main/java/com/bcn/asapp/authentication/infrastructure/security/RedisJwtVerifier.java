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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.out.JwtCacheRepository;

/**
 * Component for verifying JWT tokens with Redis-based validation.
 * <p>
 * Validates signature, expiration, type, and Redis existence for comprehensive token verification.
 *
 * @since 0.3.0
 * @author attrigo
 */
@Component
public class RedisJwtVerifier {

    private static final Logger logger = LoggerFactory.getLogger(RedisJwtVerifier.class);

    private final JwtDecoder jwtDecoder;

    private final JwtCacheRepository jwtCacheRepository;

    /**
     * Constructs a new {@code RedisJwtVerifier} with required dependencies.
     *
     * @param jwtDecoder         the JWT decoder for signature validation
     * @param jwtCacheRepository the cache repository for Redis lookup
     */
    public RedisJwtVerifier(JwtDecoder jwtDecoder, JwtCacheRepository jwtCacheRepository) {
        this.jwtDecoder = jwtDecoder;
        this.jwtCacheRepository = jwtCacheRepository;
    }

    /**
     * Verifies an access token by validating signature, type, and Redis existence.
     * <p>
     * Validation sequence: 1. Decode and validate signature + expiration (fail fast) 2. Validate token type (access vs refresh) 3. Check token exists in Redis
     * (not revoked) 4. Return DecodedJwt with claims from the JWT itself
     *
     * @param accessToken the encoded access token to verify
     * @return the {@link DecodedJwt} containing validated token data from JWT claims
     * @throws InvalidJwtException        if validation fails or token not in cache
     * @throws UnexpectedJwtTypeException if the token is not an access token
     */
    public DecodedJwt verifyAccessToken(String accessToken) {
        logger.trace("Verifying access token with Redis validation");

        try {
            // Step 1: Validate signature and expiration
            var decodedJwt = jwtDecoder.decode(accessToken);

            // Step 2: Validate type
            if (!decodedJwt.isAccessToken()) {
                throw new UnexpectedJwtTypeException(String.format("JWT %s is not an access token", decodedJwt.encodedToken()));
            }

            // Step 3: Check Redis existence (revocation check)
            var tokenExists = jwtCacheRepository.tokenExists(accessToken);

            if (!tokenExists) {
                throw new InvalidJwtException("Token not found in cache (revoked or expired)");
            }

            // Step 4: Return DecodedJwt with JWT's own claims
            return decodedJwt;

        } catch (InvalidJwtException | UnexpectedJwtTypeException e) {
            throw e;
        } catch (Exception e) {
            var message = String.format("Access token is not valid: %s", accessToken);
            logger.warn(message, e);
            throw new InvalidJwtException(message, e);
        }
    }

}
