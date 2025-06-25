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

import com.bcn.asapp.uaa.security.authentication.DecodedJwt;
import com.bcn.asapp.uaa.security.authentication.JwtAuthenticationToken;
import com.bcn.asapp.uaa.security.authentication.JwtDecoder;
import com.bcn.asapp.uaa.security.authentication.JwtMismatchException;
import com.bcn.asapp.uaa.security.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.uaa.security.authentication.matcher.JwtSessionMatcher;
import com.bcn.asapp.uaa.security.core.JwtType;

/**
 * Abstract base class for verifying JWTs within the authentication process.
 * <p>
 * This class provides a common implementation for JWT verification, defining the overall process while delegating specific verification steps to subclasses.
 * <p>
 * The JWT decoding responsibility is delegated to a {@link JwtDecoder}, and session matching is handled by a {@link JwtSessionMatcher}, enabling flexible
 * verification strategies.
 *
 * @author ttrigo
 * @since 0.2.0
 */
public abstract class AbstractJwtVerifier implements JwtVerifier {

    /**
     * JWT decoder used to decode and validate JWT strings.
     */
    private final JwtDecoder jwtDecoder;

    /**
     * Matcher used to verify if a JWT matches a stored user session.
     */
    private final JwtSessionMatcher jwtSessionMatcher;

    /**
     * Constructs a new {@code AbstractJwtVerifier} with the specified decoder and session matcher.
     *
     * @param jwtDecoder        the decoder responsible for parsing and validating JWTs
     * @param jwtSessionMatcher the matcher used to verify JWTs against user sessions
     */
    protected AbstractJwtVerifier(JwtDecoder jwtDecoder, JwtSessionMatcher jwtSessionMatcher) {
        this.jwtDecoder = jwtDecoder;
        this.jwtSessionMatcher = jwtSessionMatcher;
    }

    /**
     * Verifies the given raw JWT string by decoding, validating its type, matching it against user session data.
     *
     * @param jwt the raw JWT string to verify
     * @return a fully authenticated {@link JwtAuthenticationToken} if verification succeeds
     * @throws AuthenticationException if the JWT is invalid, mismatched, or otherwise rejected
     */
    @Override
    public final JwtAuthenticationToken verify(String jwt) {

        try {
            var decodedJwt = validateJwt(jwt);

            matchJwt(decodedJwt);

            return JwtAuthenticationToken.authenticated(decodedJwt);
        } catch (Exception e) {
            throw createAuthenticationException(e);
        }

    }

    /**
     * Decodes and validates the structure and type of the provided JWT.
     *
     * @param jwt the raw JWT string
     * @return the decoded JWT object
     * @throws UnexpectedJwtTypeException if the JWT type does not match the expected type
     */
    private DecodedJwt validateJwt(String jwt) {
        var decodedJwt = jwtDecoder.decode(jwt);

        if (!isExpectedTokenType(decodedJwt)) {
            throw new UnexpectedJwtTypeException(String.format("JWT %s is not a %s", decodedJwt.getJwt(), getTokenType()));
        }

        return decodedJwt;
    }

    /**
     * Validates that the decoded JWT matches a known user session.
     *
     * @param decodedJwt the decoded JWT to be validated
     * @throws JwtMismatchException if the token does not match the user's session
     */
    private void matchJwt(DecodedJwt decodedJwt) {
        var matches = jwtSessionMatcher.match(decodedJwt);

        if (!matches) {
            throw new JwtMismatchException(String.format("JWT does not match for user %s", decodedJwt.getSubject()));
        }

    }

    /**
     * Returns the expected {@link JwtType} for this verifier.
     * <p>
     * Implementing classes must specify which token type (access token, refresh token, etc.) this verifier handles.
     *
     * @return the expected token type
     */
    protected abstract JwtType getTokenType();

    /**
     * Determines whether the provided decoded JWT is of the expected type.
     * <p>
     * Implementing classes must verify that the decoded JWT matches the token type that this verifier handles.
     *
     * @param decodedJwt the decoded JWT to evaluate
     * @return {@code true} if the token type matches the expected type, {@code false} otherwise
     */
    protected abstract Boolean isExpectedTokenType(DecodedJwt decodedJwt);

    /**
     * Creates an appropriate {@link AuthenticationException} to wrap a JWT verification failure.
     * <p>
     * Implementing classes must wrap any exceptions that occur during verification in an appropriate authentication exception.
     *
     * @param cause the underlying exception
     * @return the corresponding authentication exception
     */
    protected abstract AuthenticationException createAuthenticationException(Throwable cause);

}
