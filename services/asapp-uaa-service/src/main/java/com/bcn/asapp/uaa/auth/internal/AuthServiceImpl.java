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
package com.bcn.asapp.uaa.auth.internal;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.bcn.asapp.uaa.auth.AccessTokenDTO;
import com.bcn.asapp.uaa.auth.AuthService;
import com.bcn.asapp.uaa.auth.JwtAuthenticationDTO;
import com.bcn.asapp.uaa.auth.RefreshTokenDTO;
import com.bcn.asapp.uaa.auth.UserCredentialsDTO;
import com.bcn.asapp.uaa.security.authentication.issuer.JwtIssuer;
import com.bcn.asapp.uaa.security.authentication.revoker.JwtRevoker;
import com.bcn.asapp.uaa.security.authentication.verifier.AccessTokenVerifier;
import com.bcn.asapp.uaa.security.authentication.verifier.RefreshTokenVerifier;
import com.bcn.asapp.uaa.security.core.JwtAuthentication;

/**
 * Standard implementation of the {@link AuthService} responsible for user authentication and JWT management operations.
 *
 * @author ttrigo
 * @see AuthenticationManager
 * @see SecurityContextHolder
 * @since 0.2.0
 */
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * Authentication manager used to authenticate user credentials.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Verifier responsible for validating access tokens.
     */
    private final AccessTokenVerifier accessTokenVerifier;

    /**
     * Verifier responsible for validating refresh tokens.
     */
    private final RefreshTokenVerifier refreshTokenVerifier;

    /**
     * Issuer responsible for issuing new JWT authentication tokens.
     */
    private final JwtIssuer jwtIssuer;

    /**
     * Revoker responsible for revoking JWT authentication tokens.
     */
    private final JwtRevoker jwtRevoker;

    /**
     * Constructs a new {@code AuthServiceImpl} with the specified dependencies.
     *
     * @param authenticationManager the authentication manager used to authenticate user credentials
     * @param accessTokenVerifier   the verifier used to validate refresh tokens
     * @param refreshTokenVerifier  the verifier used to validate refresh tokens
     * @param jwtIssuer             the issuer responsible for generating new JWT authentication tokens
     * @param jwtRevoker            the revoker used to invalidate JWT authentication tokens
     */
    public AuthServiceImpl(AuthenticationManager authenticationManager, AccessTokenVerifier accessTokenVerifier, RefreshTokenVerifier refreshTokenVerifier,
            JwtIssuer jwtIssuer, JwtRevoker jwtRevoker) {

        this.authenticationManager = authenticationManager;
        this.accessTokenVerifier = accessTokenVerifier;
        this.refreshTokenVerifier = refreshTokenVerifier;
        this.jwtIssuer = jwtIssuer;
        this.jwtRevoker = jwtRevoker;
    }

    /**
     * Authenticates a user using their credentials and issues new JWT authentication tokens.
     *
     * @param userCredentials the credentials of the user to authenticate
     * @return a {@link JwtAuthenticationDTO} containing newly issued access and refresh tokens
     */
    @Override
    public JwtAuthenticationDTO authenticate(UserCredentialsDTO userCredentials) {
        var authenticationRequest = new UsernamePasswordAuthenticationToken(userCredentials.username(), userCredentials.password());
        var authentication = authenticationManager.authenticate(authenticationRequest);

        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);

        var newAuthentication = jwtIssuer.issueAuthentication(authentication);

        return buildAuthenticationDTO(newAuthentication);
    }

    /**
     * Refreshes JWT authentication tokens using a valid refresh token.
     *
     * @param refreshToken the refresh token DTO containing the JWT to verify
     * @return a {@link JwtAuthenticationDTO} containing newly issued tokens
     */
    @Override
    public JwtAuthenticationDTO refreshToken(RefreshTokenDTO refreshToken) {
        var authentication = refreshTokenVerifier.verify(refreshToken.jwt());

        var newAuthentication = jwtIssuer.issueAuthentication(authentication);

        return buildAuthenticationDTO(newAuthentication);
    }

    /**
     * Revokes the JWT authentication for a user by invalidating both access and refresh tokens using the provided access token.
     * <p>
     * Verifies the provided access token, and if valid, proceeds to revoke both the access token and any associated refresh token for the user.
     *
     * @param accessToken the access token used to invalidate the JWT authentication
     */
    @Override
    public void revokeAuthentication(AccessTokenDTO accessToken) {
        var authentication = accessTokenVerifier.verify(accessToken.jwt());

        jwtRevoker.revokeAuthentication(authentication);
    }

    /**
     * Builds a {@link JwtAuthenticationDTO} from a {@link JwtAuthentication} object.
     *
     * @param jwtAuthentication the JWT authentication containing access and refresh tokens
     * @return a DTO encapsulating the tokens
     */
    private JwtAuthenticationDTO buildAuthenticationDTO(JwtAuthentication jwtAuthentication) {
        var newAccessToken = new AccessTokenDTO(jwtAuthentication.accessToken()
                                                                 .jwt());
        var newRefreshToken = new RefreshTokenDTO(jwtAuthentication.refreshToken()
                                                                   .jwt());
        return new JwtAuthenticationDTO(newAccessToken, newRefreshToken);
    }

}
