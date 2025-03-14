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
package com.bcn.asapp.uaa.config.security;

import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.auth.User;
import com.bcn.asapp.uaa.auth.UserRepository;

/**
 * Custom implementation of {@link UserDetailsService} that loads user-specific data for authentication purposes.
 * <p>
 * Fetches user information from a {@link UserRepository} and constructs a {@link UserDetails} object, which is used by Spring Security to authenticate the
 * user.
 *
 * @author ttrigo
 * @see UserDetailsService
 * @see org.springframework.security.core.userdetails.User
 * @since 0.2.0
 */
@Component
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Main constructor.
     *
     * @param userRepository the repository to access user's data.
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                                  .orElseThrow(() -> new UsernameNotFoundException("User not exists by Username"));

        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority(user.role()
                                                                                  .name()));

        return new org.springframework.security.core.userdetails.User(username, user.password(), authorities);
    }

}
