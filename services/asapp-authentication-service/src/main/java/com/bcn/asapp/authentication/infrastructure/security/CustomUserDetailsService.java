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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserRepository;

/**
 * Custom implementation of Spring's {@link UserDetailsService} for loading user details from the database.
 * <p>
 * Integrates with Spring Security's authentication framework by retrieving user information and constructing {@link CustomUserDetails} instances.
 *
 * @since 0.2.0
 * @see UserDetailsService
 * @author attrigo
 */
@Component
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final JdbcUserRepository userRepository;

    /**
     * Constructs a new {@code CustomUserDetailsService} with required dependencies.
     *
     * @param userRepository the user JDBC repository
     */
    public CustomUserDetailsService(JdbcUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user details by username.
     * <p>
     * Retrieves the user from the database and constructs a {@link CustomUserDetails} instance with user ID, credentials, and authorities.
     *
     * @param username the username to search for
     * @return the {@link UserDetails} containing user information
     * @throws UsernameNotFoundException if no user is found with the given username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.trace("Loading user with username {}", username);

        var user = userRepository.findByUsername(username)
                                 .orElseThrow(() -> new UsernameNotFoundException("User not exists by username: " + username));

        var authorities = AuthorityUtils.createAuthorityList(user.role());

        return new CustomUserDetails(user.id(), user.username(), user.password(), authorities);
    }

}
