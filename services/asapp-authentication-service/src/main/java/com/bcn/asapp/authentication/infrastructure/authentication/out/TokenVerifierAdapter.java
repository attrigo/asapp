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

import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.out.TokenVerifier;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.infrastructure.security.JwtVerifier;

/**
 * Adapter implementation of {@link TokenVerifier} for delegation-based token verification.
 * <p>
 * Bridges the application layer with the infrastructure layer by delegating token verification to {@link JwtVerifier} while discarding the decoded JWT data
 * returned by the infrastructure component.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class TokenVerifierAdapter implements TokenVerifier {

    private final JwtVerifier jwtVerifier;

    /**
     * Constructs a new {@code TokenVerifierAdapter} with required dependencies.
     *
     * @param jwtVerifier the JWT verifier for performing token verification
     */
    public TokenVerifierAdapter(JwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;
    }

    /**
     * Verifies an access token by delegating to {@link JwtVerifier}.
     *
     * @param encodedToken the {@link EncodedToken} to verify
     * @throws UnexpectedJwtTypeException      if the token is not an access token
     * @throws AuthenticationNotFoundException if the authentication session is not found
     */
    @Override
    public void verifyAccessToken(EncodedToken encodedToken) {
        jwtVerifier.verifyAccessToken(encodedToken);
    }

    /**
     * Verifies a refresh token by delegating to {@link JwtVerifier}.
     *
     * @param encodedToken the {@link EncodedToken} to verify
     * @throws UnexpectedJwtTypeException      if the token is not a refresh token
     * @throws AuthenticationNotFoundException if the authentication session is not found
     */
    @Override
    public void verifyRefreshToken(EncodedToken encodedToken) {
        jwtVerifier.verifyRefreshToken(encodedToken);
    }

}
