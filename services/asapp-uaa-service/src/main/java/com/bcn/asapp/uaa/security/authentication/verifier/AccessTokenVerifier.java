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
import com.bcn.asapp.uaa.security.authentication.InvalidAccessTokenException;
import com.bcn.asapp.uaa.security.authentication.JwtDecoder;
import com.bcn.asapp.uaa.security.authentication.matcher.JwtSessionMatcher;
import com.bcn.asapp.uaa.security.core.JwtType;

/**
 * Verifier implementation for validating JWT access tokens.
 * <p>
 * This verifier ensures that the token is of type {@link JwtType#ACCESS_TOKEN} and delegates session matching to the configured {@link JwtSessionMatcher}.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Component
public class AccessTokenVerifier extends AbstractJwtVerifier {

    /**
     * Constructs a new {@code AccessTokenVerifier} with the specified JWT decoder and session matcher.
     *
     * @param jwtDecoder                the decoder used to parse JWTs
     * @param accessTokenSessionMatcher the matcher used to verify access token sessions
     */
    public AccessTokenVerifier(JwtDecoder jwtDecoder, JwtSessionMatcher accessTokenSessionMatcher) {
        super(jwtDecoder, accessTokenSessionMatcher);
    }

    /**
     * Returns the expected token type as {@link JwtType#ACCESS_TOKEN}.
     *
     * @return the access token type
     */
    @Override
    protected JwtType getTokenType() {
        return JwtType.ACCESS_TOKEN;
    }

    /**
     * Determines if the decoded JWT is an access token.
     *
     * @param decodedJwt the decoded JWT to check
     * @return {@code true} if the token is an access token, {@code false} otherwise
     */
    @Override
    protected Boolean isExpectedTokenType(DecodedJwt decodedJwt) {
        return decodedJwt.isAccessToken();
    }

    /**
     * Creates an {@link InvalidAccessTokenException} when verification fails.
     *
     * @param cause the underlying cause of the failure
     * @return a new instance of {@code InvalidAccessTokenException}
     */
    @Override
    protected AuthenticationException createAuthenticationException(Throwable cause) {
        return new InvalidAccessTokenException("Access token is not valid", cause);
    }

}
