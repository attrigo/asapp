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
package com.bcn.asapp.uaa.security.authentication.revoker;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bcn.asapp.uaa.security.authentication.JwtIntegrityViolationException;
import com.bcn.asapp.uaa.security.authentication.JwtNotFoundException;
import com.bcn.asapp.uaa.security.core.AccessTokenRepository;
import com.bcn.asapp.uaa.security.core.RefreshTokenRepository;
import com.bcn.asapp.uaa.user.User;
import com.bcn.asapp.uaa.user.UserRepository;

/**
 * Component responsible for revoking JWT-based authentication tokens, including both access tokens and refresh tokens, associated with a user.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Component
public class JwtRevoker {

    /**
     * Repository for performing CRUD operations on user entities.
     */
    private final UserRepository userRepository;

    /**
     * Repository for performing CRUD operations on access tokens entities.
     */
    private final AccessTokenRepository accessTokenRepository;

    /**
     * Repository for performing CRUD operations on refresh tokens entities.
     */
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Constructs a new {@link JwtRevoker} with the specified dependencies.
     *
     * @param userRepository         the repository for performing CRUD operations on user entities
     * @param accessTokenRepository  the repository for performing CRUD operations on access tokens entities
     * @param refreshTokenRepository the repository for performing CRUD operations on refresh tokens entities
     */
    public JwtRevoker(UserRepository userRepository, AccessTokenRepository accessTokenRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Revokes the authentication of a user by invalidating both the access and refresh tokens associated with the user.
     * <p>
     * Resolves the {@link User} from the provided {@link Authentication} object and then proceeds to revoke both tokens. If no tokens are found, appropriate
     * exceptions are thrown.
     * <p>
     * This method is annotated with {@link Transactional} to ensure that both token deletions occur within a single transactional boundary.
     *
     * @param authentication the authentication requesting token revocation
     * @throws UsernameNotFoundException      if the user associated with the authentication does not exist
     * @throws JwtIntegrityViolationException if token deletion fails
     */
    @Transactional
    public void revokeAuthentication(Authentication authentication) {
        var user = resolveAuthenticationUser(authentication);

        try {
            revokeAccessToken(user);
            revokeRefreshToken(user);
        } catch (JwtNotFoundException e) {
            throw new JwtIntegrityViolationException("Authentication could not be revoked due to: " + e.getMessage(), e);
        }

    }

    /**
     * Resolves a {@link User} from the username associated with the provided {@link Authentication}.
     *
     * @param authentication the authentication containing the username
     * @return the resolved {@link User} entity
     * @throws UsernameNotFoundException if no user is found for the given username
     */
    private User resolveAuthenticationUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                             .orElseThrow(() -> new UsernameNotFoundException("User not exists by username " + authentication.getName()));
    }

    /**
     * Revokes the access token associated with the given user.
     *
     * @param user the user whose access token should be revoked
     * @throws JwtNotFoundException if no access token is deleted for the user
     */
    private void revokeAccessToken(User user) {
        var deleted = accessTokenRepository.deleteByUserId(user.id());

        if (deleted == 0) {
            throw new JwtNotFoundException("Access token not found for user " + user.username());
        }

    }

    /**
     * Revokes the refresh token associated with the given user.
     *
     * @param user the user whose refresh token should be revoked
     * @throws JwtNotFoundException if no refresh token is deleted for the user
     */
    private void revokeRefreshToken(User user) {
        var deleted = refreshTokenRepository.deleteByUserId(user.id());

        if (deleted == 0) {
            throw new JwtNotFoundException("Refresh tokens not found for user " + user.username());
        }

    }

}
