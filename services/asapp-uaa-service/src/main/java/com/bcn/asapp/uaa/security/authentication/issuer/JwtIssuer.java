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
package com.bcn.asapp.uaa.security.authentication.issuer;

import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bcn.asapp.uaa.security.authentication.JwtIntegrityViolationException;
import com.bcn.asapp.uaa.security.core.AccessToken;
import com.bcn.asapp.uaa.security.core.AccessTokenRepository;
import com.bcn.asapp.uaa.security.core.JwtAuthentication;
import com.bcn.asapp.uaa.security.core.RefreshToken;
import com.bcn.asapp.uaa.security.core.RefreshTokenRepository;
import com.bcn.asapp.uaa.user.User;
import com.bcn.asapp.uaa.user.UserRepository;

/**
 * Issues and persists JWT-based authentication tokens for authenticated users.
 * <p>
 * All operations are transactional to ensure atomicity and consistency of token and user data.
 * <p>
 * Intended to be used by authentication endpoints or services requiring stateless session issuance.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Component
public class JwtIssuer {

    /**
     * Provides functionality for generating JWT access and refresh tokens.
     */
    private final JwtProvider jwtProvider;

    /**
     * Repository for managing user.
     */
    private final UserRepository userRepository;

    /**
     * Repository for managing access tokens entities.
     */
    private final AccessTokenRepository accessTokenRepository;

    /**
     * Repository for managing refresh tokens entities.
     */
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Constructs a new {@code JwtIssuer} with required components for token generation and persistence.
     *
     * @param jwtProvider            the provider responsible for JWT generation
     * @param userRepository         the repository for managing user entities
     * @param accessTokenRepository  the repository for managing access tokens entities
     * @param refreshTokenRepository the repository for managing refresh tokens entities
     */
    public JwtIssuer(JwtProvider jwtProvider, UserRepository userRepository, AccessTokenRepository accessTokenRepository,
            RefreshTokenRepository refreshTokenRepository) {

        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
        this.accessTokenRepository = accessTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Issues and persists a new {@link JwtAuthentication} for the given {@link Authentication} principal.
     * <p>
     * Generates both an access and refresh token using {@link JwtProvider}, resolves the associated user, and persists the tokens atomically in the database.
     * If tokens already exist, they are updated.
     * <p>
     * This method is annotated with {@link Transactional} to ensure that token generation and persistence occur within a single transactional boundary.
     *
     * @param authentication the authentication requesting token issuance
     * @return a {@link JwtAuthentication} containing the issued access and refresh tokens
     * @throws UsernameNotFoundException      if the user associated with the authentication does not exist
     * @throws JwtIntegrityViolationException if token persistence fails
     */
    @Transactional
    public JwtAuthentication issueAuthentication(Authentication authentication) {
        var user = resolveAuthenticationUser(authentication);

        try {
            var accessToken = issueAccessToken(user, authentication);
            var refreshToken = issueRefreshToken(user, authentication);

            return new JwtAuthentication(accessToken, refreshToken);
        } catch (DbActionExecutionException e) {
            throw new JwtIntegrityViolationException("Authentication could not be issued due to: " + e.getMessage(), e);
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
     * Issues and persists an access token for the specified user and authentication.
     *
     * @param user           the user for whom the token is issued
     * @param authentication the authenticated principal
     * @return the issued {@link AccessToken}
     */
    private AccessToken issueAccessToken(User user, Authentication authentication) {
        var accessToken = jwtProvider.generateAccessToken(authentication);

        return saveAccessToken(user, accessToken);
    }

    /**
     * Issues and persists a new refresh token for the specified user and authentication details.
     *
     * @param user           the user for whom the token is being issued
     * @param authentication the authenticated principal
     * @return the issued {@link RefreshToken}
     */
    private RefreshToken issueRefreshToken(User user, Authentication authentication) {
        var refreshToken = jwtProvider.generateRefreshToken(authentication);

        return saveRefreshToken(user, refreshToken);
    }

    /**
     * Persists an access token for the given user.
     * <p>
     * If a token already exists for the user, it is updated with the new JWT.
     *
     * @param user        the user associated with the token
     * @param accessToken the generated access token to persist
     * @return the persisted {@link AccessToken} entity
     */
    private AccessToken saveAccessToken(User user, AccessToken accessToken) {
        var accessTokenId = accessTokenRepository.findByUserId(user.id())
                                                 .map(AccessToken::id)
                                                 .orElse(null);

        var accessTokenToSave = new AccessToken(accessTokenId, user.id(), accessToken.jwt(), accessToken.createdAt(), accessToken.expiresAt());

        return accessTokenRepository.save(accessTokenToSave);
    }

    /**
     * Persists a refresh token for the given user.
     * <p>
     * If a token already exists for the user, it is updated with the new JWT.
     *
     * @param user         the user associated with the token
     * @param refreshToken the generated refresh token to persist
     * @return the persisted {@link RefreshToken} entity
     */
    private RefreshToken saveRefreshToken(User user, RefreshToken refreshToken) {
        var refreshTokenId = refreshTokenRepository.findByUserId(user.id())
                                                   .map(RefreshToken::id)
                                                   .orElse(null);

        var refreshTokenToSave = new RefreshToken(refreshTokenId, user.id(), refreshToken.jwt(), refreshToken.createdAt(), refreshToken.expiresAt());

        return refreshTokenRepository.save(refreshTokenToSave);
    }

}
