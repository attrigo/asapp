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
package com.bcn.asapp.tasks.config.security;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authentication token that extends Spring Security's {@link UsernamePasswordAuthenticationToken} to include JWT support.
 * <p>
 * This token holds the JWT string along with the standard authentication information (principal, credentials, and authorities). It is used throughout the
 * application to represent an authenticated user with their associated JWT.
 * <p>
 * The JWT is preserved in an immutable field and can be accessed via {@link #getJwt()}, allowing the token to be used for later authenticated requests.
 *
 * @author ttrigo
 * @since 0.2.0
 * @see UsernamePasswordAuthenticationToken
 */
public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private final String jwt;

    /**
     * Main constructor.
     *
     * @param principal   the identity of the principal being authenticated (typically username).
     * @param credentials the credentials that verify the principal (typically password).
     * @param authorities the authorities granted to the principal.
     * @param jwt         the JWT string associated with this authentication.
     */
    public JwtAuthenticationToken(String principal, String credentials, Collection<? extends GrantedAuthority> authorities, String jwt) {
        super(principal, credentials, authorities);
        this.jwt = jwt;
    }

    /**
     * Returns the JWT associated with this authentication.
     *
     * @return the JWT string.
     */
    public String getJwt() {
        return jwt;
    }

}
