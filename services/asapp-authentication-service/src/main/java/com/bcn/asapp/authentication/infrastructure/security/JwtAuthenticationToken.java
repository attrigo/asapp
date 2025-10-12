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

package com.bcn.asapp.authentication.infrastructure.security;

import java.io.Serial;
import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.util.Assert;

import jakarta.annotation.Nullable;

import com.bcn.asapp.authentication.domain.authentication.Jwt;

/**
 * Spring Security authentication token for JWT-based authentication.
 * <p>
 * Represents an authenticated user with a validated JWT token.
 * <p>
 * Extends {@link AbstractAuthenticationToken} to integrate with Spring Security's authentication framework.
 *
 * @since 0.2.0
 * @see AbstractAuthenticationToken
 * @author attrigo
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 6479402217923388079L;

    private final Jwt jwt;

    /**
     * Constructs a new authenticated {@code JwtAuthenticationToken}.
     *
     * @param jwt         the validated {@link Jwt}
     * @param authorities the granted authorities extracted from the JWT
     */
    private JwtAuthenticationToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        super.setAuthenticated(true);
    }

    /**
     * Factory method to create an authenticated {@code JwtAuthenticationToken}.
     * <p>
     * Extracts the role from the JWT and creates the corresponding authorities.
     *
     * @param jwt the validated {@link Jwt}
     * @return a new authenticated {@code JwtAuthenticationToken}
     * @throws IllegalArgumentException if jwt is {@code null}
     */
    public static JwtAuthenticationToken authenticated(Jwt jwt) {
        Assert.notNull(jwt, "JWT must not be null");

        var role = jwt.roleClaim();
        var authorities = role == null ? AuthorityUtils.NO_AUTHORITIES : AuthorityUtils.createAuthorityList(role.name());
        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * Returns the credentials.
     * <p>
     * Always returns {@code null} as JWT tokens do not expose credentials after authentication.
     *
     * @return {@code null}
     */
    @Override
    @Nullable
    public Object getCredentials() {
        return null;
    }

    /**
     * Returns the principal (subject) of the JWT.
     *
     * @return the {@code Subject} from the JWT
     */
    @Override
    public Object getPrincipal() {
        return jwt.subject();
    }

    /**
     * Returns the encoded JWT token value as {@link String}.
     *
     * @return the encoded JWT token
     */
    public String getJwt() {
        return jwt.encodedTokenValue();
    }

}
