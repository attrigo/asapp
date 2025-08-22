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

package com.bcn.asapp.uaa.infrastructure.security;

import java.util.Collection;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Custom implementation of Spring Security's {@link User} with additional user ID information.
 * <p>
 * Extends {@link User} to include the user's unique identifier, which is required for domain operations beyond authentication.
 *
 * @since 0.2.0
 * @see User
 * @author attrigo
 */
public class CustomUserDetails extends User {

    private final UUID userId;

    /**
     * Constructs a new {@code CustomUserDetails} with user information.
     *
     * @param userId      the user's unique identifier
     * @param username    the username
     * @param password    the encoded password
     * @param authorities the granted authorities
     */
    public CustomUserDetails(UUID userId, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
    }

    /**
     * Returns the user's unique identifier.
     *
     * @return the user ID
     */
    public UUID getUserId() {
        return userId;
    }

}
