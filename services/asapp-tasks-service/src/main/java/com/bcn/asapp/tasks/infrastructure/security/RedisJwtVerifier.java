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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.bcn.asapp.tasks.infrastructure.security.util.TokenHasher;

@Component
public class RedisJwtVerifier {

    private static final Logger logger = LoggerFactory.getLogger(RedisJwtVerifier.class);

    private static final String ACCESS_TOKEN_PREFIX = "jwt:access:";

    private final JwtDecoder jwtDecoder;

    private final RedisTemplate<String, String> redisTemplate;

    public RedisJwtVerifier(JwtDecoder jwtDecoder, RedisTemplate<String, String> redisTemplate) {
        this.jwtDecoder = jwtDecoder;
        this.redisTemplate = redisTemplate;
    }

    public DecodedJwt verifyAccessToken(String accessToken) {
        logger.trace("Verifying access token with Redis validation");

        try {
            // Step 1: Validate signature and expiration
            var decodedJwt = jwtDecoder.decode(accessToken);

            // Step 2: Validate type
            if (!decodedJwt.isAccessToken()) {
                throw new UnexpectedJwtTypeException(String.format("JWT %s is not an access token", accessToken));
            }

            // Step 3: Check Redis existence (revocation check)
            var key = ACCESS_TOKEN_PREFIX + TokenHasher.hash(accessToken);
            var exists = Boolean.TRUE.equals(redisTemplate.hasKey(key));

            if (!exists) {
                throw new InvalidJwtException("Token not found in cache (revoked or expired)", null);
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
