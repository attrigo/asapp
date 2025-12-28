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
 * This adapter performs type translation by converting Spring Security types ({@link Authentication}, {@link GrantedAuthority}) to domain value objects
 * ({@link UserAuthentication}, {@link UserId}, {@link Role}) for authentication operations, using {@link AuthenticationManager} to delegate credential
 * validation while adapting the authentication result to the domain model.
 *
 * @since 0.2.0
 * @see AuthenticationManager
 * @author attrigo
 */
@Component
public class CredentialsAuthenticatorAdapter implements CredentialsAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(CredentialsAuthenticatorAdapter.class);

    private final AuthenticationManager authenticationManager;

    /**
     * Constructs a new {@code CredentialsAuthenticatorAdapter} with required dependencies.
     *
     * @param authenticationManager the Spring Security authentication manager
     */
    public CredentialsAuthenticatorAdapter(AuthenticationManager authenticationManager) {
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
        logger.debug("[CREDENTIALS_AUTH] Authenticating credentials with username={}", username);

        try {
            var authenticationToken = authenticateUsernamePassword(username, password);

            return buildUserAuthentication(authenticationToken);
        } catch (Exception e) {
            var message = String.format("Authentication failed due to: %s", e.getMessage());
            logger.warn("[CREDENTIALS_AUTH] {}", message, e);
            throw new BadCredentialsException(message, e);
        }
    }

    /**
     * Authenticates the username and password using Spring Security's authentication manager.
     *
     * @param username the {@link Username} to authenticate
     * @param password the {@link RawPassword} to validate
     * @return the Spring Security {@link Authentication} token if authentication succeeds
     * @throws BadCredentialsException if the credentials are invalid
     */
    private Authentication authenticateUsernamePassword(Username username, RawPassword password) {
        logger.trace("[CREDENTIALS_AUTH] Step 1/2: Validating credentials with authentication manager");
        var authenticationTokenRequest = UsernamePasswordAuthenticationToken.unauthenticated(username.value(), password.value());
        return authenticationManager.authenticate(authenticationTokenRequest);
    }

    /**
     * Builds a domain {@link UserAuthentication} object from a Spring Security authentication token.
     * <p>
     * Extracts the user ID, username, and role from the authentication token and constructs an authenticated domain object.
     *
     * @param authenticationToken the Spring Security authentication token
     * @return the {@link UserAuthentication} containing user identity and role information
     */
    private UserAuthentication buildUserAuthentication(Authentication authenticationToken) {
        logger.trace("[CREDENTIALS_AUTH] Step 2/2: Building domain authentication object");
        var userId = extractUserIdFromAuthentication(authenticationToken);
        var username = Username.of(authenticationToken.getName());
        var role = extractRoleFromAuthentication(authenticationToken);
        return UserAuthentication.authenticated(userId, username, role);
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
