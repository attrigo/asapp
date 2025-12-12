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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.out.CredentialsAuthenticator;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;
import com.bcn.asapp.authentication.infrastructure.security.CustomUserDetails;
import com.bcn.asapp.authentication.infrastructure.security.InvalidPrincipalException;
import com.bcn.asapp.authentication.infrastructure.security.RoleNotFoundException;

/**
 * Adapter implementation of {@link CredentialsAuthenticator} using Spring Security.
 * <p>
 * Bridges the application layer with Spring Security's authentication mechanism, validating user credentials and extracting authenticated user information.
 * <p>
 * This adapter translates between the domain model ({@link UserAuthentication}) and Spring Security's authentication framework ({@link AuthenticationManager}).
 *
 * @since 0.2.0
 * @see AuthenticationManager
 * @author attrigo
 */
@Component
public class SpringCredentialsAuthenticator implements CredentialsAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(SpringCredentialsAuthenticator.class);

    private final AuthenticationManager authenticationManager;

    /**
     * Constructs a new {@code SpringCredentialsAuthenticator} with required dependencies.
     *
     * @param authenticationManager the Spring Security authentication manager
     */
    public SpringCredentialsAuthenticator(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Authenticates a user based on provided credentials.
     * <p>
     * Validates the user's credentials using Spring Security, then extracts and returns an authenticated user with identity and role information.
     *
     * @param username the {@link Username} to authenticate
     * @param password the {@link RawPassword} to validate
     * @return the {@link UserAuthentication} containing authenticated user data with ID and role
     * @throws BadCredentialsException if authentication fails
     */
    @Override
    public UserAuthentication authenticate(Username username, RawPassword password) {
        logger.trace("Authenticating user {}", username);

        try {
            var authenticationTokenRequest = UsernamePasswordAuthenticationToken.unauthenticated(username.value(), password.value());

            var authenticationToken = authenticationManager.authenticate(authenticationTokenRequest);

            var authenticatedUserId = extractUserIdFromAuthentication(authenticationToken);
            var authenticatedUsername = Username.of(authenticationToken.getName());
            var authenticatedRole = extractRoleFromAuthentication(authenticationToken);

            return UserAuthentication.authenticated(authenticatedUserId, authenticatedUsername, authenticatedRole);

        } catch (Exception e) {
            var message = String.format("Authentication failed due to: %s", e.getMessage());
            logger.warn(message, e);
            throw new BadCredentialsException(message, e);
        }
    }

    /**
     * Extracts the user ID from the authentication token principal.
     *
     * @param authenticationToken the Spring Security authentication token
     * @return the {@link UserId} extracted from the principal
     * @throws InvalidPrincipalException if the principal does not contain a user ID
     */
    private UserId extractUserIdFromAuthentication(Authentication authenticationToken) {
        var principal = authenticationToken.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            var userId = userDetails.getUserId();
            return UserId.of(userId);
        }
        throw new InvalidPrincipalException("Authentication principal must contain the ID of the user");
    }

    /**
     * Extracts the user role from the authentication token authorities.
     *
     * @param authenticationToken the Spring Security authentication token
     * @return the {@link Role} extracted from the authorities
     * @throws RoleNotFoundException if no role is found in the authorities
     */
    private Role extractRoleFromAuthentication(Authentication authenticationToken) {
        return authenticationToken.getAuthorities()
                                  .stream()
                                  .findFirst()
                                  .map(GrantedAuthority::getAuthority)
                                  .map(Role::valueOf)
                                  .orElseThrow(() -> new RoleNotFoundException("Authentication authorities must contain at least one role"));
    }

}
