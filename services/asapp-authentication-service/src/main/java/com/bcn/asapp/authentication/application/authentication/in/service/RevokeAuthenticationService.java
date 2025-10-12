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

package com.bcn.asapp.authentication.application.authentication.in.service;

import com.bcn.asapp.authentication.application.ApplicationService;
import com.bcn.asapp.authentication.application.authentication.in.RevokeAuthenticationUseCase;
import com.bcn.asapp.authentication.application.authentication.out.AuthenticationRevoker;
import com.bcn.asapp.authentication.application.authentication.out.JwtVerifier;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;

/**
 * Application service responsible for orchestrating authentication revocation.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class RevokeAuthenticationService implements RevokeAuthenticationUseCase {

    private final JwtVerifier jwtVerifier;

    private final AuthenticationRevoker authenticationRevoker;

    /**
     * Constructs a new {@code RevokeAuthenticationService} with required dependencies.
     *
     * @param jwtVerifier           the JWT verifier for validating access tokens
     * @param authenticationRevoker the JWT revoker for removing authentications
     */
    public RevokeAuthenticationService(JwtVerifier jwtVerifier, AuthenticationRevoker authenticationRevoker) {
        this.jwtVerifier = jwtVerifier;
        this.authenticationRevoker = authenticationRevoker;
    }

    /**
     * Revokes an authentication using a valid access token.
     * <p>
     * Validates the access token, verifies it, and removes the associated JWT authentication from the system.
     *
     * @param accessToken the access token string
     * @throws IllegalArgumentException if the access token is invalid or blank
     */
    @Override
    public void revokeAuthentication(String accessToken) {
        var rawAccessToken = EncodedToken.of(accessToken);

        var authentication = jwtVerifier.verifyAccessToken(rawAccessToken);

        authenticationRevoker.revokeAuthentication(authentication);
    }

}
