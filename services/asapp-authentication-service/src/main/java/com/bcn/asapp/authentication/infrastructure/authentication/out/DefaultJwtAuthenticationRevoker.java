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

package com.bcn.asapp.authentication.infrastructure.authentication.out;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRevoker;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtAuthenticationException;

/**
 * Default implementation of {@link JwtAuthenticationRevoker} for removing JWT authentications.
 * <p>
 * Bridges the application layer with the infrastructure layer, removing JWT tokens from the repository.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class DefaultJwtAuthenticationRevoker implements JwtAuthenticationRevoker {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJwtAuthenticationRevoker.class);

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code AuthenticationRevokerAdapter} with required dependencies.
     *
     * @param jwtAuthenticationRepository the JWT authentication repository
     */
    public DefaultJwtAuthenticationRevoker(JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Revokes a JWT authentication session.
     * <p>
     * Removes the authentication and its associated tokens from the system, effectively invalidating the session.
     *
     * @param authentication the {@link JwtAuthentication} to revoke
     * @throws InvalidJwtAuthenticationException if revocation fails
     */
    @Override
    public void revokeAuthentication(JwtAuthentication authentication) {
        logger.trace("Revoking authentication with id {}", authentication.getId());

        try {
            jwtAuthenticationRepository.deleteById(authentication.getId());

        } catch (Exception e) {
            var message = String.format("Authentication could not be revoked due to: %s", e.getMessage());
            logger.warn(message, e);
            throw new InvalidJwtAuthenticationException(message, e);
        }
    }

}
