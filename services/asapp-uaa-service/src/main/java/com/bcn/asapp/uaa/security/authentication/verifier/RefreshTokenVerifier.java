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
package com.bcn.asapp.uaa.security.authentication.verifier;

import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.security.authentication.DecodedJwt;
import com.bcn.asapp.uaa.security.authentication.InvalidRefreshTokenException;
import com.bcn.asapp.uaa.security.authentication.JwtDecoder;
import com.bcn.asapp.uaa.security.authentication.matcher.JwtSessionMatcher;
import com.bcn.asapp.uaa.security.core.JwtType;

/**
 * Verifier implementation for validating JWT refresh tokens.
 * <p>
 * This verifier ensures that the token is of type {@link JwtType#REFRESH_TOKEN} and delegates session matching to the configured {@link JwtSessionMatcher}.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Component
public class RefreshTokenVerifier extends AbstractJwtVerifier {

    /**
     * Constructs a new {@code RefreshTokenVerifier} with the specified JWT decoder and session matcher.
     *
     * @param jwtDecoder                 the decoder used to parse JWTs
     * @param refreshTokenSessionMatcher the matcher used to verify refresh token sessions
     */
    public RefreshTokenVerifier(JwtDecoder jwtDecoder, JwtSessionMatcher refreshTokenSessionMatcher) {
        super(jwtDecoder, refreshTokenSessionMatcher);
    }

    /**
     * Returns the expected token type as {@link JwtType#REFRESH_TOKEN}.
     *
     * @return the refresh token type
     */
    @Override
    protected JwtType getTokenType() {
        return JwtType.REFRESH_TOKEN;
    }

    /**
     * Determines if the decoded JWT is an refresh token.
     *
     * @param decodedJwt the decoded JWT to check
     * @return {@code true} if the token is an refresh token, {@code false} otherwise
     */
    @Override
    protected Boolean isExpectedTokenType(DecodedJwt decodedJwt) {
        return decodedJwt.isRefreshToken();
    }

    /**
     * Creates an {@link InvalidRefreshTokenException} when verification fails.
     *
     * @param cause the underlying cause of the failure
     * @return a new instance of {@code InvalidRefreshTokenException}
     */
    @Override
    protected AuthenticationException createAuthenticationException(Throwable cause) {
        return new InvalidRefreshTokenException("Refresh token is not valid", cause);
    }

}
