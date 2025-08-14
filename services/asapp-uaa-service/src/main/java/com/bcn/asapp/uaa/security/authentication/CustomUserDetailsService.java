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

import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.user.UserRepository;

/**
 * Loads user-specific data required for authentication by implementing {@link UserDetailsService}.
 * <p>
 * This service retrieves user information from a {@link UserRepository} and constructs a Spring Security {@link UserDetails} instance containing the username,
 * password, and authorities.
 * <p>
 * User roles are mapped to {@link SimpleGrantedAuthority} objects to represent granted authorities used by Spring Security during authorization decisions.
 * <p>
 * This class is typically used by Spring Security's authentication manager during user authentication.
 *
 * @author ttrigo
 * @see UserDetailsService
 * @see org.springframework.security.core.userdetails.User
 * @see SimpleGrantedAuthority
 * @since 0.2.0
 */
@Component
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Repository for performing CRUD operations on user entities.
     */
    private final UserRepository userRepository;

    /**
     * Constructs a new {@code CustomUserDetailsService} with the specified dependencies.
     *
     * @param userRepository the repository for performing CRUD operations on user entities
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by the provided username.
     * <p>
     * Retrieves the user from the repository and builds a {@link UserDetails} instance with their credentials and role-based authorities.
     *
     * @param username the username identifying the user
     * @return a fully populated {@link UserDetails} instance
     * @throws UsernameNotFoundException if no user is found with the provided username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                                 .orElseThrow(() -> new UsernameNotFoundException("User not exists by Username"));

        var authorities = Set.of(new SimpleGrantedAuthority(user.role()
                                                                .name()));

        return new org.springframework.security.core.userdetails.User(username, user.password(), authorities);
    }

}
