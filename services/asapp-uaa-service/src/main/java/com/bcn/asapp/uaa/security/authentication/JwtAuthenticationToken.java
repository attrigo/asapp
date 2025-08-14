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

package com.bcn.asapp.uaa.security.authentication;

import java.io.Serial;
import java.util.Collection;
import java.util.Set;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;

import jakarta.annotation.Nullable;

/**
 * Represents an authentication token based on JSON Web Tokens (JWT) within the Spring Security context.
 * <p>
 * This class encapsulates JWT details such as the raw token string and the subject (user identity), along with the authentication state and granted
 * authorities.
 * <p>
 * Instances can be created as either authenticated or unauthenticated using the provided static factory methods.
 *
 * @author ttrigo
 * @see AbstractAuthenticationToken
 * @since 0.2.0
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 6479402217923388079L;

    /**
     * The subject associated with this JWT, typically representing the user identity.
     */
    private final String subject;

    /**
     * The raw JWT string that this token encapsulates.
     */
    private final String jwt;

    /**
     * Constructs an unauthenticated {@code JwtAuthenticationToken} with the specified subject and JWT string.
     *
     * @param subject the user identity or subject claim contained in the JWT, may be {@literal null}
     * @param jwt     the raw JWT string, must not be {@literal null} or empty
     * @throws IllegalArgumentException if {@code jwt} is {@literal null} or empty
     */
    JwtAuthenticationToken(String subject, String jwt) {
        super(null);
        Assert.hasText(jwt, "JWT must not be null or empty");
        this.subject = subject;
        this.jwt = jwt;
        super.setAuthenticated(false);
    }

    /**
     * Constructs an authenticated {@code JwtAuthenticationToken} with the specified subject, authorities, and JWT string.
     *
     * @param subject     the user identity or subject claim contained in the JWT, may be {@literal null}
     * @param authorities the authorities granted to the principal; may be empty but must not be {@literal null}
     * @param jwt         the raw JWT string, must not be {@literal null} or empty
     * @throws IllegalArgumentException if {@code authorities} is {@literal null}, or if {@code jwt} is {@literal null} or empty
     */
    JwtAuthenticationToken(String subject, Collection<? extends GrantedAuthority> authorities, String jwt) {
        super(authorities);
        Assert.hasText(jwt, "JWT must not be null or empty");
        this.subject = subject;
        this.jwt = jwt;
        super.setAuthenticated(true);
    }

    /**
     * Creates an unauthenticated {@code JwtAuthenticationToken} instance from a decoded JWT.
     *
     * @param decodedJwt the decoded JWT object, must not be {@literal null}
     * @return a new unauthenticated {@link JwtAuthenticationToken} instance
     * @throws IllegalArgumentException if {@code decodedJwt} is {@literal null}
     */
    public static JwtAuthenticationToken unauthenticated(DecodedJwt decodedJwt) {
        Assert.notNull(decodedJwt, "Decoded JWT must not be null");
        var subject = decodedJwt.getSubject();
        var jwt = decodedJwt.getJwt();
        return new JwtAuthenticationToken(subject, jwt);
    }

    /**
     * Creates an authenticated {@code JwtAuthenticationToken} instance from a decoded JWT.
     * <p>
     * The authorities are extracted from the role claim of the decoded JWT.
     *
     * @param decodedJwt the decoded JWT object, must not be {@literal null}
     * @return a new authenticated {@code JwtAuthenticationToken} instance
     * @throws IllegalArgumentException if {@code decodedJwt} is {@literal null}
     */
    public static JwtAuthenticationToken authenticated(DecodedJwt decodedJwt) {
        Assert.notNull(decodedJwt, "Decoded JWT must not be null");
        var subject = decodedJwt.getSubject();
        var roleClaim = decodedJwt.getRole();
        var authorities = Set.of((new SimpleGrantedAuthority(roleClaim.name())));
        var jwt = decodedJwt.getJwt();
        return new JwtAuthenticationToken(subject, authorities, jwt);
    }

    /**
     * Returns the credentials associated with this token.
     * <p>
     * This implementation always returns {@literal null} as credentials are not stored here.
     *
     * @return {@literal null}, since credentials are not stored in this token
     */
    @Override
    @Nullable
    public Object getCredentials() {
        return null;
    }

    /**
     * Returns the principal associated with this token, typically the subject of the JWT.
     *
     * @return the subject representing the principal, may be {@literal null}
     */
    @Override
    public Object getPrincipal() {
        return subject;
    }

    /**
     * Returns the raw JWT string associated with this token.
     *
     * @return the raw JWT string, never {@literal null} or empty
     */
    public String getJwt() {
        return jwt;
    }

}
